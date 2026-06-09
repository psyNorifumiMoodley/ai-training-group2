import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { UserService } from '../../../../core/services/user.service';
import { CandidateResponse } from '../../../../core/models/user.model';
import { CandidateFormComponent } from '../candidate-form/candidate-form.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-candidate-list',
  standalone: true,
  imports: [CandidateFormComponent, ReactiveFormsModule, RouterLink],
  templateUrl: './candidate-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  readonly candidates = signal<CandidateResponse[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly loading = signal(false);
  readonly showForm = signal(false);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly sortBy = signal<string>('name');
  readonly sortDir = signal<'asc' | 'desc'>('asc');
  readonly statusFilter = signal<string>('');

  readonly statusOptions = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'SUBMITTED', label: 'Submitted' },
    { value: 'MARKED', label: 'Marked' },
    { value: 'CLOSED', label: 'Closed' },
  ];

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.currentPage.set(0);
      this.loadCandidates();
    });
    this.loadCandidates();
  }

  loadCandidates(): void {
    this.loading.set(true);
    const search = this.searchControl.value || undefined;
    const status = this.statusFilter() || undefined;
    this.userService.getCandidates(
      this.currentPage(),
      PAGE_SIZE,
      search,
      this.sortBy(),
      this.sortDir(),
      status
    ).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.candidates.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  toggleSort(column: string): void {
    if (this.sortBy() === column) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortBy.set(column);
      this.sortDir.set('asc');
    }
    this.currentPage.set(0);
    this.loadCandidates();
  }

  onStatusFilterChange(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value);
    this.currentPage.set(0);
    this.loadCandidates();
  }

  openForm(): void {
    this.showForm.set(true);
  }

  onFormSaved(): void {
    this.showForm.set(false);
    this.currentPage.set(0);
    this.loadCandidates();
  }

  onFormCancelled(): void {
    this.showForm.set(false);
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadCandidates();
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString();
  }
}
