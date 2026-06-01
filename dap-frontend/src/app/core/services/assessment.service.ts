import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { AssessmentRequest, AssessmentResponse, AssessmentStatus } from '../models/assessment.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  private readonly http = inject(HttpClient);

  generateAssessment(request: AssessmentRequest): Observable<AssessmentResponse> {
    // Stub — swap to: return this.http.post<AssessmentResponse>(`${environment.apiBaseUrl}/assessments`, request);
    void this.http; void environment;
    return of<AssessmentResponse>({
      id: 'asm-' + Math.random().toString(36).slice(2, 9),
      candidateId: request.candidateId,
      status: 'PENDING' as AssessmentStatus,
      invitationLink: `http://localhost:4200/assessment/stub-token-${Date.now()}`,
      timeLimitMinutes: request.timeLimitMinutes,
      createdAt: new Date().toISOString(),
    }).pipe(delay(600));
  }

  getSeenQuestionIds(candidateId: string): Observable<string[]> {
    // Stub — swap to: return this.http.get<string[]>(`${environment.apiBaseUrl}/candidates/${candidateId}/seen-questions`);
    void candidateId;
    return of<string[]>([]).pipe(delay(200));
  }
}
