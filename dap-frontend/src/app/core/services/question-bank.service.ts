import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { QuestionBankResponse } from '../models/question.model';

@Injectable({ providedIn: 'root' })
export class QuestionBankService {
  private readonly http = inject(HttpClient);

  getQuestionBanks(): Observable<QuestionBankResponse[]> {
    return this.http.get<QuestionBankResponse[]>(`${environment.apiBaseUrl}/question-banks`);
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
