import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'dap-score-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-center gap-2">
      <input
        type="number"
        [value]="value()"
        [readonly]="readonly()"
        [min]="0"
        [max]="max()"
        class="w-[3.25rem] text-center border rounded-input text-caption px-1 py-1 focus:outline-none focus:ring-2 focus:ring-primary/30"
        [class.bg-gray-50]="readonly()"
        [class.cursor-not-allowed]="readonly()"
        (blur)="onCommit($event)"
        (keydown.enter)="onCommit($event)">
      <span class="text-caption text-gray-400">/ {{ max() }}</span>
    </div>
  `,
})
export class ScoreInputComponent {
  readonly max      = input.required<number>();
  readonly value    = input<number>(0);
  readonly readonly = input(false);

  readonly scoreChanged = output<number>();

  onCommit(event: Event): void {
    if (this.readonly()) return;
    const raw = +(event.target as HTMLInputElement).value;
    const clamped = Math.max(0, Math.min(this.max(), isNaN(raw) ? 0 : raw));
    (event.target as HTMLInputElement).value = String(clamped);
    this.scoreChanged.emit(clamped);
  }
}
