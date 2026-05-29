import { Injectable } from '@angular/core';
import { Observable, EMPTY } from 'rxjs';
import {
  DocQuestionRequest,
  GroupQuestionRequest,
  McqQuestionRequest,
  QuestionResponse,
  TextQuestionRequest,
} from '../models/question.model';
import { PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  createQuestion(
    request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest
  ): Observable<QuestionResponse> {
    return EMPTY;
  }

  getQuestions(page: number, size: number, category?: string): Observable<PageResponse<QuestionResponse>> {
    return EMPTY;
  }

  getCategories(): Observable<string[]> {
    return EMPTY;
  }
}
