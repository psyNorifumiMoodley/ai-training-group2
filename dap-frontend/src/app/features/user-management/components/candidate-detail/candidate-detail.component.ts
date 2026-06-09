import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CandidateResponse } from '../../../../core/models/user.model';
import { AssessmentResponse } from '../../../../core/models/assessment.model';
import { CandidateFormComponent } from '../candidate-form/candidate-form.component';
import { BadgeComponent } from '../../../../shared/components/badge/badge.component';

@Component({
  selector: 'app-candidate-detail',
  standalone: true,
  imports: [RouterLink, CandidateFormComponent, BadgeComponent],
  templateUrl: './candidate-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);
  readonly authService = inject(AuthService);

  readonly candidate = signal<CandidateResponse | null>(null);
  readonly assessments = signal<AssessmentResponse[]>([]);
  readonly loading = signal(true);
  readonly loadingAssessments = signal(true);
  readonly showEditForm = signal(false);
  readonly confirmingDelete = signal(false);
  readonly deleting = signal(false);
  readonly error = signal<string | null>(null);

  get isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadCandidate(id);
    this.loadAssessments(id);
  }

  private loadCandidate(id: string): void {
    this.userService.getCandidateById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (c) => {
          this.candidate.set(c);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Candidate not found.');
        }
      });
  }

  private loadAssessments(id: string): void {
    this.userService.getCandidateAssessments(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          this.assessments.set(list);
          this.loadingAssessments.set(false);
        },
        error: () => this.loadingAssessments.set(false)
      });
  }

  openEditForm(): void {
    this.showEditForm.set(true);
  }

  onFormSaved(): void {
    this.showEditForm.set(false);
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loading.set(true);
    this.loadCandidate(id);
  }

  onFormCancelled(): void {
    this.showEditForm.set(false);
  }

  requestDelete(): void {
    this.confirmingDelete.set(true);
  }

  cancelDelete(): void {
    this.confirmingDelete.set(false);
  }

  confirmDelete(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.deleting.set(true);
    this.userService.deleteCandidate(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/candidates']),
        error: () => {
          this.deleting.set(false);
          this.confirmingDelete.set(false);
          this.error.set('Could not delete this candidate. They may have existing assessments.');
        }
      });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString();
  }
}
