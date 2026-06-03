import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { DestroyRef } from '@angular/core';

@Component({
  selector: 'dap-countdown-timer',
  standalone: true,
  templateUrl: './countdown-timer.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountdownTimerComponent implements OnInit {
  readonly initialSeconds = input.required<number>();
  readonly expired = output<void>();

  private readonly destroyRef = inject(DestroyRef);
  readonly remaining = signal(0);

  readonly display = computed(() => {
    const s = this.remaining();
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  });

  readonly isWarning = computed(() => this.remaining() < 300);

  ngOnInit(): void {
    this.remaining.set(this.initialSeconds());
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const next = this.remaining() - 1;
        if (next <= 0) {
          this.remaining.set(0);
          this.expired.emit();
        } else {
          this.remaining.set(next);
        }
      });
  }
}
