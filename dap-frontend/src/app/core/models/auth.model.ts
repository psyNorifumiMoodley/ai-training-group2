export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: Role;
}

export type Role = 'ADMIN' | 'MARKER' | 'CANDIDATE';

export interface CurrentUser {
  id: string;
  email: string;
  role: Role;
}
