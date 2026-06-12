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
import { QuestionBankService } from '../../../../core/services/question-bank.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  CodingQuestionLanguage,
  CodingQuestionResponse,
  DocQuestionResponse,
  GroupChildRequest,
  GroupQuestionResponse,
  McqPlusQuestionResponse,
  McqQuestionResponse,
  QuestionBankResponse,
  QuestionResponse,
  QuestionType,
  TestCaseRequest,
  TextQuestionResponse,
} from '../../../../core/models/question.model';
import { ButtonComponent } from '../../../../shared/components/button/button.component';
import { McqOptionBuilderComponent, McqBuilderValue } from '../mcq-option-builder/mcq-option-builder.component';
import { KeywordListComponent } from '../keyword-list/keyword-list.component';
import { GroupChildrenBuilderComponent } from '../group-children-builder/group-children-builder.component';
import { TestCaseBuilderComponent } from '../test-case-builder/test-case-builder.component';

@Component({
  selector: 'dap-question-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, McqOptionBuilderComponent, KeywordListComponent, GroupChildrenBuilderComponent, TestCaseBuilderComponent],
  templateUrl: './question-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly questionService = inject(QuestionService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly existingQuestion = input<QuestionResponse | undefined>(undefined);

  readonly questionAdded = output<QuestionResponse>();
  readonly cancelled = output<void>();

  readonly selectedType = signal<QuestionType | ''>('');
  readonly saving = signal(false);
  readonly errorMsg = signal('');
  readonly availableBanks = signal<QuestionBankResponse[]>([]);
  readonly selectedBankIds = signal<Set<string>>(new Set());

  readonly groupChildren = signal<GroupChildRequest[]>([]);
  readonly orderedGroup = signal(false);

  readonly followUpKeywords = signal<string[]>([]);

  private currentKeywords: string[] = [];
  private currentMcqValue: McqBuilderValue = { options: [], correctAnswers: [], isValid: false };
  private currentTestCases: TestCaseRequest[] = [];

  readonly mcqBuilder = viewChild<McqOptionBuilderComponent>('mcqBuilder');
  readonly keywordList = viewChild<KeywordListComponent>('keywordList');
  readonly testCaseBuilder = viewChild<TestCaseBuilderComponent>('testCaseBuilder');

  readonly form = this.fb.nonNullable.group({
    questionText: ['', [Validators.required, Validators.minLength(10)]],
    marks: [1, [Validators.required, Validators.min(1)]],
    followUpQuestion: [''],
    followUpMarks: [1, [Validators.required, Validators.min(1)]],
    language: ['' as CodingQuestionLanguage | ''],
  });

  readonly questionTypes: QuestionType[] = ['MCQ', 'MCQ_PLUS', 'TEXT', 'GROUP', 'CODING'];

  readonly codingLanguages: { value: CodingQuestionLanguage; label: string }[] = [
    { value: 'JAVA', label: 'Java' },
    { value: 'PYTHON', label: 'Python' },
    { value: 'CSHARP', label: 'C#' },
  ];

  readonly isEditMode = computed(() => !!this.existingQuestion());

  readonly mcqInitialOptions = computed(() => {
    const q = this.existingQuestion();
    if (!q || (this.resolveType(q) !== 'MCQ' && this.resolveType(q) !== 'MCQ_PLUS')) return [];
    const mq = q as McqQuestionResponse | McqPlusQuestionResponse;
    return mq.options.map(opt => ({ text: opt, correct: mq.correctAnswers.includes(opt) }));
  });

  readonly textInitialKeywords = computed(() => {
    const q = this.existingQuestion();
    if (!q || this.resolveType(q) !== 'TEXT') return [];
    return (q as TextQuestionResponse).keywords ?? [];
  });

  readonly groupInitialChildren = computed(() => {
    const q = this.existingQuestion();
    if (!q || this.resolveType(q) !== 'GROUP') return [];
    return (q as GroupQuestionResponse).children ?? [];
  });

  readonly codingInitialTestCases = computed(() => {
    const q = this.existingQuestion();
    if (!q || this.resolveType(q) !== 'CODING') return [];
    return (q as CodingQuestionResponse).testCases ?? [];
  });

  ngOnInit(): void {
    this.questionBankService.getQuestionBanks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: banks => this.availableBanks.set(banks) });

    const q = this.existingQuestion();
    if (!q) return;

    const type = this.resolveType(q);
    this.selectedType.set(type);
    this.form.patchValue({ questionText: q.question });

    const bankIds = new Set(q.questionBanks?.map(b => b.id) ?? []);
    this.selectedBankIds.set(bankIds);

    if (type === 'TEXT') {
      this.form.patchValue({ marks: (q as TextQuestionResponse).marks });
    }
    if (type === 'DOC') {
      this.form.patchValue({ marks: (q as DocQuestionResponse).marks });
    }
    if (type === 'GROUP') {
      this.orderedGroup.set((q as GroupQuestionResponse).ordered);
      this.groupChildren.set((q as GroupQuestionResponse).children.map(c => ({
        questionText: c.questionText,
        keywords: [...c.keywords],
        marks: c.marks,
      })));
    }
    if (type === 'MCQ_PLUS') {
      const mq = q as McqPlusQuestionResponse;
      this.form.patchValue({ followUpQuestion: mq.followUpQuestion, followUpMarks: mq.followUpMarks });
      this.followUpKeywords.set([...(mq.followUpKeywords ?? [])]);
    }
    if (type === 'CODING') {
      this.form.patchValue({ language: (q as CodingQuestionResponse).language });
    }
  }

  onTypeChange(type: QuestionType | ''): void {
    this.selectedType.set(type);
    this.errorMsg.set('');
  }

  isBankSelected(id: string): boolean {
    return this.selectedBankIds().has(id);
  }

  toggleBank(id: string): void {
    this.selectedBankIds.update(ids => {
      const next = new Set(ids);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  onKeywordsChange(keywords: string[]): void { this.currentKeywords = keywords; }
  onMcqValueChange(value: McqBuilderValue): void { this.currentMcqValue = value; }
  onGroupChildrenChange(children: GroupChildRequest[]): void { this.groupChildren.set(children); }
  onFollowUpKeywordsChange(keywords: string[]): void { this.followUpKeywords.set(keywords); }
  onTestCasesChange(testCases: TestCaseRequest[]): void { this.currentTestCases = testCases; }

  cancel(): void { this.cancelled.emit(); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const type = this.selectedType();
    if (!type) { this.errorMsg.set('Please select a question type.'); return; }
    if (this.selectedBankIds().size === 0) { this.errorMsg.set('Select at least one question bank.'); return; }

    const { questionText, marks, followUpQuestion, followUpMarks, language } = this.form.getRawValue();
    const questionBankIds = Array.from(this.selectedBankIds());
    this.errorMsg.set('');

    if (type === 'MCQ') {
      const builder = this.mcqBuilder();
      const mcqVal = builder ? builder.getValue() : this.currentMcqValue;
      if (!mcqVal.isValid) { this.errorMsg.set('Add at least one option and mark a correct answer.'); return; }
      this.save({ type: 'MCQ', questionBankIds, question: questionText, options: mcqVal.options, correctAnswers: mcqVal.correctAnswers });
      return;
    }

    if (type === 'MCQ_PLUS') {
      const builder = this.mcqBuilder();
      const mcqVal = builder ? builder.getValue() : this.currentMcqValue;
      if (!mcqVal.isValid) { this.errorMsg.set('Add at least one option and mark a correct answer.'); return; }
      if (!followUpQuestion.trim()) { this.errorMsg.set('Follow-up question is required.'); return; }
      this.save({
        type: 'MCQ_PLUS', questionBankIds, question: questionText,
        options: mcqVal.options, correctAnswers: mcqVal.correctAnswers,
        followUpQuestion: followUpQuestion.trim(),
        followUpKeywords: this.followUpKeywords(),
        followUpMarks,
      });
      return;
    }

    if (type === 'TEXT') {
      const keywords = this.keywordList()?.keywords() ?? this.currentKeywords;
      this.save({ type: 'TEXT', questionBankIds, question: questionText, keywords: [...keywords], marks });
      return;
    }

    if (type === 'DOC') {
      this.save({ type: 'DOC', questionBankIds, question: questionText, marks });
      return;
    }

    if (type === 'GROUP') {
      const children = this.groupChildren();
      if (children.length === 0) { this.errorMsg.set('Add at least one child question.'); return; }
      this.save({ type: 'GROUP', questionBankIds, question: questionText, ordered: this.orderedGroup(), children });
      return;
    }

    if (type === 'CODING') {
      if (!language) { this.errorMsg.set('Select a language for the coding question.'); return; }
      const builder = this.testCaseBuilder();
      if (builder) {
        builder.markTouched();
        if (!builder.isValid) { this.errorMsg.set('Fix the test case errors before submitting.'); return; }
      }
      const testCases = builder ? builder.rows() : this.currentTestCases;
      this.save({ type: 'CODING', questionBankIds, question: questionText, language, testCases: [...testCases] });
    }
  }

  resolveType(q: QuestionResponse): QuestionType {
    if (q.type) return q.type;
    if ('followUpQuestion' in q) return 'MCQ_PLUS';
    if ('correctAnswers' in q) return 'MCQ';
    if ('children' in q) return 'GROUP';
    if ('keywords' in q) return 'TEXT';
    if ('testCases' in q) return 'CODING';
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
        this.isEditMode()
          ? this.toastService.update('Question updated.')
          : this.toastService.success('Question created.');
        this.questionAdded.emit(res);
      },
      error: () => {
        this.saving.set(false);
        this.errorMsg.set('Failed to save question. Please try again.');
        this.toastService.error('Failed to save question. Please try again.');
      },
    });
  }
}
