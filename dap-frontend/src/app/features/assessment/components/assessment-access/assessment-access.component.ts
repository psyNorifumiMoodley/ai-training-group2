import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CandidateAssessmentService } from '../../../../core/services/candidate-assessment.service';
import { AuthService } from '../../../../core/services/auth.service';

type ErrorType = 'expired' | 'submitted' | null;

@Component({
  selector: 'dap-assessment-access',
  standalone: true,
  templateUrl: './assessment-access.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssessmentAccessComponent implements OnInit {
  readonly token = input.required<string>();

  private readonly service = inject(CandidateAssessmentService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly errorType = signal<ErrorType>(null);

  ngOnInit(): void {
    this.service.accessAssessment(this.token()).subscribe({
      next: (session) => {
        this.loading.set(false);
        this.auth.storeToken(session.candidateToken);
        this.router.navigate(['/assessment', session.assessmentId, 'take'], {
          state: { session },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorType.set(err.status === 409 ? 'submitted' : 'expired');
      },
    });
  }
}
