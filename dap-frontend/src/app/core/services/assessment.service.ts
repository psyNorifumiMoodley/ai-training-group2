import { Injectable } from '@angular/core';
import { Observable, EMPTY } from 'rxjs';
import { AssessmentRequest, AssessmentResponse } from '../models/assessment.model';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  generateAssessment(_request: AssessmentRequest): Observable<AssessmentResponse> {
    return EMPTY;
  }
}
