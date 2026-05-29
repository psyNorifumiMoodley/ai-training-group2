import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserService } from '../../../../core/services/user.service';
import { MarkerResponse } from '../../../../core/models/user.model';
import { MarkerFormComponent } from '../marker-form/marker-form.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-marker-list',
  standalone: true,
  imports: [MarkerFormComponent],
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

  ngOnInit(): void {
    this.loadMarkers();
  }

  loadMarkers(): void {
    this.loading.set(true);
    this.userService.getMarkers(this.currentPage(), PAGE_SIZE)
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

  openForm(): void {
    this.showForm.set(true);
  }

  onFormSaved(): void {
    this.showForm.set(false);
    this.currentPage.set(0);
    this.loadMarkers();
  }

  onFormCancelled(): void {
    this.showForm.set(false);
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadMarkers();
  }
}
