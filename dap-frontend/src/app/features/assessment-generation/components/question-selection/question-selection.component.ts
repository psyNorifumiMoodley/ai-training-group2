import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { QuestionService } from '../../../../core/services/question.service';
import { AssessmentService } from '../../../../core/services/assessment.service';
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
  private readonly questionService   = inject(QuestionService);
  private readonly assessmentService = inject(AssessmentService);
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
      const typeOk    = this.selectedType() === 'ALL' || q.type === this.selectedType();
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

  typeVariant(type: QuestionType): 'mcq' | 'text' | 'doc' | 'info' {
    const map: Record<QuestionType, 'mcq' | 'text' | 'doc' | 'info'> = {
      MCQ: 'mcq', TEXT: 'text', DOC: 'doc', GROUP: 'info',
    };
    return map[type];
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
        },
        error: () => {
          this.submitError.set('Failed to generate assessment. Please try again.');
          this.submitting.set(false);
        },
      });
  }
}
