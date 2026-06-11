import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { QuestionBankResponse } from '../models/question.model';
import { PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class QuestionBankService {
  private readonly http = inject(HttpClient);

  getQuestionBanks(): Observable<QuestionBankResponse[]> {
    const params = new HttpParams().set('page', 0).set('size', 100);
    return this.http.get<PageResponse<QuestionBankResponse>>(`${environment.apiBaseUrl}/question-banks`, { params })
      .pipe(map(page => page.content));
  }

  createQuestionBank(name: string): Observable<QuestionBankResponse> {
    return this.http.post<QuestionBankResponse>(`${environment.apiBaseUrl}/question-banks`, { name });
  }

  renameQuestionBank(id: string, name: string): Observable<QuestionBankResponse> {
    return this.http.put<QuestionBankResponse>(`${environment.apiBaseUrl}/question-banks/${id}`, { name });
  }

  deleteQuestionBank(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/question-banks/${id}`);
  }
}
