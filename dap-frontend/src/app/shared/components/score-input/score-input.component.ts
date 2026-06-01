import { ChangeDetectionStrategy, Component, input } from '@angular/core';

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
        [class.cursor-not-allowed]="readonly()">
      <span class="text-caption text-gray-400">/ {{ max() }}</span>
    </div>
  `,
})
export class ScoreInputComponent {
  readonly max      = input.required<number>();
  readonly value    = input<number>(0);
  readonly readonly = input(false);
}
