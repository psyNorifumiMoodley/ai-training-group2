import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly loginError = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  get emailControl() { return this.form.controls.email; }
  get passwordControl() { return this.form.controls.password; }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.loginError.set(false);
    this.submitting.set(true);

    this.authService.login(this.form.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          const role = this.authService.currentUser()?.role;
          if (role === 'ADMIN' || role === 'MARKER') {
            this.router.navigateByUrl('/admin/users');
          }
        },
        error: (err: HttpErrorResponse) => {
          this.submitting.set(false);
          if (err.status === 401) {
            this.loginError.set(true);
          }
        },
      });
  }
}
