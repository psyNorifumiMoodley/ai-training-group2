import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { CurrentUser, LoginRequest, LoginResponse, Role } from '../models/auth.model';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'dap_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly _currentUser = signal<CurrentUser | null>(this.decodeStoredToken());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = signal(this._currentUser() !== null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request).pipe(
      tap(response => {
        localStorage.setItem(TOKEN_KEY, response.token);
        this._currentUser.set(this.decodeToken(response.token));
        this.isLoggedIn.set(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._currentUser.set(null);
    this.isLoggedIn.set(false);
  }

  hasRole(role: Role): boolean {
    return this._currentUser()?.role === role;
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  storeToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }

  private decodeStoredToken(): CurrentUser | null {
    const token = localStorage.getItem(TOKEN_KEY);
    return token ? this.decodeToken(token) : null;
  }

  private decodeToken(token: string): CurrentUser | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const now = Math.floor(Date.now() / 1000);
      if (payload.exp && payload.exp < now) {
        return null;
      }
      return { id: payload.sub, email: payload.email ?? '', role: payload.role as Role };
    } catch {
      return null;
    }
  }
}
