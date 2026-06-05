import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';
import { Assessment } from '../../core/models/assessment.model';
import { DashboardStats } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { AssessmentService } from '../../core/services/assessment.service';

@Component({
  selector: 'dap-dashboard',
  standalone: true,
  imports: [StatCardComponent, BadgeComponent, AvatarComponent, ProgressBarComponent],
  templateUrl: './dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService  = inject(DashboardService);
  private readonly assessmentService = inject(AssessmentService);
  private readonly destroyRef        = inject(DestroyRef);

  readonly stats        = signal<DashboardStats | null>(null);
  readonly assessments  = signal<Assessment[]>([]);
  readonly loadingStats = signal(true);
  readonly loadingList  = signal(true);

  /** Percentage of each status bucket relative to total assessments. */
  readonly pendingPct    = computed(() => this.pct(this.stats()?.pendingCount));
  readonly inProgressPct = computed(() => this.pct(this.stats()?.inProgressCount));
  readonly submittedPct  = computed(() => this.pct(this.stats()?.submittedCount));
  readonly markedPct     = computed(() => this.pct(this.stats()?.markedCount));

  ngOnInit(): void {
    this.loadStats();
    this.loadRecentAssessments();
  }

  private loadStats(): void {
    this.dashboardService
      .getStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: stats => {
          this.stats.set(stats);
          this.loadingStats.set(false);
        },
        error: () => this.loadingStats.set(false),
      });
  }

  private loadRecentAssessments(): void {
    this.assessmentService
      .getAssessments(0, 5)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.assessments.set(response.content);
          this.loadingList.set(false);
        },
        error: () => this.loadingList.set(false),
      });
  }

  private pct(count: number | undefined): number {
    const total = this.stats()?.totalAssessments ?? 0;
    if (!count || total === 0) return 0;
    return Math.round((count / total) * 100);
  }
}
