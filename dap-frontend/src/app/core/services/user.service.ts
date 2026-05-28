import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { CandidateRequest, CandidateResponse, MarkerRequest, MarkerResponse, PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {

  registerCandidate(_request: CandidateRequest): Observable<CandidateResponse> {
    return EMPTY;
  }

  registerMarker(_request: MarkerRequest): Observable<MarkerResponse> {
    return EMPTY;
  }

  getCandidates(_page: number, _size: number): Observable<PageResponse<CandidateResponse>> {
    return EMPTY;
  }

  getMarkers(_page: number, _size: number): Observable<PageResponse<MarkerResponse>> {
    return EMPTY;
  }
}
