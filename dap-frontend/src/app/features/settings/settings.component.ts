import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { UserSettingsService } from '../../core/services/user-settings.service';
import { ThemeService } from '../../core/services/theme.service';
import { ThemePreference } from '../../core/models/user-settings.model';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const newPassword = control.get('newPassword')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  if (newPassword && confirmPassword && newPassword !== confirmPassword) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
  selector: 'dap-settings',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './settings.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userSettingsService = inject(UserSettingsService);
  readonly themeService = inject(ThemeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly profileSuccess = signal(false);
  readonly profileError = signal<string | null>(null);
  readonly profileSubmitting = signal(false);

  readonly passwordSuccess = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly passwordSubmitting = signal(false);

  readonly themeSuccess = signal(false);
  readonly themeError = signal<string | null>(null);

  readonly profileForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]],
  }, { validators: passwordMatchValidator });

  get nameControl() { return this.profileForm.controls.name; }
  get emailControl() { return this.profileForm.controls.email; }
  get currentPasswordControl() { return this.passwordForm.controls.currentPassword; }
  get newPasswordControl() { return this.passwordForm.controls.newPassword; }
  get confirmPasswordControl() { return this.passwordForm.controls.confirmPassword; }
  get isDark(): boolean { return this.themeService.currentTheme() === 'DARK'; }

  ngOnInit(): void {
    this.userSettingsService.getProfile()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: profile => {
          this.profileForm.patchValue({
            name: profile.name,
            email: profile.email,
          });
          this.themeService.applyTheme(profile.themePreference);
        },
      });
  }

  submitProfile(): void {
    if (this.profileForm.invalid || this.profileSubmitting()) return;
    this.profileSuccess.set(false);
    this.profileError.set(null);
    this.profileSubmitting.set(true);

    this.userSettingsService.updateProfile(this.profileForm.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.profileSubmitting.set(false);
          this.profileSuccess.set(true);
        },
        error: (err: HttpErrorResponse) => {
          this.profileSubmitting.set(false);
          if (err.status === 409) {
            this.profileError.set('This email address is already in use.');
          } else {
            this.profileError.set('Failed to update profile. Please try again.');
          }
        },
      });
  }

  submitPassword(): void {
    if (this.passwordForm.invalid || this.passwordSubmitting()) return;
    this.passwordSuccess.set(false);
    this.passwordError.set(null);
    this.passwordSubmitting.set(true);

    this.userSettingsService.changePassword(this.passwordForm.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.passwordSubmitting.set(false);
          this.passwordSuccess.set(true);
          this.passwordForm.reset();
        },
        error: (err: HttpErrorResponse) => {
          this.passwordSubmitting.set(false);
          if (err.status === 400) {
            this.passwordError.set(err.error?.message ?? 'Current password is incorrect.');
          } else {
            this.passwordError.set('Failed to change password. Please try again.');
          }
        },
      });
  }

  toggleTheme(): void {
    const newTheme: ThemePreference = this.isDark ? 'LIGHT' : 'DARK';
    this.themeService.applyTheme(newTheme);
    this.themeSuccess.set(false);
    this.themeError.set(null);

    this.userSettingsService.updateTheme({ theme: newTheme })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.themeSuccess.set(true),
        error: () => this.themeError.set('Failed to save theme preference.'),
      });
  }
}
