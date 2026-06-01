import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { InitialsPipe } from '../../pipes/initials.pipe';

const SIZE_CLASSES = {
  sm: 'w-[1.375rem] h-[1.375rem] text-[0.625rem]',
  md: 'w-[1.625rem] h-[1.625rem] text-[0.6875rem]',
  lg: 'w-8 h-8 text-caption',
};

@Component({
  selector: 'dap-avatar',
  standalone: true,
  imports: [InitialsPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [class]="containerClass()">{{ name() | initials }}</div>
  `,
})
export class AvatarComponent {
  readonly name        = input.required<string>();
  readonly size        = input<'sm' | 'md' | 'lg'>('md');
  readonly interactive = input(false);

  containerClass(): string {
    const ring = this.interactive() ? ' ring-2 ring-primary' : '';
    return `${SIZE_CLASSES[this.size()]} rounded-avatar bg-primary-fill text-primary-text flex items-center justify-center font-medium flex-shrink-0${ring}`;
  }
}
