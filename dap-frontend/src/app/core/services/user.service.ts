import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CandidateRequest, CandidateResponse, MarkerRequest, MarkerResponse, PageResponse } from '../models/user.model';
import { AssessmentResponse } from '../models/assessment.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  registerCandidate(request: CandidateRequest): Observable<CandidateResponse> {
    return this.http.post<CandidateResponse>(`${environment.apiBaseUrl}/candidates`, request);
  }

  registerMarker(request: MarkerRequest): Observable<MarkerResponse> {
    return this.http.post<MarkerResponse>(`${environment.apiBaseUrl}/markers`, request);
  }

  getCandidates(
    page: number,
    size: number,
    search?: string,
    sortBy?: string,
    sortDir?: string,
    status?: string
  ): Observable<PageResponse<CandidateResponse>> {
    const params: Record<string, string | number> = { page, size };
    if (search) params['search'] = search;
    if (sortBy) params['sortBy'] = sortBy;
    if (sortDir) params['sortDir'] = sortDir;
    if (status) params['status'] = status;
    return this.http.get<PageResponse<CandidateResponse>>(`${environment.apiBaseUrl}/candidates`, { params });
  }

  getCandidateById(id: string): Observable<CandidateResponse> {
    return this.http.get<CandidateResponse>(`${environment.apiBaseUrl}/candidates/${id}`);
  }

  updateCandidate(id: string, request: CandidateRequest): Observable<CandidateResponse> {
    return this.http.put<CandidateResponse>(`${environment.apiBaseUrl}/candidates/${id}`, request);
  }

  deleteCandidate(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/candidates/${id}`);
  }

  getCandidateAssessments(id: string): Observable<AssessmentResponse[]> {
    return this.http.get<AssessmentResponse[]>(`${environment.apiBaseUrl}/candidates/${id}/assessments`);
  }

  getMarkers(page: number, size: number): Observable<PageResponse<MarkerResponse>> {
    return this.http.get<PageResponse<MarkerResponse>>(`${environment.apiBaseUrl}/markers`, {
      params: { page, size }
    });
  }
}
