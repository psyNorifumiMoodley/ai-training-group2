import { ChangeDetectionStrategy, Component, input } from '@angular/core';

const VARIANT_CLASSES: Record<string, string> = {
  default: 'bg-gray-100 text-gray-500',
  mcq:     'bg-green-100 text-green-700',
  text:    'bg-primary-fill text-primary-text',
  doc:     'bg-amber-100 text-amber-800',
  info:    'bg-blue-100 text-blue-700',
  coding:  'bg-purple-100 text-purple-700',
};

@Component({
  selector: 'dap-tag',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="'inline-block rounded-badge px-1.5 py-0.5 text-[0.6875rem] font-medium ' + variantClass()">
      {{ label() }}
    </span>
  `,
})
export class TagComponent {
  readonly label   = input.required<string>();
  readonly variant = input<'default' | 'mcq' | 'text' | 'doc' | 'info' | 'coding'>('default');

  variantClass(): string { return VARIANT_CLASSES[this.variant()]; }
}
