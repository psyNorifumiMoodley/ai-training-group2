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

  readonly cardClicked    = output<void>();
  readonly feedbackChanged = output<{ responseId: string; feedbackText: string }>();

  readonly feedbackText = linkedSignal(() => {
    const item = this.item();
    if (item.feedbackDraft?.trim()) return item.feedbackDraft;
    return item.questionType === 'MCQ'
      ? (item.correct === true ? 'Correct.' : 'Incorrect.')
      : '';
  });

  tagVariant(): 'mcq' | 'text' | 'doc' {
    const t = this.item().questionType;
    return t === 'MCQ' ? 'mcq' : t === 'TEXT' ? 'text' : 'doc';
  }

  mcqAnswers(): string[] {
    return (this.item().answer as McqAnswerPayload)?.selectedAnswers ?? [];
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
}
