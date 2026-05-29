import { ChangeDetectionStrategy, Component, DestroyRef, inject, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '../../../../core/services/user.service';
import { MarkerRequest } from '../../../../core/models/user.model';

@Component({
  selector: 'app-marker-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './marker-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly submitting = signal(false);
  readonly conflictError = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  get nameControl() { return this.form.controls.name; }
  get emailControl() { return this.form.controls.email; }
  get passwordControl() { return this.form.controls.password; }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.conflictError.set(false);
    this.submitting.set(true);

    const request: MarkerRequest = this.form.getRawValue();
    this.userService.registerMarker(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.saved.emit();
        },
        error: (err: HttpErrorResponse) => {
          this.submitting.set(false);
          if (err.status === 409) {
            this.conflictError.set(true);
          }
        }
      });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
