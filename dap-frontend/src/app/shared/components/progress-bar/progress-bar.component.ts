import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'dap-progress-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      [class]="'w-full bg-gray-200 rounded-full overflow-hidden ' + (height() === 'xs' ? 'h-0.5' : 'h-[0.3125rem]')"
      [attr.aria-label]="label()"
      role="progressbar"
      [attr.aria-valuenow]="value()"
      aria-valuemin="0"
      aria-valuemax="100">
      <div
        [class]="color() + ' h-full rounded-full transition-all'"
        [style.width.%]="value()">
      </div>
    </div>
  `,
})
export class ProgressBarComponent {
  readonly value  = input.required<number>();
  readonly color  = input('bg-primary');
  readonly height = input<'xs' | 'sm'>('sm');
  readonly label  = input<string | undefined>(undefined);
}
