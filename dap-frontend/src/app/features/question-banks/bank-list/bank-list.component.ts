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
  readonly typeFilter = signal<QuestionType | 'ALL'>('ALL');
  readonly openMenuId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly editingQuestion = signal<QuestionResponse | null>(null);
  readonly deletingQuestion = signal<QuestionResponse | null>(null);
  readonly deleting = signal(false);

  readonly filterTypes: Array<{ value: QuestionType | 'ALL'; label: string }> = [
    { value: 'ALL',   label: 'All types' },
    { value: 'MCQ',   label: 'MCQ' },
    { value: 'TEXT',  label: 'Text' },
    { value: 'DOC',   label: 'Doc' },
    { value: 'GROUP', label: 'Group' },
  ];

  readonly banks = computed<Bank[]>(() => {
    const counts = new Map<string, number>();
    for (const q of this.allQuestions()) {
      counts.set(q.category, (counts.get(q.category) ?? 0) + 1);
    }
    return Array.from(counts.entries())
      .map(([name, questionCount]) => ({ name, questionCount }))
      .sort((a, b) => a.name.localeCompare(b.name));
  });

  readonly categoryQuestions = computed(() => {
    const cat = this.selectedCategory();
    return cat ? this.allQuestions().filter(q => q.category === cat) : this.allQuestions();
  });

  readonly filteredQuestions = computed(() => {
    const type = this.typeFilter();
    const questions = this.categoryQuestions();
    if (type === 'ALL') return questions;
    return questions.filter(q => this.resolveType(q) === type);
  });

  readonly activeBank = computed(() =>
    this.banks().find(b => b.name === this.selectedCategory()) ?? null
  );

  constructor() {
    this.loadQuestions();
  }

  selectBank(name: string): void {
    this.selectedCategory.set(name);
    this.typeFilter.set('ALL');
    this.openMenuId.set(null);
  }

  setTypeFilter(type: QuestionType | 'ALL'): void {
    this.typeFilter.set(type);
    this.openMenuId.set(null);
  }

  toggleMenu(id: string): void {
    this.openMenuId.update(current => (current === id ? null : id));
  }

  closeMenu(): void {
    this.openMenuId.set(null);
  }

  openAdd(): void {
    this.editingQuestion.set(null);
    this.openMenuId.set(null);
    this.showForm.set(true);
  }

  openEdit(q: QuestionResponse): void {
    this.openMenuId.set(null);
    this.showForm.set(false);
    this.editingQuestion.set(q);
  }

  requestDelete(q: QuestionResponse): void {
    this.openMenuId.set(null);
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
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
