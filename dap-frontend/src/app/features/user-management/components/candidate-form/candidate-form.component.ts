import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '../../../../core/services/user.service';
import { CandidateRequest, CandidateResponse } from '../../../../core/models/user.model';

@Component({
  selector: 'app-candidate-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './candidate-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  readonly editCandidate = input<CandidateResponse | null>(null);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly submitting = signal(false);
  readonly conflictError = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  get nameControl() { return this.form.controls.name; }
  get emailControl() { return this.form.controls.email; }

  ngOnInit(): void {
    const existing = this.editCandidate();
    if (existing) {
      this.form.patchValue({ name: existing.name, email: existing.email });
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.conflictError.set(false);
    this.submitting.set(true);

    const request: CandidateRequest = this.form.getRawValue();
    const editId = this.editCandidate()?.id;
    const obs = editId
      ? this.userService.updateCandidate(editId, request)
      : this.userService.registerCandidate(request);

    obs.pipe(takeUntilDestroyed(this.destroyRef))
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
