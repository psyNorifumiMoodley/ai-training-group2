import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'dap-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled() || loading()"
      [class]="buttonClass()"
      (click)="handleClick()">
      @if (loading()) {
        <i class="pi pi-spin pi-spinner text-caption"></i>
      } @else if (iconLeft()) {
        <i [class]="'pi text-caption ' + iconLeft()"></i>
      }
      <ng-content />
      @if (!loading() && iconRight()) {
        <i [class]="'pi text-caption ' + iconRight()"></i>
      }
    </button>
  `,
})
export class ButtonComponent {
  readonly variant = input<'primary' | 'secondary' | 'ghost'>('primary');
  readonly size    = input<'sm' | 'md'>('md');
  readonly disabled = input(false);
  readonly loading  = input(false);
  readonly iconLeft  = input<string | undefined>(undefined);
  readonly iconRight = input<string | undefined>(undefined);
  readonly type = input<'button' | 'submit' | 'reset'>('button');

  readonly clicked = output<void>();

  handleClick(): void {
    if (!this.disabled() && !this.loading()) this.clicked.emit();
  }

  buttonClass(): string {
    const base = 'rounded-input text-caption font-medium flex items-center gap-[0.3125rem] transition-colors focus:outline-none focus:shadow-focus disabled:opacity-50 disabled:cursor-not-allowed';
    const pad  = this.size() === 'sm' ? 'px-3 py-1' : 'px-4 py-2';
    const variants: Record<string, string> = {
      primary:   'bg-primary text-white border border-transparent hover:bg-primary-hover active:bg-primary-pressed cursor-pointer',
      secondary: 'bg-transparent text-primary border-[0.09375rem] border-primary hover:bg-primary-light cursor-pointer',
      ghost:     'bg-transparent text-gray-500 border-[0.03125rem] border-gray-300 hover:bg-gray-100 cursor-pointer',
    };
    return `${base} ${pad} ${variants[this.variant()]}`;
  }
}
