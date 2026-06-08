import { ChangeDetectionStrategy, Component, input, linkedSignal, output } from '@angular/core';
import { DocAnswerPayload, McqAnswerPayload, ResponseReviewItem, TextAnswerPayload } from '../../../../core/models/marking.model';
import { TagComponent } from '../../../../shared/components/tag/tag.component';
import { ScoreInputComponent } from '../../../../shared/components/score-input/score-input.component';

@Component({
  selector: 'dap-feedback-item-editor',
  standalone: true,
  imports: [TagComponent, ScoreInputComponent],
  templateUrl: './feedback-item-editor.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackItemEditorComponent {
  readonly item           = input.required<ResponseReviewItem>();
  readonly questionNumber = input.required<number>();
  readonly isActive       = input(false);

  readonly cardClicked     = output<void>();
  readonly feedbackChanged = output<{ responseId: string; feedbackText: string }>();
  readonly scoreChanged    = output<{ responseId: string; score: number }>();

  readonly feedbackText = linkedSignal(() => {
    const item = this.item();
    if (item.feedbackDraft?.trim()) return item.feedbackDraft;
    return item.questionType === 'MCQ'
      ? (item.correct === true ? 'Correct.' : 'Incorrect.')
      : '';
  });

  tagVariant(): 'mcq' | 'text' | 'doc' | 'default' {
    const t = this.item().questionType;
    if (t === 'MCQ') return 'mcq';
    if (t === 'TEXT') return 'text';
    if (t === 'DOC') return 'doc';
    return 'default';
  }

  childAnswerText(child: ResponseReviewItem): string {
    return (child.answer as TextAnswerPayload)?.answer ?? '';
  }

  mcqAnswers(): string[] {
    return (this.item().answer as McqAnswerPayload)?.selectedAnswers ?? [];
  }

  mcqAllOptions(): string[] {
    return (this.item().answer as McqAnswerPayload)?.allOptions ?? [];
  }

  mcqCorrectAnswers(): string[] {
    return (this.item().answer as McqAnswerPayload)?.correctAnswers ?? [];
  }

  textAnswerText(): string {
    return (this.item().answer as TextAnswerPayload)?.answer ?? '';
  }

  docFileName(): string {
    return (this.item().answer as DocAnswerPayload)?.fileName ?? '';
  }

  onFeedbackInput(event: Event): void {
    this.feedbackText.set((event.target as HTMLTextAreaElement).value);
  }

  onFeedbackBlur(): void {
    this.feedbackChanged.emit({ responseId: this.item().responseId, feedbackText: this.feedbackText() });
  }

  onScoreChanged(score: number): void {
    this.scoreChanged.emit({ responseId: this.item().responseId, score });
  }
}
