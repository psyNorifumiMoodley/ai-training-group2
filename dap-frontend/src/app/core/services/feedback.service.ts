import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { FeedbackItem } from '../models/marking.model';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  getMyFeedback(assessmentId: string): Observable<FeedbackItem[]> {
    void assessmentId;
    return EMPTY;
  }
}
