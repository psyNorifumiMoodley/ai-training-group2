import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CandidateRequest, CandidateResponse, MarkerRequest, MarkerResponse, PageResponse } from '../models/user.model';
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

  getCandidates(page: number, size: number): Observable<PageResponse<CandidateResponse>> {
    return this.http.get<PageResponse<CandidateResponse>>(`${environment.apiBaseUrl}/candidates`, {
      params: { page, size }
    });
  }

  getMarkers(page: number, size: number): Observable<PageResponse<MarkerResponse>> {
    return this.http.get<PageResponse<MarkerResponse>>(`${environment.apiBaseUrl}/markers`, {
      params: { page, size }
    });
  }
}
