import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  output,
  signal,
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserService } from '../../../../core/services/user.service';
import { AssessmentService } from '../../../../core/services/assessment.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CandidateResponse } from '../../../../core/models/user.model';
import { AssessmentResponse } from '../../../../core/models/assessment.model';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

export interface NavigateToQuestionsPayload {
  candidateId: string;
  candidateName: string;
  timeLimit: number;
}

@Component({
  selector: 'dap-assessment-generate',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, AvatarComponent, ButtonComponent],
  templateUrl: './assessment-generate.component.html',
})
export class AssessmentGenerateComponent {
  private readonly userService       = inject(UserService);
  private readonly assessmentService = inject(AssessmentService);
  private readonly toastService      = inject(ToastService);
  private readonly destroyRef        = inject(DestroyRef);

  readonly cancelled           = output<void>();
  readonly assessmentCreated   = output<void>();
  readonly navigateToQuestions = output<NavigateToQuestionsPayload>();

  // Candidates + dropdown
  readonly candidates        = signal<CandidateResponse[]>([]);
  readonly candidatesLoading = signal(false);
  readonly selectedCandidateId = signal('');
  readonly dropdownOpen      = signal(false);
  readonly searchText        = signal('');

  readonly filteredCandidates = computed(() => {
    const q = this.searchText().toLowerCase();
    return q
      ? this.candidates().filter(c =>
          c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q))
      : this.candidates();
  });

  readonly selectedCandidate = computed(() =>
    this.candidates().find(c => c.id === this.selectedCandidateId()) ?? null
  );

  // Options
  readonly manualQuestions  = signal(false);
  readonly timeLimitControl = new FormControl<number>(60, {
    validators: [Validators.required, Validators.min(5)],
    nonNullable: true,
  });

  readonly canProceed = computed(() =>
    !!this.selectedCandidateId() && this.timeLimitControl.valid
  );

  // Submit state
  readonly submitting  = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly result      = signal<AssessmentResponse | null>(null);
  readonly done        = signal(false);
  readonly copied      = signal(false);

  constructor() { this.loadCandidates(); }

  selectCandidate(id: string): void {
    this.selectedCandidateId.set(id);
    this.dropdownOpen.set(false);
    this.searchText.set('');
  }

  proceed(): void {
    if (!this.canProceed()) return;

    if (this.manualQuestions()) {
      this.navigateToQuestions.emit({
        candidateId:   this.selectedCandidateId(),
        candidateName: this.selectedCandidate()?.name ?? '',
        timeLimit:     this.timeLimitControl.value,
      });
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);
    this.assessmentService.generateAssessment({
      candidateId:      this.selectedCandidateId(),
      questionIds:      [],
      timeLimitMinutes: this.timeLimitControl.value,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.result.set(res);
          this.done.set(true);
          this.submitting.set(false);
          this.toastService.success('Assessment generated successfully.');
          this.assessmentCreated.emit();
        },
        error: () => {
          this.submitError.set('Failed to generate assessment. Please try again.');
          this.submitting.set(false);
          this.toastService.error('Failed to generate assessment. Please try again.');
        },
      });
  }

  copyLink(): void {
    navigator.clipboard.writeText(this.result()!.invitationLink).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  private loadCandidates(): void {
    this.candidatesLoading.set(true);
    this.userService.getCandidates(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.candidates.set(page.content);
          this.candidatesLoading.set(false);
        },
        error: () => this.candidatesLoading.set(false),
      });
  }
}
