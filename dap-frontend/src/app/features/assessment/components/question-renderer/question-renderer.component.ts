import { ChangeDetectionStrategy, Component, effect, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuestionResponse, McqQuestionResponse, McqPlusQuestionResponse, GroupQuestionResponse, QuestionType } from '../../../../core/models/question.model';
import { ResponseRequest, McqResponseRequest, McqPlusResponseRequest, TextResponseRequest, DocResponseRequest, GroupResponseRequest } from '../../../../core/models/assessment-session.model';

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
  readonly savedAnswer = input<ResponseRequest | undefined>(undefined);
  readonly answerChanged = output<AnswerChangedEvent>();

  readonly selectedOption = signal<string | null>(null);
  readonly selectedOptions = signal<Set<string>>(new Set());
  readonly textAnswer = signal('');
  readonly childAnswers = signal<(string | undefined)[]>([]);
  readonly followUpAnswer = signal('');

  constructor() {
    effect(() => {
      const q = this.question();
      const saved = untracked(() => this.savedAnswer());

      this.selectedOption.set(null);
      this.selectedOptions.set(new Set());
      this.textAnswer.set('');
      this.childAnswers.set([]);
      this.followUpAnswer.set('');

      if (!saved) return;

      const resolvedType = this.resolveQuestionType(q);

      if (resolvedType === 'MCQ' || resolvedType === 'MCQ_PLUS') {
        const mcqSaved = saved as McqResponseRequest | McqPlusResponseRequest;
        if ((q as McqQuestionResponse).multiCorrect) {
          this.selectedOptions.set(new Set(mcqSaved.selectedAnswers ?? []));
        } else {
          this.selectedOption.set(mcqSaved.selectedAnswers?.[0] ?? null);
        }
        if (resolvedType === 'MCQ_PLUS') {
          this.followUpAnswer.set((saved as McqPlusResponseRequest).followUpAnswer ?? '');
        }
      } else if (resolvedType === 'TEXT') {
        this.textAnswer.set((saved as TextResponseRequest).answer ?? '');
      } else if (resolvedType === 'GROUP') {
        const groupSaved = saved as GroupResponseRequest;
        this.childAnswers.set(groupSaved.childAnswers ?? []);
      }
    }, { allowSignalWrites: true });
  }

  private resolveQuestionType(q: QuestionResponse): QuestionType {
    if (q.type) return q.type;
    if ('followUpQuestion' in q) return 'MCQ_PLUS';
    if ('correctAnswers' in q) return 'MCQ';
    if ('children' in q) return 'GROUP';
    if ('keywords' in q) return 'TEXT';
    return 'DOC';
  }

  resolveType(): QuestionType {
    return this.resolveQuestionType(this.question());
  }

  isMcq(): boolean     { return this.resolveType() === 'MCQ'; }
  isMcqPlus(): boolean { return this.resolveType() === 'MCQ_PLUS'; }
  isText(): boolean    { return this.resolveType() === 'TEXT'; }
  isDoc(): boolean     { return this.resolveType() === 'DOC'; }
  isGroup(): boolean   { return this.resolveType() === 'GROUP'; }

  isMultiCorrect(): boolean {
    return (this.question() as McqQuestionResponse).multiCorrect;
  }

  asMcq(): McqQuestionResponse         { return this.question() as McqQuestionResponse; }
  asMcqPlus(): McqPlusQuestionResponse { return this.question() as McqPlusQuestionResponse; }
  asGroup(): GroupQuestionResponse     { return this.question() as GroupQuestionResponse; }

  onRadioSelect(option: string): void {
    this.selectedOption.set(option);
    this.emitMcqResponse([option]);
  }

  onCheckboxToggle(option: string): void {
    const current = new Set(this.selectedOptions());
    current.has(option) ? current.delete(option) : current.add(option);
    this.selectedOptions.set(current);
    this.emitMcqResponse(Array.from(current));
  }

  private emitMcqResponse(answers: string[]): void {
    if (this.isMcqPlus()) {
      const req: McqPlusResponseRequest = { selectedAnswers: answers, followUpAnswer: this.followUpAnswer() };
      this.answerChanged.emit({ questionId: this.question().id, request: req });
    } else {
      const req: McqResponseRequest = { selectedAnswers: answers };
      this.answerChanged.emit({ questionId: this.question().id, request: req });
    }
  }

  onMcqPlusFollowUpChange(value: string): void {
    this.followUpAnswer.set(value);
    const answers = this.isMultiCorrect()
      ? Array.from(this.selectedOptions())
      : (this.selectedOption() ? [this.selectedOption()!] : []);
    const req: McqPlusResponseRequest = { selectedAnswers: answers, followUpAnswer: value };
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

  onChildTextChange(childIndex: number, value: string): void {
    const updated = [...this.childAnswers()];
    updated[childIndex] = value;
    this.childAnswers.set(updated);
    const req: GroupResponseRequest = { childAnswers: updated.map(a => a ?? '') };
    this.answerChanged.emit({ questionId: this.question().id, request: req });
  }
}
