import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { AssessmentAccessResponse, ResponseRequest } from '../models/assessment-session.model';

@Injectable({ providedIn: 'root' })
export class CandidateAssessmentService {

  accessAssessment(token: string): Observable<AssessmentAccessResponse> {
    return EMPTY;
  }

  saveResponse(assessmentId: string, questionId: string, request: ResponseRequest): Observable<void> {
    return EMPTY;
  }

  submitAssessment(assessmentId: string): Observable<void> {
    return EMPTY;
  }
}
