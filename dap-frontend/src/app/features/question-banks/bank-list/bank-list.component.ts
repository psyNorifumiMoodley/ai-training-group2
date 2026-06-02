import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { QuestionService } from '../../../core/services/question.service';
import { QuestionResponse, QuestionType } from '../../../core/models/question.model';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { TagComponent } from '../../../shared/components/tag/tag.component';
import { QuestionFormComponent } from '../../question-management/components/question-form/question-form.component';
import { ConfirmModalComponent } from '../../../shared/components/confirm-modal/confirm-modal.component';

interface Bank {
  name: string;
  questionCount: number;
}

@Component({
  selector: 'dap-bank-list',
  standalone: true,
  imports: [ButtonComponent, TagComponent, QuestionFormComponent, ConfirmModalComponent],
  templateUrl: './bank-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankListComponent {
  private readonly questionService = inject(QuestionService);
  private readonly destroyRef = inject(DestroyRef);

  readonly allQuestions = signal<QuestionResponse[]>([]);
  readonly selectedCategory = signal<string>('');
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly editingQuestion = signal<QuestionResponse | null>(null);
  readonly deletingQuestion = signal<QuestionResponse | null>(null);
  readonly deleting = signal(false);

  readonly banks = computed<Bank[]>(() => {
    const counts = new Map<string, number>();
    for (const q of this.allQuestions()) {
      counts.set(q.category, (counts.get(q.category) ?? 0) + 1);
    }
    return Array.from(counts.entries()).map(([name, questionCount]) => ({ name, questionCount }));
  });

  readonly filteredQuestions = computed(() => {
    const cat = this.selectedCategory();
    return cat ? this.allQuestions().filter(q => q.category === cat) : this.allQuestions();
  });

  readonly activeBank = computed(() =>
    this.banks().find(b => b.name === this.selectedCategory()) ?? null
  );

  constructor() {
    this.loadQuestions();
  }

  selectBank(name: string): void {
    this.selectedCategory.set(name);
  }

  openAdd(): void {
    this.editingQuestion.set(null);
    this.showForm.set(true);
  }

  openEdit(q: QuestionResponse): void {
    this.showForm.set(false);
    this.editingQuestion.set(q);
  }

  requestDelete(q: QuestionResponse): void {
    this.deletingQuestion.set(q);
  }

  confirmDelete(): void {
    const q = this.deletingQuestion();
    if (!q) return;
    this.deleting.set(true);
    this.questionService.deleteQuestion(q.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deleting.set(false);
          this.deletingQuestion.set(null);
          this.loadQuestions();
        },
        error: () => {
          this.deleting.set(false);
          this.deletingQuestion.set(null);
        },
      });
  }

  onQuestionSaved(): void {
    this.showForm.set(false);
    this.editingQuestion.set(null);
    this.loadQuestions();
  }

  typeVariant(type: QuestionType): 'mcq' | 'text' | 'doc' | 'info' {
    const map: Record<QuestionType, 'mcq' | 'text' | 'doc' | 'info'> = {
      MCQ: 'mcq', TEXT: 'text', DOC: 'doc', GROUP: 'info',
    };
    return map[type];
  }

  resolveType(q: QuestionResponse): QuestionType {
    if (q.type) return q.type;
    if ('correctAnswers' in q) return 'MCQ';
    if ('followUpQuestions' in q) return 'GROUP';
    if ('keywords' in q) return 'TEXT';
    return 'DOC';
  }

  private loadQuestions(): void {
    this.loading.set(true);
    this.questionService.getQuestions(0, 1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.allQuestions.set(page.content);
          if (!this.selectedCategory() && page.content.length > 0) {
            this.selectedCategory.set(page.content[0].category);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
