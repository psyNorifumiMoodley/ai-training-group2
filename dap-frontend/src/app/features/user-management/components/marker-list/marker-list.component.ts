import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { UserService } from '../../../../core/services/user.service';
import { MarkerResponse } from '../../../../core/models/user.model';
import { MarkerFormComponent } from '../marker-form/marker-form.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-marker-list',
  standalone: true,
  imports: [MarkerFormComponent, ReactiveFormsModule],
  templateUrl: './marker-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkerListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  readonly markers = signal<MarkerResponse[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly editingMarker = signal<MarkerResponse | null>(null);
  readonly confirmDeleteId = signal<string | null>(null);
  readonly deleteError = signal<string | null>(null);
  readonly deleting = signal(false);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly sortBy = signal<string>('name');
  readonly sortDir = signal<'asc' | 'desc'>('asc');

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.currentPage.set(0);
      this.loadMarkers();
    });
    this.loadMarkers();
  }

  loadMarkers(): void {
    this.loading.set(true);
    const search = this.searchControl.value || undefined;
    this.userService.getMarkers(this.currentPage(), PAGE_SIZE, search, this.sortBy(), this.sortDir())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.markers.set(page.content);
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
    this.loadMarkers();
  }

  openForm(): void {
    this.editingMarker.set(null);
    this.showForm.set(true);
  }

  openEditForm(marker: MarkerResponse): void {
    this.editingMarker.set(marker);
    this.showForm.set(true);
  }

  onFormSaved(): void {
    this.showForm.set(false);
    this.editingMarker.set(null);
    this.currentPage.set(0);
    this.loadMarkers();
  }

  onFormCancelled(): void {
    this.showForm.set(false);
    this.editingMarker.set(null);
  }

  requestDelete(id: string): void {
    this.deleteError.set(null);
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (!id) return;
    this.deleting.set(true);
    this.userService.deleteMarker(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deleting.set(false);
          this.confirmDeleteId.set(null);
          this.loadMarkers();
        },
        error: (err) => {
          this.deleting.set(false);
          this.confirmDeleteId.set(null);
          this.deleteError.set(
            err.status === 409
              ? 'This marker cannot be deleted because they have submitted markings.'
              : 'Could not delete this marker.'
          );
        }
      });
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadMarkers();
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString();
  }
}
