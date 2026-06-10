import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { environment } from '../../../environments/environment';
import { QuestionBankResponse } from '../models/question.model';

@Injectable({ providedIn: 'root' })
export class QuestionBankService {
  private readonly http = inject(HttpClient);

  getQuestionBanks(): Observable<QuestionBankResponse[]> {
    return this.http.get<QuestionBankResponse[]>(`${environment.apiBaseUrl}/question-banks`);
  }

  createQuestionBank(name: string): Observable<QuestionBankResponse> {
    return EMPTY;
  }

  renameQuestionBank(id: string, name: string): Observable<QuestionBankResponse> {
    return EMPTY;
  }

  deleteQuestionBank(id: string): Observable<void> {
    return EMPTY;
  }
}
