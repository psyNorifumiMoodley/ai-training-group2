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
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { FeedbackItemEditorComponent } from './feedback-item-editor/feedback-item-editor.component';
import { MarkingService } from '../../../core/services/marking.service';
import { ResponseReviewItem } from '../../../core/models/marking.model';

const FEEDBACK_TEMPLATES = {
  interview:   'We were impressed with your performance on the assessment and would like to invite you for an interview. Please let us know your availability — we\'re looking at scheduling this for [DATE/TIME]. We look forward to meeting you and discussing the role further.',
  additional:  'Thank you for completing the assessment. We have reviewed your submission and would like to invite you to take a follow-up assessment to explore certain areas in more depth. We will be in touch shortly with the details.',
  elsewhere:   'Thank you for taking the time to complete our assessment. After careful consideration, we have decided to move forward with other candidates whose profiles more closely match our current requirements. We appreciate your interest and wish you the best in your job search.',
} as const;

type TabType = 'ALL' | 'MCQ' | 'TEXT' | 'GROUP' | 'DOC';

@Component({
  selector: 'dap-marking',
  standalone: true,
  imports: [
    AvatarComponent,
    ProgressBarComponent,
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
  readonly activeTab       = signal<TabType>('ALL');
  readonly loading         = signal(true);
  readonly finalising      = signal(false);
  readonly closing         = signal(false);

  readonly availableTabs = computed<TabType[]>(() => {
    const types = new Set(this.reviewItems().map(i => i.questionType as TabType));
    return ['ALL', ...(['MCQ', 'TEXT', 'GROUP', 'DOC'] as TabType[]).filter(t => types.has(t))];
  });

  readonly filteredItems = computed(() => {
    const tab = this.activeTab();
    if (tab === 'ALL') return this.reviewItems();
    return this.reviewItems().filter(i => i.questionType === tab);
  });

  readonly markedCount = computed(() => {
    const map = this.feedbackMap();
    return this.reviewItems().filter(i => (map[i.questionId] ?? '').trim().length > 0).length;
  });

  readonly canFinalise = computed(() => {
    const items = this.reviewItems();
    if (!items.length) return false;
    const map = this.feedbackMap();
    return items
      .filter(i => (i.questionType === 'TEXT' || i.questionType === 'DOC') && i.responseId !== null)
      .every(i => (map[i.questionId] ?? '').trim().length > 0);
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

  readonly hasAnyManualScore = computed(() =>
    this.reviewItems().some(i => i.questionType !== 'MCQ' && i.score !== null)
  );

  readonly manualScore = computed(() =>
    this.reviewItems()
      .filter(i => i.questionType !== 'MCQ' && i.score !== null)
      .reduce((sum, i) => sum + (i.score ?? 0), 0)
  );

  readonly manualMaxScore = computed(() =>
    this.reviewItems()
      .filter(i => i.questionType !== 'MCQ')
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
            if (item.responseId !== null) {
              map[item.questionId] = item.feedbackDraft?.trim()
                ? item.feedbackDraft
                : item.questionType === 'MCQ'
                  ? (item.correct === true ? 'Correct.' : 'Incorrect.')
                  : '';
            }
          }
          this.feedbackMap.set(map);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  onFeedbackChanged(event: { questionId: string; responseId: string | null; feedbackText: string }): void {
    this.feedbackMap.update(m => ({ ...m, [event.questionId]: event.feedbackText }));
    if (event.responseId) {
      this.markingService
        .updateResponseFeedback(this.assessmentId(), event.responseId, { feedbackText: event.feedbackText })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  onScoreChanged(event: { questionId: string; responseId: string | null; score: number }): void {
    this.reviewItems.update(items =>
      items.map(i => i.questionId === event.questionId ? { ...i, score: event.score } : i)
    );
    if (event.responseId) {
      this.markingService
        .updateResponseScore(this.assessmentId(), event.responseId, event.score)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  setFeedbackTemplate(key: keyof typeof FEEDBACK_TEMPLATES): void {
    this.overallFeedback.set(FEEDBACK_TEMPLATES[key]);
  }

  backToAssessments(): void {
    this.router.navigate(['/assessments']);
  }

  close(): void {
    this.closing.set(true);
    this.markingService
      .closeAssessment(this.assessmentId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/assessments']),
        error: () => this.closing.set(false),
      });
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
