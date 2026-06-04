import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { AvatarComponent } from '../../../shared/components/avatar/avatar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { Assessment } from '../../../core/models/assessment.model';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AssessmentGenerateComponent, NavigateToQuestionsPayload } from '../../assessment-generation/components/assessment-generate/assessment-generate.component';

@Component({
  selector: 'dap-assessment-list',
  standalone: true,
  imports: [BadgeComponent, AvatarComponent, ButtonComponent, AssessmentGenerateComponent],
  templateUrl: './assessment-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssessmentListComponent implements OnInit {
  private readonly assessmentService = inject(AssessmentService);
  private readonly router            = inject(Router);
  private readonly destroyRef        = inject(DestroyRef);

  readonly assessments    = signal<Assessment[]>([]);
  readonly totalPages     = signal(0);
  readonly totalElements  = signal(0);
  readonly currentPage    = signal(0);
  readonly pageSize       = 10;
  readonly loading        = signal(false);
  readonly showModal      = signal(false);

  ngOnInit(): void {
    this.loadPage(0);
  }

  private loadPage(page: number): void {
    this.loading.set(true);
    this.assessmentService
      .getAssessments(page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.assessments.set(response.content);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
          this.currentPage.set(response.number);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  prevPage(): void {
    if (this.currentPage() > 0) this.loadPage(this.currentPage() - 1);
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) this.loadPage(this.currentPage() + 1);
  }

  onMark(a: Assessment): void {
    this.router.navigate(['/marking', a.id], {
      state: {
        candidateName: a.candidateName,
        assessmentMeta: `${a.bankName} · ${a.timeLimitMinutes} min`,
      },
    });
  }

  onNavigateToQuestions(payload: NavigateToQuestionsPayload): void {
    this.showModal.set(false);
    this.router.navigate(['/assessments/generate'], {
      queryParams: {
        candidateId:   payload.candidateId,
        candidateName: payload.candidateName,
        timeLimit:     payload.timeLimit,
      },
    });
  }
}
