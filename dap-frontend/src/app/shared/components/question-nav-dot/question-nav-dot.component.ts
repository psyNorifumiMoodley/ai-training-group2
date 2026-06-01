import { ChangeDetectionStrategy, Component, input } from '@angular/core';

const STATE_CLASSES: Record<string, string> = {
  done:   'bg-primary/20 text-[#93c5fd]',
  active: 'bg-primary text-white',
  todo:   'bg-white/5 text-[#8b949e]',
};

@Component({
  selector: 'dap-question-nav-dot',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [class]="'w-7 h-7 rounded-md flex items-center justify-center text-xs font-medium cursor-pointer select-none ' + stateClass()">
      {{ number() }}
    </div>
  `,
})
export class QuestionNavDotComponent {
  readonly number = input.required<number>();
  readonly state  = input<'done' | 'active' | 'todo'>('todo');

  stateClass(): string { return STATE_CLASSES[this.state()]; }
}
