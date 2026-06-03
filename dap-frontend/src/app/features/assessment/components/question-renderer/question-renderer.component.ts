import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuestionResponse, McqQuestionResponse, GroupQuestionResponse } from '../../../../core/models/question.model';
import { ResponseRequest, McqResponseRequest, TextResponseRequest, DocResponseRequest, GroupResponseRequest } from '../../../../core/models/assessment-session.model';

export interface AnswerChangedEvent {
  questionId: string;
  request: ResponseRequest;
}

@Component({
  selector: 'dap-question-renderer',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './question-renderer.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionRendererComponent {
  readonly question = input.required<QuestionResponse>();
  readonly answerChanged = output<AnswerChangedEvent>();

  readonly selectedOption = signal<string | null>(null);
  readonly selectedOptions = signal<Set<string>>(new Set());
  readonly textAnswer = signal('');
  readonly childAnswers = signal<Record<string, string | undefined>>({});

  isMcq(): boolean { return this.question().type === 'MCQ'; }
  isText(): boolean { return this.question().type === 'TEXT'; }
  isDoc(): boolean { return this.question().type === 'DOC'; }
  isGroup(): boolean { return this.question().type === 'GROUP'; }

  isMultiCorrect(): boolean {
    const q = this.question() as McqQuestionResponse;
    return q.correctAnswers?.length > 1;
  }

  asMcq(): McqQuestionResponse { return this.question() as McqQuestionResponse; }
  asGroup(): GroupQuestionResponse { return this.question() as GroupQuestionResponse; }

  onRadioSelect(option: string): void {
    this.selectedOption.set(option);
    const req: McqResponseRequest = { selectedAnswers: [option] };
    this.answerChanged.emit({ questionId: this.question().id, request: req });
  }

  onCheckboxToggle(option: string): void {
    const current = new Set(this.selectedOptions());
    if (current.has(option)) {
      current.delete(option);
    } else {
      current.add(option);
    }
    this.selectedOptions.set(current);
    const req: McqResponseRequest = { selectedAnswers: Array.from(current) };
    this.answerChanged.emit({ questionId: this.question().id, request: req });
  }

  isChecked(option: string): boolean {
    return this.selectedOptions().has(option);
  }

  onTextChange(value: string): void {
    this.textAnswer.set(value);
    const req: TextResponseRequest = { answer: value };
    this.answerChanged.emit({ questionId: this.question().id, request: req });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const req: DocResponseRequest = { filePath: file.name };
    this.answerChanged.emit({ questionId: this.question().id, request: req });
  }

  onChildTextChange(childId: string, value: string): void {
    this.childAnswers.update(m => ({ ...m, [childId]: value }));
    const childResponses: Record<string, ResponseRequest> = {};
    const updated = { ...this.childAnswers(), [childId]: value };
    for (const [id, ans] of Object.entries(updated)) {
      childResponses[id] = { answer: ans ?? '' } as TextResponseRequest;
    }
    const req: GroupResponseRequest = { childResponses };
    this.answerChanged.emit({ questionId: this.question().id, request: req });
  }
}
