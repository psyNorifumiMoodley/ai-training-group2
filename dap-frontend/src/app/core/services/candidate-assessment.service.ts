import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssessmentAccessResponse, ResponseRequest } from '../models/assessment-session.model';

@Injectable({ providedIn: 'root' })
export class CandidateAssessmentService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  accessAssessment(token: string): Observable<AssessmentAccessResponse> {
    return this.http.get<AssessmentAccessResponse>(`${this.base}/assessments/access/${token}`);
  }

  saveResponse(assessmentId: string, questionId: string, request: ResponseRequest): Observable<void> {
    return this.http.put<void>(`${this.base}/assessments/${assessmentId}/responses/${questionId}`, request);
  }

  submitAssessment(assessmentId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/assessments/${assessmentId}/submit`, {});
  }
}
