import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { QuestionService } from '../../../../core/services/question.service';
import { QuestionBankService } from '../../../../core/services/question-bank.service';
import { QuestionBankResponse, QuestionResponse, QuestionType } from '../../../../core/models/question.model';
import { TagComponent } from '../../../../shared/components/tag/tag.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'dap-question-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagComponent, ButtonComponent],
  templateUrl: './question-picker.component.html',
})
export class QuestionPickerComponent {
  private readonly questionService = inject(QuestionService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly destroyRef = inject(DestroyRef);

  readonly seenQuestionIds   = input.required<string[]>();
  readonly questionsSelected = output<string[]>();

  readonly banks           = signal<QuestionBankResponse[]>([]);
  readonly allQuestions    = signal<QuestionResponse[]>([]);
  readonly selectedBankId  = signal('');
  readonly checkedIds      = signal<Set<string>>(new Set());
  readonly loading         = signal(false);
  readonly banksLoading    = signal(false);

  readonly seenSet = computed(() => new Set(this.seenQuestionIds()));
  readonly filteredQuestions = computed(() => this.allQuestions());
  readonly checkedCount = computed(() => this.checkedIds().size);
  readonly selectedBankName = computed(() =>
    this.banks().find(b => b.id === this.selectedBankId())?.name ?? 'Questions'
  );

  constructor() {
    this.banksLoading.set(true);
    this.questionBankService.getQuestionBanks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: banks => {
          this.banks.set(banks);
          this.banksLoading.set(false);
          if (banks.length > 0) {
            this.loadQuestionsForBank(banks[0].id);
            this.selectedBankId.set(banks[0].id);
          }
        },
        error: () => this.banksLoading.set(false),
      });
  }

  selectBank(id: string): void {
    this.selectedBankId.set(id);
    this.loadQuestionsForBank(id);
  }

  toggle(id: string): void {
    const next = new Set(this.checkedIds());
    next.has(id) ? next.delete(id) : next.add(id);
    this.checkedIds.set(next);
  }

  isChecked(id: string): boolean { return this.checkedIds().has(id); }
  isSeen(id: string): boolean    { return this.seenSet().has(id); }

  typeVariant(type: QuestionType): 'mcq' | 'text' | 'doc' | 'info' | 'coding' {
    const map: Record<QuestionType, 'mcq' | 'text' | 'doc' | 'info' | 'coding'> = {
      MCQ: 'mcq', MCQ_PLUS: 'mcq', TEXT: 'text', DOC: 'doc', GROUP: 'info', CODING: 'coding',
    };
    return map[type];
  }

  confirm(): void {
    this.questionsSelected.emit(Array.from(this.checkedIds()));
  }

  private loadQuestionsForBank(bankId: string): void {
    this.loading.set(true);
    this.questionService.getQuestions(0, 100, bankId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.allQuestions.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
