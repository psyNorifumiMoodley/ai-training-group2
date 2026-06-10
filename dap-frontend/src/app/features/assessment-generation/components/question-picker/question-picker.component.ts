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
import { QuestionResponse, QuestionType } from '../../../../core/models/question.model';
import { TagComponent } from '../../../../shared/components/tag/tag.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

interface Bank { name: string; questionCount: number; }

@Component({
  selector: 'dap-question-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagComponent, ButtonComponent],
  templateUrl: './question-picker.component.html',
})
export class QuestionPickerComponent {
  private readonly questionService = inject(QuestionService);
  private readonly destroyRef      = inject(DestroyRef);

  readonly seenQuestionIds  = input.required<string[]>();
  readonly questionsSelected = output<string[]>();

  readonly allQuestions    = signal<QuestionResponse[]>([]);
  readonly selectedCategory = signal('');
  readonly checkedIds       = signal<Set<string>>(new Set());
  readonly loading          = signal(false);

  readonly seenSet = computed(() => new Set(this.seenQuestionIds()));

  readonly banks = computed<Bank[]>(() => {
    const counts = new Map<string, number>();
    for (const q of this.allQuestions()) {
      const bankName = q.questionBanks?.[0]?.name ?? '';
      counts.set(bankName, (counts.get(bankName) ?? 0) + 1);
    }
    return Array.from(counts.entries()).map(([name, questionCount]) => ({ name, questionCount }));
  });

  readonly filteredQuestions = computed(() => {
    const cat = this.selectedCategory();
    return cat
      ? this.allQuestions().filter(q => (q.questionBanks?.[0]?.name ?? '') === cat)
      : this.allQuestions();
  });

  readonly checkedCount = computed(() => this.checkedIds().size);

  constructor() {
    this.loading.set(true);
    this.questionService.getQuestions(0, 1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.allQuestions.set(page.content);
          if (!this.selectedCategory() && page.content.length > 0) {
            this.selectedCategory.set(page.content[0].questionBanks?.[0]?.name ?? '');
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  selectBank(name: string): void {
    this.selectedCategory.set(name);
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
}
