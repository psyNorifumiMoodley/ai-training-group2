import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { QuestionService } from '../../../../core/services/question.service';
import { AssessmentService } from '../../../../core/services/assessment.service';
import { ToastService } from '../../../../core/services/toast.service';
import { QuestionResponse, QuestionType } from '../../../../core/models/question.model';
import { AssessmentResponse } from '../../../../core/models/assessment.model';
import { TagComponent } from '../../../../shared/components/tag/tag.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';
import { AssessmentConfirmationComponent } from '../assessment-confirmation/assessment-confirmation.component';

type TypeFilter = 'ALL' | QuestionType;
const TYPE_FILTERS: TypeFilter[] = ['ALL', 'MCQ', 'TEXT', 'DOC', 'GROUP'];

@Component({
  selector: 'dap-question-selection',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagComponent, ButtonComponent, AssessmentConfirmationComponent],
  templateUrl: './question-selection.component.html',
})
export class QuestionSelectionComponent {
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly questionService   = inject(QuestionService);
  private readonly assessmentService = inject(AssessmentService);
  private readonly toastService      = inject(ToastService);
  private readonly destroyRef        = inject(DestroyRef);

  readonly typeFilters = TYPE_FILTERS;

  // From query params
  readonly candidateId   = signal('');
  readonly candidateName = signal('');
  readonly timeLimitMinutes = signal(60);

  // Questions
  readonly allQuestions = signal<QuestionResponse[]>([]);
  readonly loading      = signal(false);
  readonly seenSet      = signal<Set<string>>(new Set());

  // Filters
  readonly selectedType    = signal<TypeFilter>('ALL');
  readonly selectedSubject = signal('ALL');
  readonly checkedIds      = signal<Set<string>>(new Set());

  // Derived
  readonly subjects = computed<string[]>(() => {
    const cats = new Set(this.allQuestions().map(q => q.category));
    return ['ALL', ...Array.from(cats).sort()];
  });

  readonly filteredQuestions = computed(() =>
    this.allQuestions().filter(q => {
      const typeOk    = this.selectedType() === 'ALL' || this.resolveType(q) === this.selectedType();
      const subjectOk = this.selectedSubject() === 'ALL' || q.category === this.selectedSubject();
      return typeOk && subjectOk;
    })
  );

  readonly checkedCount = computed(() => this.checkedIds().size);

  // Submit
  readonly submitting  = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly result      = signal<AssessmentResponse | null>(null);
  readonly done        = signal(false);

  constructor() {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.candidateId.set(params['candidateId'] ?? '');
        this.candidateName.set(params['candidateName'] ?? '');
        this.timeLimitMinutes.set(Number(params['timeLimit']) || 60);

        if (params['candidateId']) {
          this.assessmentService.getSeenQuestionIds(params['candidateId'])
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(ids => this.seenSet.set(new Set(ids)));
        }
      });

    this.loading.set(true);
    this.questionService.getQuestions(0, 1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.allQuestions.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  toggle(id: string): void {
    const next = new Set(this.checkedIds());
    next.has(id) ? next.delete(id) : next.add(id);
    this.checkedIds.set(next);
  }

  isChecked(id: string): boolean { return this.checkedIds().has(id); }
  isSeen(id: string): boolean    { return this.seenSet().has(id); }

  resolveType(q: QuestionResponse): QuestionType {
    if (q.type) return q.type;
    if ('correctAnswers' in q) return 'MCQ';
    if ('followUpQuestions' in q) return 'GROUP';
    if ('keywords' in q) return 'TEXT';
    return 'DOC';
  }

  typeVariant(type: QuestionType): 'mcq' | 'text' | 'doc' | 'info' {
    const map: Record<QuestionType, 'mcq' | 'text' | 'doc' | 'info'> = {
      MCQ: 'mcq', TEXT: 'text', DOC: 'doc', GROUP: 'info',
    };
    return map[type];
  }

  onDone(): void {
    this.router.navigate(['/assessments']);
  }

  submit(): void {
    if (this.checkedCount() === 0) return;
    this.submitting.set(true);
    this.submitError.set(null);

    this.assessmentService.generateAssessment({
      candidateId:      this.candidateId(),
      questionIds:      Array.from(this.checkedIds()),
      timeLimitMinutes: this.timeLimitMinutes(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.result.set(res);
          this.done.set(true);
          this.submitting.set(false);
          this.toastService.success('Assessment generated successfully.');
        },
        error: (err: HttpErrorResponse) => {
          const message = err.error?.message ?? 'Failed to generate assessment. Please try again.';
          this.submitError.set(message);
          this.submitting.set(false);
          this.toastService.error(message);
        },
      });
  }
}
