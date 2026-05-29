import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserService } from '../../../../core/services/user.service';
import { CandidateResponse } from '../../../../core/models/user.model';
import { CandidateFormComponent } from '../candidate-form/candidate-form.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-candidate-list',
  standalone: true,
  imports: [CandidateFormComponent],
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

  ngOnInit(): void {
    this.loadCandidates();
  }

  loadCandidates(): void {
    this.loading.set(true);
    this.userService.getCandidates(this.currentPage(), PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
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
}
