import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CandidateAssessmentService } from '../../../../core/services/candidate-assessment.service';
import { AssessmentAccessResponse, ResponseRequest } from '../../../../core/models/assessment-session.model';
import { CountdownTimerComponent } from '../countdown-timer/countdown-timer.component';
import { QuestionRendererComponent, AnswerChangedEvent } from '../question-renderer/question-renderer.component';
import { ConfirmModalComponent } from '../../../../shared/components/confirm-modal/confirm-modal.component';
import { QuestionNavDotComponent } from '../../../../shared/components/question-nav-dot/question-nav-dot.component';
import { ProgressBarComponent } from '../../../../shared/components/progress-bar/progress-bar.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'dap-assessment-taking',
  standalone: true,
  imports: [
    DecimalPipe,
    CountdownTimerComponent,
    QuestionRendererComponent,
    ConfirmModalComponent,
    QuestionNavDotComponent,
    ProgressBarComponent,
    ButtonComponent,
  ],
  templateUrl: './assessment-taking.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssessmentTakingComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly service = inject(CandidateAssessmentService);

  readonly session = signal<AssessmentAccessResponse | null>(
    history.state['session'] ?? null,
  );

  readonly currentIndex = signal(0);
  readonly answeredQuestions = signal<Set<string>>(new Set());
  readonly savedAnswers = signal<Record<string, ResponseRequest>>({});
  readonly showConfirm = signal(false);
  readonly submitting = signal(false);
  readonly started = signal(history.state['session']?.alreadyStarted === true);
  readonly starting = signal(false);

  readonly questions = computed(() => this.session()?.questions ?? []);
  readonly currentQuestion = computed(() => this.questions()[this.currentIndex()]);
  readonly isLast = computed(() => this.currentIndex() === this.questions().length - 1);
  readonly answeredCount = computed(() => this.answeredQuestions().size);

  private readonly autoSave$ = new Subject<AnswerChangedEvent>();

  constructor() {
    this.autoSave$
      .pipe(
        debounceTime(1500),
        switchMap((event) => {
          const id = this.session()?.assessmentId;
          if (!id) return [];
          return this.service.saveResponse(id, event.questionId, event.request);
        }),
        takeUntilDestroyed(),
      )
      .subscribe();
  }

  ngOnInit(): void {
    if (!this.session()) {
      this.router.navigate(['/login']);
    }
  }

  startAssessment(): void {
    const id = this.session()?.assessmentId;
    if (!id || this.starting()) return;
    this.starting.set(true);
    this.service.startAssessment(id).subscribe({
      next: () => {
        this.starting.set(false);
        this.started.set(true);
      },
      error: () => this.starting.set(false),
    });
  }

  navState(index: number): 'done' | 'active' | 'todo' {
    const q = this.questions()[index];
    if (this.answeredQuestions().has(q.id)) return 'done';
    if (index === this.currentIndex()) return 'active';
    return 'todo';
  }

  onAnswerChanged(event: AnswerChangedEvent): void {
    this.answeredQuestions.update((s) => new Set([...s, event.questionId]));
    this.savedAnswers.update((m) => ({ ...m, [event.questionId]: event.request }));
    this.autoSave$.next(event);
  }

  goTo(index: number): void {
    this.currentIndex.set(index);
  }

  prev(): void {
    if (this.currentIndex() > 0) this.currentIndex.update((i) => i - 1);
  }

  next(): void {
    if (!this.isLast()) this.currentIndex.update((i) => i + 1);
  }

  requestSubmit(): void {
    this.showConfirm.set(true);
  }

  cancelSubmit(): void {
    this.showConfirm.set(false);
  }

  confirmSubmit(): void {
    this.showConfirm.set(false);
    this.doSubmit();
  }

  onTimerExpired(): void {
    this.doSubmit();
  }

  private doSubmit(): void {
    const id = this.session()?.assessmentId;
    if (!id || this.submitting()) return;
    this.submitting.set(true);
    this.service.submitAssessment(id).subscribe({
      next: () => this.router.navigate(['/assessment', id, 'confirmation']),
      error: () => this.submitting.set(false),
    });
  }
}
