import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { QuestionService } from '../../../../core/services/question.service';
import {
  QuestionResponse,
  QuestionType,
  TextQuestionResponse,
} from '../../../../core/models/question.model';
import { ButtonComponent } from '../../../../shared/components/button/button.component';
import { McqOptionBuilderComponent, McqBuilderValue } from '../mcq-option-builder/mcq-option-builder.component';
import { KeywordListComponent } from '../keyword-list/keyword-list.component';

@Component({
  selector: 'dap-question-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, McqOptionBuilderComponent, KeywordListComponent],
  templateUrl: './question-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly questionService = inject(QuestionService);
  private readonly destroyRef = inject(DestroyRef);

  readonly questionAdded = output<QuestionResponse>();
  readonly cancelled = output<void>();

  readonly selectedType = signal<QuestionType | ''>('');
  readonly saving = signal(false);
  readonly errorMsg = signal('');
  readonly textQuestions = signal<TextQuestionResponse[]>([]);
  readonly selectedFollowUpIds = signal<string[]>([]);
  readonly orderedGroup = signal(false);

  private currentKeywords: string[] = [];
  private currentMcqValue: McqBuilderValue = { options: [], correctAnswers: [], isValid: false };

  readonly mcqBuilder = viewChild<McqOptionBuilderComponent>('mcqBuilder');
  readonly keywordList = viewChild<KeywordListComponent>('keywordList');

  readonly form = this.fb.nonNullable.group({
    category: ['', [Validators.required, Validators.minLength(2)]],
    questionText: ['', [Validators.required, Validators.minLength(10)]],
  });

  readonly questionTypes: QuestionType[] = ['MCQ', 'TEXT', 'DOC', 'GROUP'];

  onTypeChange(type: QuestionType | ''): void {
    this.selectedType.set(type);
    this.errorMsg.set('');
    if (type === 'GROUP') {
      this.loadTextQuestions();
    }
  }

  onKeywordsChange(keywords: string[]): void {
    this.currentKeywords = keywords;
  }

  onMcqValueChange(value: McqBuilderValue): void {
    this.currentMcqValue = value;
  }

  toggleFollowUp(id: string): void {
    this.selectedFollowUpIds.update(ids =>
      ids.includes(id) ? ids.filter(i => i !== id) : [...ids, id]
    );
  }

  cancel(): void {
    this.cancelled.emit();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const type = this.selectedType();
    if (!type) {
      this.errorMsg.set('Please select a question type.');
      return;
    }
    const { category, questionText } = this.form.getRawValue();
    this.errorMsg.set('');

    if (type === 'MCQ') {
      const builder = this.mcqBuilder();
      const mcqVal = builder ? builder.getValue() : this.currentMcqValue;
      if (!mcqVal.isValid) {
        this.errorMsg.set('Add at least one option and mark a correct answer.');
        return;
      }
      this.save({ type: 'MCQ', category, question: questionText, options: mcqVal.options, correctAnswers: mcqVal.correctAnswers });
      return;
    }

    if (type === 'TEXT') {
      const keywords = this.keywordList()?.keywords() ?? this.currentKeywords;
      this.save({ type: 'TEXT', category, question: questionText, keywords: [...keywords] });
      return;
    }

    if (type === 'DOC') {
      this.save({ type: 'DOC', category, question: questionText });
      return;
    }

    if (type === 'GROUP') {
      this.save({
        type: 'GROUP',
        category,
        question: questionText,
        ordered: this.orderedGroup(),
        followUpQuestionIds: this.selectedFollowUpIds(),
      });
    }
  }

  private save(request: Parameters<QuestionService['createQuestion']>[0]): void {
    this.saving.set(true);
    this.questionService.createQuestion(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.saving.set(false);
          this.questionAdded.emit(res);
        },
        error: () => {
          this.saving.set(false);
          this.errorMsg.set('Failed to save question. Please try again.');
        },
      });
  }

  private loadTextQuestions(): void {
    this.questionService.getQuestions(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.textQuestions.set(
            page.content.filter((q): q is TextQuestionResponse => q.type === 'TEXT')
          );
        },
        error: () => {},
      });
  }
}
