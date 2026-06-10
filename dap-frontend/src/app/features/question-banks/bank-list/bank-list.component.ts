import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
  computed,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { QuestionService } from '../../../core/services/question.service';
import { QuestionBankService } from '../../../core/services/question-bank.service';
import { ToastService } from '../../../core/services/toast.service';
import { QuestionBankResponse, QuestionResponse, QuestionType } from '../../../core/models/question.model';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { TagComponent } from '../../../shared/components/tag/tag.component';
import { QuestionFormComponent } from '../../question-management/components/question-form/question-form.component';
import { ConfirmModalComponent } from '../../../shared/components/confirm-modal/confirm-modal.component';

@Component({
  selector: 'dap-bank-list',
  standalone: true,
  imports: [FormsModule, ButtonComponent, TagComponent, QuestionFormComponent, ConfirmModalComponent],
  templateUrl: './bank-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankListComponent {
  private readonly questionService = inject(QuestionService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly banks = signal<QuestionBankResponse[]>([]);
  readonly allQuestions = signal<QuestionResponse[]>([]);
  readonly selectedBankId = signal<string>('');
  readonly typeFilter = signal<QuestionType | 'ALL'>('ALL');
  readonly openMenuId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly banksLoading = signal(false);

  // Question form
  readonly showForm = signal(false);
  readonly editingQuestion = signal<QuestionResponse | null>(null);
  readonly deletingQuestion = signal<QuestionResponse | null>(null);
  readonly deleting = signal(false);

  // Create bank
  readonly showCreateBankForm = signal(false);
  readonly newBankName = signal('');
  readonly creatingBank = signal(false);
  readonly createBankError = signal('');

  // Rename bank
  readonly renamingBankId = signal<string | null>(null);
  readonly renameDraft = signal('');
  readonly renamingBank = signal(false);
  readonly renameBankError = signal('');

  // Delete bank
  readonly deletingBank = signal<QuestionBankResponse | null>(null);
  readonly deletingBankInProgress = signal(false);

  readonly filterTypes: Array<{ value: QuestionType | 'ALL'; label: string }> = [
    { value: 'ALL',   label: 'All types' },
    { value: 'MCQ',   label: 'MCQ' },
    { value: 'MCQ_PLUS', label: 'MCQ+' },
    { value: 'TEXT',  label: 'Text' },
    { value: 'DOC',   label: 'Doc' },
    { value: 'GROUP', label: 'Group' },
  ];

  readonly activeBank = computed(() =>
    this.banks().find(b => b.id === this.selectedBankId()) ?? null
  );

  readonly filteredQuestions = computed(() => {
    const type = this.typeFilter();
    if (type === 'ALL') return this.allQuestions();
    return this.allQuestions().filter(q => this.resolveType(q) === type);
  });

  constructor() {
    this.loadBanks();
    this.loadQuestions();
  }

  selectBank(bankId: string): void {
    this.selectedBankId.set(bankId);
    this.typeFilter.set('ALL');
    this.openMenuId.set(null);
    this.loadQuestions();
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

  // --- Bank CRUD ---

  submitCreateBank(): void {
    const name = this.newBankName().trim();
    if (!name) return;
    this.creatingBank.set(true);
    this.createBankError.set('');
    this.questionBankService.createQuestionBank(name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.creatingBank.set(false);
          this.newBankName.set('');
          this.showCreateBankForm.set(false);
          this.toastService.success('Question bank created.');
          this.loadBanks();
        },
        error: (err) => {
          this.creatingBank.set(false);
          if (err?.status === 409) {
            this.createBankError.set('A bank with this name already exists.');
          } else {
            this.toastService.error('Failed to create question bank.');
          }
        },
      });
  }

  startRename(bank: QuestionBankResponse): void {
    this.renamingBankId.set(bank.id);
    this.renameDraft.set(bank.name);
    this.renameBankError.set('');
  }

  cancelRename(): void {
    this.renamingBankId.set(null);
    this.renameDraft.set('');
    this.renameBankError.set('');
  }

  submitRenameBank(bank: QuestionBankResponse): void {
    const name = this.renameDraft().trim();
    if (!name) return;
    this.renamingBank.set(true);
    this.renameBankError.set('');
    this.questionBankService.renameQuestionBank(bank.id, name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.renamingBank.set(false);
          this.renamingBankId.set(null);
          this.toastService.update('Question bank renamed.');
          this.loadBanks();
        },
        error: (err) => {
          this.renamingBank.set(false);
          if (err?.status === 409) {
            this.renameBankError.set('A bank with this name already exists.');
          } else {
            this.toastService.error('Failed to rename question bank.');
          }
        },
      });
  }

  requestDeleteBank(bank: QuestionBankResponse): void {
    this.deletingBank.set(bank);
  }

  confirmDeleteBank(): void {
    const bank = this.deletingBank();
    if (!bank) return;
    this.deletingBankInProgress.set(true);
    this.questionBankService.deleteQuestionBank(bank.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingBankInProgress.set(false);
          this.deletingBank.set(null);
          if (this.selectedBankId() === bank.id) this.selectedBankId.set('');
          this.toastService.success('Question bank deleted.');
          this.loadBanks();
          this.loadQuestions();
        },
        error: (err) => {
          this.deletingBankInProgress.set(false);
          this.deletingBank.set(null);
          if (err?.status === 409) {
            this.toastService.error('This bank still has questions — remove them first.');
          } else {
            this.toastService.error('Failed to delete question bank.');
          }
        },
      });
  }

  typeVariant(type: QuestionType): 'mcq' | 'text' | 'doc' | 'info' | 'coding' {
    const map: Record<QuestionType, 'mcq' | 'text' | 'doc' | 'info' | 'coding'> = {
      MCQ: 'mcq', MCQ_PLUS: 'mcq', TEXT: 'text', DOC: 'doc', GROUP: 'info', CODING: 'coding',
    };
    return map[type];
  }

  resolveType(q: QuestionResponse): QuestionType {
    if (q.type) return q.type;
    if ('correctAnswers' in q) return 'MCQ';
    if ('children' in q) return 'GROUP';
    if ('keywords' in q) return 'TEXT';
    return 'DOC';
  }

  private loadBanks(): void {
    this.banksLoading.set(true);
    this.questionBankService.getQuestionBanks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: banks => {
          this.banks.set(banks);
          this.banksLoading.set(false);
        },
        error: () => this.banksLoading.set(false),
      });
  }

  private loadQuestions(): void {
    this.loading.set(true);
    const bankId = this.selectedBankId() || undefined;
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
