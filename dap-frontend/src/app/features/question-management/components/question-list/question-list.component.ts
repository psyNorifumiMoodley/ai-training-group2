import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { QuestionService } from '../../../../core/services/question.service';
import { ToastService } from '../../../../core/services/toast.service';
import { QuestionResponse, QuestionType } from '../../../../core/models/question.model';
import { ButtonComponent } from '../../../../shared/components/button/button.component';
import { TagComponent } from '../../../../shared/components/tag/tag.component';
import { QuestionFormComponent } from '../question-form/question-form.component';
import { ConfirmModalComponent } from '../../../../shared/components/confirm-modal/confirm-modal.component';

@Component({
  selector: 'dap-question-list',
  standalone: true,
  imports: [ButtonComponent, TagComponent, QuestionFormComponent, ConfirmModalComponent],
  templateUrl: './question-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionListComponent {
  private readonly questionService = inject(QuestionService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly questions = signal<QuestionResponse[]>([]);
  readonly categories = ['Java Core', 'Angular', 'Spring Boot', 'SQL Basics'];
  readonly selectedCategory = signal<string>('');
  readonly typeFilter = signal<QuestionType | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly editingQuestion = signal<QuestionResponse | null>(null);
  readonly deletingQuestion = signal<QuestionResponse | null>(null);
  readonly deleting = signal(false);

  readonly PAGE_SIZE = 20;
  readonly filterTypes: Array<QuestionType | 'ALL'> = ['ALL', 'MCQ', 'TEXT', 'DOC', 'GROUP'];

  readonly filteredQuestions = computed(() => {
    const type = this.typeFilter();
    if (type === 'ALL') return this.questions();
    return this.questions().filter(q => this.resolveType(q) === type);
  });

  constructor() {
    this.loadQuestions();
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
          this.toastService.removed('Question deleted.');
          this.page.set(0);
          this.loadQuestions();
        },
        error: () => {
          this.deleting.set(false);
          this.deletingQuestion.set(null);
          this.toastService.error('Failed to delete question. Please try again.');
        },
      });
  }

  onQuestionSaved(): void {
    this.showForm.set(false);
    this.editingQuestion.set(null);
    this.page.set(0);
    this.loadQuestions();
  }

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.page.set(0);
    this.loadQuestions();
  }

  prevPage(): void {
    if (this.page() > 0) { this.page.update(p => p - 1); this.loadQuestions(); }
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) { this.page.update(p => p + 1); this.loadQuestions(); }
  }

  typeTagVariant(type: QuestionType): 'mcq' | 'text' | 'doc' | 'info' {
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
    const cat = this.selectedCategory() || undefined;
    this.questionService.getQuestions(this.page(), this.PAGE_SIZE, cat)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.questions.set(page.content);
          this.totalPages.set(page.totalPages);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
