import { ChangeDetectionStrategy, Component } from '@angular/core';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';
import { Assessment } from '../../core/models/assessment.model';

const STUB_ASSESSMENTS: Assessment[] = [
  { id: '1', candidateName: 'Aisha Nkosi',      candidateInitials: 'AN', role: 'Backend Engineer',   bankName: 'Java Core',      status: 'IN_PROGRESS', assignedDate: '2026-05-28', timeLimitMinutes: 60 },
  { id: '2', candidateName: 'Liam Okonkwo',     candidateInitials: 'LO', role: 'Frontend Engineer',  bankName: 'Angular',        status: 'PENDING',     assignedDate: '2026-05-27', timeLimitMinutes: 90 },
  { id: '3', candidateName: 'Sara Patel',        candidateInitials: 'SP', role: 'Full Stack',         bankName: 'Spring Boot',    status: 'SUBMITTED',   assignedDate: '2026-05-26', timeLimitMinutes: 120 },
  { id: '4', candidateName: 'David Chen',        candidateInitials: 'DC', role: 'DevOps',             bankName: 'Cloud & Infra',  status: 'MARKED',      assignedDate: '2026-05-24', timeLimitMinutes: 60 },
  { id: '5', candidateName: 'Mia Fernandez',     candidateInitials: 'MF', role: 'Backend Engineer',   bankName: 'SQL Basics',     status: 'PENDING',     assignedDate: '2026-05-23', timeLimitMinutes: 45 },
];

const STUB_ACTIVITY = [
  { dot: 'bg-primary',   text: 'Aisha Nkosi started assessment',      time: '12 min ago' },
  { dot: 'bg-blue-400',  text: 'Sara Patel submitted for review',     time: '1 hr ago'   },
  { dot: 'bg-green-400', text: "David Chen's assessment was marked",  time: '3 hr ago'   },
  { dot: 'bg-amber-400', text: 'Liam Okonkwo invited via email',      time: 'Yesterday'  },
];

@Component({
  selector: 'dap-dashboard',
  standalone: true,
  imports: [StatCardComponent, BadgeComponent, AvatarComponent, ProgressBarComponent],
  templateUrl: './dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  readonly assessments = STUB_ASSESSMENTS;
  readonly activity    = STUB_ACTIVITY;
}
