import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Assessment, AssessmentRequest, AssessmentResponse, AssessmentStatus } from '../models/assessment.model';
import { PageResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  private readonly http = inject(HttpClient);

  generateAssessment(request: AssessmentRequest): Observable<AssessmentResponse> {
    return this.http.post<AssessmentResponse>(`${environment.apiBaseUrl}/assessments`, request);
  }

  getSeenQuestionIds(candidateId: string): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiBaseUrl}/candidates/${candidateId}/seen-questions`);
  }

  getAssessments(page: number, size: number): Observable<PageResponse<Assessment>> {
    return this.http.get<PageResponse<Omit<Assessment, 'candidateInitials'> & { candidateName: string; status: AssessmentStatus; invitationLink: string | null }>>(
      `${environment.apiBaseUrl}/assessments`,
      { params: { page: String(page), size: String(size) } }
    ).pipe(
      map(response => ({
        ...response,
        content: response.content.map(a => ({
          ...a,
          candidateInitials: a.candidateName
            .split(' ')
            .map((n: string) => n[0] ?? '')
            .join('')
            .toUpperCase()
            .slice(0, 2),
        })),
      }))
    );
  }

  remindCandidate(assessmentId: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/assessments/${assessmentId}/remind`, null);
  }
}
