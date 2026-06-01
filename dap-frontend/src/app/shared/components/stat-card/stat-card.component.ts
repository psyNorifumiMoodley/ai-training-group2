import { ChangeDetectionStrategy, Component, input } from '@angular/core';

const SUB_COLOR_CLASSES = {
  primary: 'text-primary',
  success: 'text-green-600',
  warning: 'text-amber-600',
  danger:  'text-red-600',
};

@Component({
  selector: 'dap-stat-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bg-white border border-gray-200 rounded-card p-3.5 relative overflow-hidden">
      @if (accentColor()) {
        <div class="absolute top-0 left-0 right-0 h-[0.1875rem]" [style.background-color]="accentColor()"></div>
      }
      <p class="text-[0.6875rem] font-medium text-gray-400 uppercase tracking-[0.07em] mt-1">{{ label() }}</p>
      <p class="text-[1.75rem] font-medium text-gray-900 leading-tight mt-1">{{ value() }}</p>
      @if (sub()) {
        <p [class]="'text-[0.6875rem] mt-1 ' + subColorClass()">{{ sub() }}</p>
      }
    </div>
  `,
})
export class StatCardComponent {
  readonly label       = input.required<string>();
  readonly value       = input.required<string | number>();
  readonly sub         = input<string | undefined>(undefined);
  readonly subColor    = input<'primary' | 'success' | 'warning' | 'danger'>('primary');
  readonly accentColor = input<string | undefined>(undefined);

  subColorClass(): string { return SUB_COLOR_CLASSES[this.subColor()]; }
}
