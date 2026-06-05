import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/user.model';
import { AssessmentSummaryResponse, FeedbackUpdateRequest, ResponseReviewItem } from '../models/marking.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MarkingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/assessments`;

  getSubmittedAssessments(page: number, size: number): Observable<PageResponse<AssessmentSummaryResponse>> {
    return this.http.get<PageResponse<AssessmentSummaryResponse>>(this.base, {
      params: { status: 'SUBMITTED', page: String(page), size: String(size) },
    });
  }

  getResponsesForReview(assessmentId: string): Observable<ResponseReviewItem[]> {
    return this.http.get<ResponseReviewItem[]>(`${this.base}/${assessmentId}/responses`);
  }

  updateResponseFeedback(
    assessmentId: string,
    responseId: string,
    request: FeedbackUpdateRequest
  ): Observable<void> {
    return this.http.patch<void>(`${this.base}/${assessmentId}/responses/${responseId}`, request);
  }

  finaliseMarking(assessmentId: string, overallFeedback: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${assessmentId}/finalise`, { overallFeedback });
  }
}
