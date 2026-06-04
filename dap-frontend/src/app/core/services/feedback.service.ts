import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FeedbackItem } from '../models/marking.model';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  constructor(private http: HttpClient) {}

  getMyFeedback(assessmentId: string): Observable<FeedbackItem[]> {
    return this.http.get<FeedbackItem[]>(`/api/assessments/${assessmentId}/feedback`);
  }
}
