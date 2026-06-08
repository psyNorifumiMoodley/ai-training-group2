import { Role } from './auth.model';

export type ThemePreference = 'LIGHT' | 'DARK';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: Role;
  themePreference: ThemePreference;
}

export interface UpdateProfileRequest {
  name: string;
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface UpdateThemeRequest {
  theme: ThemePreference;
}
