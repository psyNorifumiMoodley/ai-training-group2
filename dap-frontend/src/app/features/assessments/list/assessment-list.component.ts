import { ChangeDetectionStrategy, Component, computed, DestroyRef, HostListener, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { AvatarComponent } from '../../../shared/components/avatar/avatar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { Assessment, AssessmentStatus } from '../../../core/models/assessment.model';
import { AssessmentService } from '../../../core/services/assessment.service';
import { ToastService } from '../../../core/services/toast.service';
import { AssessmentGenerateComponent, NavigateToQuestionsPayload } from '../../assessment-generation/components/assessment-generate/assessment-generate.component';

@Component({
  selector: 'dap-assessment-list',
  standalone: true,
  imports: [BadgeComponent, AvatarComponent, ButtonComponent, AssessmentGenerateComponent, SlicePipe],
  templateUrl: './assessment-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssessmentListComponent implements OnInit {
  private readonly assessmentService = inject(AssessmentService);
  private readonly toastService      = inject(ToastService);
  private readonly router            = inject(Router);
  private readonly destroyRef        = inject(DestroyRef);

  readonly assessments    = signal<Assessment[]>([]);
  readonly totalPages     = signal(0);
  readonly totalElements  = signal(0);
  readonly currentPage    = signal(0);
  readonly pageSize       = 10;
  readonly loading        = signal(false);
  readonly showModal      = signal(false);

  readonly searchText   = signal('');
  readonly statusFilter = signal<AssessmentStatus | 'ALL'>('SUBMITTED');

  readonly copiedId        = signal<string | null>(null);
  readonly openDropdownId  = signal<string | null>(null);

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openDropdownId.set(null);
  }

  readonly filteredAssessments = computed(() => {
    const q      = this.searchText().toLowerCase();
    const status = this.statusFilter();
    return this.assessments().filter(a => {
      const nameMatch   = !q || a.candidateName.toLowerCase().includes(q);
      const statusMatch = status === 'ALL' || a.status === status;
      return nameMatch && statusMatch;
    });
  });

  readonly statusOptions: Array<{ label: string; value: AssessmentStatus | 'ALL' }> = [
    { label: 'All',         value: 'ALL' },
    { label: 'Pending',     value: 'PENDING' },
    { label: 'In Progress', value: 'IN_PROGRESS' },
    { label: 'Submitted',   value: 'SUBMITTED' },
    { label: 'Marked',      value: 'MARKED' },
  ];

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
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
        assessmentMeta: `${a.timeLimitMinutes} min`,
      },
    });
  }

  onCopyLink(a: Assessment): void {
    if (!a.invitationLink) return;
    navigator.clipboard.writeText(a.invitationLink).then(() => {
      this.copiedId.set(a.id);
      setTimeout(() => this.copiedId.set(null), 2000);
    });
  }

  toggleDropdown(id: string, e: MouseEvent): void {
    e.stopPropagation();
    this.openDropdownId.update(current => current === id ? null : id);
  }

  closeDropdown(): void {
    this.openDropdownId.set(null);
  }

  onRemind(a: Assessment): void {
    this.assessmentService.remindCandidate(a.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.toastService.success(`Reminder sent to ${a.candidateName}.`),
        error: () => this.toastService.error('Failed to send reminder. Please try again.'),
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
