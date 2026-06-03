import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { PageResponse } from '../models/user.model';
import {
  AssessmentSummaryResponse,
  FeedbackUpdateRequest,
  ResponseReviewItem,
} from '../models/marking.model';

@Injectable({ providedIn: 'root' })
export class MarkingService {
  getSubmittedAssessments(page: number, size: number): Observable<PageResponse<AssessmentSummaryResponse>> {
    void page;
    void size;
    return EMPTY;
  }

  getResponsesForReview(assessmentId: string): Observable<ResponseReviewItem[]> {
    void assessmentId;
    return EMPTY;
  }

  updateResponseFeedback(
    assessmentId: string,
    responseId: string,
    request: FeedbackUpdateRequest
  ): Observable<void> {
    void assessmentId;
    void responseId;
    void request;
    return EMPTY;
  }

  updateQuestionFeedback(
    assessmentId: string,
    questionId: string,
    request: FeedbackUpdateRequest
  ): Observable<void> {
    void assessmentId;
    void questionId;
    void request;
    return EMPTY;
  }

  finaliseMarking(assessmentId: string): Observable<void> {
    void assessmentId;
    return EMPTY;
  }
}
