import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssessmentRequest, AssessmentResponse } from '../models/assessment.model';
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
}
