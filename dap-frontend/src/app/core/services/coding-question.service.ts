import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TestCase, TestCaseRequest } from '../models/coding-question.model';

@Injectable({ providedIn: 'root' })
export class CodingQuestionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/coding-questions`;

  addTestCase(questionId: string, request: TestCaseRequest): Observable<TestCase> {
    return EMPTY;
  }

  getTestCases(questionId: string): Observable<TestCase[]> {
    return EMPTY;
  }

  updateTestCase(questionId: string, testCaseId: string, request: TestCaseRequest): Observable<TestCase> {
    return EMPTY;
  }

  deleteTestCase(questionId: string, testCaseId: string): Observable<void> {
    return EMPTY;
  }
}
