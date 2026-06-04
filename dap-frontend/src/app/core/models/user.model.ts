export interface CandidateRequest {
  name: string;
  email: string;
}

export interface CandidateResponse {
  id: string;
  name: string;
  email: string;
  createdAt: string;
}

export interface MarkerRequest {
  name: string;
  email: string;
  password: string;
}

export interface MarkerResponse {
  id: string;
  name: string;
  email: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
