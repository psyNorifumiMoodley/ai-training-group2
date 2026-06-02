import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { AvatarComponent } from '../../../shared/components/avatar/avatar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { Assessment } from '../../../core/models/assessment.model';
import { AssessmentGenerateComponent, NavigateToQuestionsPayload } from '../../assessment-generation/components/assessment-generate/assessment-generate.component';

// TODO: replace with API call
const STUB_ASSESSMENTS: Assessment[] = [
  { id: 'a1', candidateName: 'Aisha Nkosi',   candidateInitials: 'AN', role: 'Backend Engineer',  bankName: 'Java Core',   status: 'IN_PROGRESS', assignedDate: '2026-05-28', timeLimitMinutes: 60  },
  { id: 'a2', candidateName: 'Liam Okonkwo',  candidateInitials: 'LO', role: 'Frontend Engineer', bankName: 'Angular',     status: 'PENDING',     assignedDate: '2026-05-27', timeLimitMinutes: 90  },
  { id: 'a3', candidateName: 'Sara Patel',     candidateInitials: 'SP', role: 'Full Stack',        bankName: 'Spring Boot', status: 'SUBMITTED',   assignedDate: '2026-05-26', timeLimitMinutes: 120 },
  { id: 'a4', candidateName: 'David Chen',     candidateInitials: 'DC', role: 'DevOps',            bankName: 'Cloud',       status: 'MARKED',      assignedDate: '2026-05-24', timeLimitMinutes: 60  },
  { id: 'a5', candidateName: 'Mia Fernandez',  candidateInitials: 'MF', role: 'Backend Engineer',  bankName: 'SQL Basics',  status: 'PENDING',     assignedDate: '2026-05-23', timeLimitMinutes: 45  },
  { id: 'a6', candidateName: 'James Kimani',   candidateInitials: 'JK', role: 'Full Stack',        bankName: 'Java Core',   status: 'SUBMITTED',   assignedDate: '2026-05-21', timeLimitMinutes: 90  },
];

@Component({
  selector: 'dap-assessment-list',
  standalone: true,
  imports: [BadgeComponent, AvatarComponent, ButtonComponent, AssessmentGenerateComponent],
  templateUrl: './assessment-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssessmentListComponent {
  private readonly router = inject(Router);
  readonly assessments = STUB_ASSESSMENTS;
  readonly showModal   = signal(false);

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
