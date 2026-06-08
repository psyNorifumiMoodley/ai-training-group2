import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ChangePasswordRequest,
  UpdateProfileRequest,
  UpdateThemeRequest,
  UserProfile,
} from '../models/user-settings.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/users/me`;

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.baseUrl);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.baseUrl}/profile`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/password`, request);
  }

  updateTheme(request: UpdateThemeRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.baseUrl}/theme`, request);
  }
}
