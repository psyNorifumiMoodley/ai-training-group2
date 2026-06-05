import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AvatarComponent } from '../../../shared/components/avatar/avatar.component';
import { ProgressBarComponent } from '../../../shared/components/progress-bar/progress-bar.component';
import { QuestionNavDotComponent } from '../../../shared/components/question-nav-dot/question-nav-dot.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { FeedbackItemEditorComponent } from './feedback-item-editor/feedback-item-editor.component';
import { MarkingService } from '../../../core/services/marking.service';
import { ResponseReviewItem } from '../../../core/models/marking.model';

const FEEDBACK_TEMPLATES = {
  strong: 'The candidate demonstrated a strong understanding of the core concepts and provided clear, well-reasoned responses throughout the assessment.',
  good:   'The candidate showed a good grasp of the fundamentals. Some responses would benefit from greater depth, but the overall performance was satisfactory.',
  needs:  "The candidate's responses indicate a basic familiarity with the subject matter. We recommend further study and practice before the next assessment.",
} as const;

@Component({
  selector: 'dap-marking',
  standalone: true,
  imports: [
    AvatarComponent,
    ProgressBarComponent,
    QuestionNavDotComponent,
    ButtonComponent,
    FeedbackItemEditorComponent,
  ],
  templateUrl: './marking.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkingComponent implements OnInit {
  private readonly markingService = inject(MarkingService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly destroyRef     = inject(DestroyRef);

  readonly assessmentId    = signal('');
  readonly candidateName   = signal('');
  readonly assessmentMeta  = signal('');
  readonly reviewItems     = signal<ResponseReviewItem[]>([]);
  readonly feedbackMap     = signal<Record<string, string>>({});
  readonly overallFeedback = signal('');
  readonly activeIndex     = signal(0);
  readonly loading         = signal(true);
  readonly finalising      = signal(false);

  readonly markedCount = computed(() => {
    const map = this.feedbackMap();
    return this.reviewItems().filter(i => (map[i.responseId] ?? '').trim().length > 0).length;
  });

  readonly canFinalise = computed(() => {
    const items = this.reviewItems();
    if (!items.length) return false;
    const map = this.feedbackMap();
    return items
      .filter(i => i.questionType === 'TEXT' || i.questionType === 'DOC')
      .every(i => (map[i.responseId] ?? '').trim().length > 0);
  });

  readonly mcqScore = computed(() =>
    this.reviewItems()
      .filter(i => i.questionType === 'MCQ')
      .reduce((sum, i) => sum + (i.score ?? 0), 0)
  );

  readonly mcqMaxScore = computed(() =>
    this.reviewItems()
      .filter(i => i.questionType === 'MCQ')
      .reduce((sum, i) => sum + i.marks, 0)
  );

  constructor() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state as { candidateName?: string; assessmentMeta?: string } | undefined;
    if (state?.candidateName) this.candidateName.set(state.candidateName);
    if (state?.assessmentMeta) this.assessmentMeta.set(state.assessmentMeta);
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('assessmentId') ?? '';
    this.assessmentId.set(id);

    this.markingService
      .getResponsesForReview(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: items => {
          this.reviewItems.set(items);
          const map: Record<string, string> = {};
          for (const item of items) {
            map[item.responseId] = item.feedbackDraft?.trim()
              ? item.feedbackDraft
              : item.questionType === 'MCQ'
                ? (item.correct === true ? 'Correct.' : 'Incorrect.')
                : '';
          }
          this.feedbackMap.set(map);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  navState(index: number): 'done' | 'active' | 'todo' {
    if (index < this.activeIndex()) return 'done';
    if (index === this.activeIndex()) return 'active';
    return 'todo';
  }

  onFeedbackChanged(event: { responseId: string; feedbackText: string }): void {
    this.feedbackMap.update(m => ({ ...m, [event.responseId]: event.feedbackText }));
    this.markingService
      .updateResponseFeedback(this.assessmentId(), event.responseId, { feedbackText: event.feedbackText })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  setFeedbackTemplate(key: keyof typeof FEEDBACK_TEMPLATES): void {
    this.overallFeedback.set(FEEDBACK_TEMPLATES[key]);
  }

  backToAssessments(): void {
    this.router.navigate(['/assessments']);
  }

  finalise(): void {
    this.finalising.set(true);
    this.markingService
      .finaliseMarking(this.assessmentId(), this.overallFeedback())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/assessments']),
        error: () => this.finalising.set(false),
      });
  }
}
