import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CodingQuestionRequest,
  DocQuestionRequest,
  GroupQuestionRequest,
  McqQuestionRequest,
  QuestionResponse,
  TextQuestionRequest,
} from '../models/question.model';
import { PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly http = inject(HttpClient);

  createQuestion(
    request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest | CodingQuestionRequest
  ): Observable<QuestionResponse> {
    return this.http.post<QuestionResponse>(`${environment.apiBaseUrl}/questions`, request);
  }

  getQuestions(page: number, size: number, category?: string): Observable<PageResponse<QuestionResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (category) params = params.set('category', category);
    return this.http.get<PageResponse<QuestionResponse>>(`${environment.apiBaseUrl}/questions`, { params });
  }

  updateQuestion(
    id: string,
    request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest | CodingQuestionRequest
  ): Observable<QuestionResponse> {
    return this.http.put<QuestionResponse>(`${environment.apiBaseUrl}/questions/${id}`, request);
  }

  deleteQuestion(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/questions/${id}`);
  }

  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiBaseUrl}/questions/categories`);
  }
}
