import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  OnInit,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { QuestionService } from '../../../../core/services/question.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  GroupQuestionResponse,
  McqQuestionResponse,
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
export class QuestionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly questionService = inject(QuestionService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly existingQuestion = input<QuestionResponse | undefined>(undefined);

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
    category: ['', Validators.required],
    questionText: ['', [Validators.required, Validators.minLength(10)]],
  });

  readonly questionTypes: QuestionType[] = ['MCQ', 'TEXT', 'DOC', 'GROUP'];

  readonly isEditMode = computed(() => !!this.existingQuestion());

  readonly mcqInitialOptions = computed(() => {
    const q = this.existingQuestion();
    if (!q || this.resolveType(q) !== 'MCQ') return [];
    const mq = q as McqQuestionResponse;
    return mq.options.map(opt => ({ text: opt, correct: mq.correctAnswers.includes(opt) }));
  });

  readonly textInitialKeywords = computed(() => {
    const q = this.existingQuestion();
    if (!q || this.resolveType(q) !== 'TEXT') return [];
    return (q as TextQuestionResponse).keywords ?? [];
  });

  ngOnInit(): void {
    const q = this.existingQuestion();
    if (!q) return;

    const type = this.resolveType(q);
    this.selectedType.set(type);
    this.form.patchValue({ category: q.category, questionText: q.question });

    if (type === 'GROUP') {
      const gq = q as GroupQuestionResponse;
      this.orderedGroup.set(gq.ordered);
      this.selectedFollowUpIds.set(gq.followUpQuestions.map(fq => fq.id));
      this.loadTextQuestions();
    }
  }

  onTypeChange(type: QuestionType | ''): void {
    this.selectedType.set(type);
    this.errorMsg.set('');
    if (type === 'GROUP') this.loadTextQuestions();
  }

  onKeywordsChange(keywords: string[]): void { this.currentKeywords = keywords; }
  onMcqValueChange(value: McqBuilderValue): void { this.currentMcqValue = value; }

  toggleFollowUp(id: string): void {
    this.selectedFollowUpIds.update(ids =>
      ids.includes(id) ? ids.filter(i => i !== id) : [...ids, id]
    );
  }

  cancel(): void { this.cancelled.emit(); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const type = this.selectedType();
    if (!type) { this.errorMsg.set('Please select a question type.'); return; }
    const { category, questionText } = this.form.getRawValue();
    this.errorMsg.set('');

    if (type === 'MCQ') {
      const builder = this.mcqBuilder();
      const mcqVal = builder ? builder.getValue() : this.currentMcqValue;
      if (!mcqVal.isValid) { this.errorMsg.set('Add at least one option and mark a correct answer.'); return; }
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
      this.save({ type: 'GROUP', category, question: questionText, ordered: this.orderedGroup(), followUpQuestionIds: this.selectedFollowUpIds() });
    }
  }

  resolveType(q: QuestionResponse): QuestionType {
    if (q.type) return q.type;
    if ('correctAnswers' in q) return 'MCQ';
    if ('followUpQuestions' in q) return 'GROUP';
    if ('keywords' in q) return 'TEXT';
    return 'DOC';
  }

  private save(request: Parameters<QuestionService['createQuestion']>[0]): void {
    this.saving.set(true);
    const existing = this.existingQuestion();
    const obs = existing
      ? this.questionService.updateQuestion(existing.id, request)
      : this.questionService.createQuestion(request);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.saving.set(false);
        this.toastService.success(this.isEditMode() ? 'Question updated.' : 'Question created.');
        this.questionAdded.emit(res);
      },
      error: () => {
        this.saving.set(false);
        this.errorMsg.set('Failed to save question. Please try again.');
        this.toastService.error('Failed to save question. Please try again.');
      },
    });
  }

  private loadTextQuestions(): void {
    this.questionService.getQuestions(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.textQuestions.set(
            page.content.filter((q): q is TextQuestionResponse => this.resolveType(q) === 'TEXT')
          );
        },
        error: () => {},
      });
  }
}
