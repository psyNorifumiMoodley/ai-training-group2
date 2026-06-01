import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core';

export interface McqBuilderValue {
  options: string[];
  correctAnswers: string[];
  isValid: boolean;
}

interface Option {
  text: string;
  correct: boolean;
}

@Component({
  selector: 'dap-mcq-option-builder',
  standalone: true,
  templateUrl: './mcq-option-builder.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class McqOptionBuilderComponent {
  readonly valueChange = output<McqBuilderValue>();

  readonly options = signal<Option[]>([
    { text: '', correct: false },
    { text: '', correct: false },
  ]);

  get isValid(): boolean {
    const filled = this.options().filter(o => o.text.trim());
    return filled.length >= 1 && filled.some(o => o.correct);
  }

  get hasAnyText(): boolean {
    return this.options().some(o => o.text.trim().length > 0);
  }

  getValue(): McqBuilderValue {
    const filled = this.options().filter(o => o.text.trim());
    return {
      options: filled.map(o => o.text),
      correctAnswers: filled.filter(o => o.correct).map(o => o.text),
      isValid: filled.length >= 1 && filled.some(o => o.correct),
    };
  }

  addOption(): void {
    this.options.update(opts => [...opts, { text: '', correct: false }]);
  }

  removeOption(index: number): void {
    if (this.options().length <= 2) return;
    this.options.update(opts => opts.filter((_, i) => i !== index));
    this.emit();
  }

  updateOptionText(index: number, text: string): void {
    this.options.update(opts =>
      opts.map((opt, i) => i === index ? { ...opt, text } : opt)
    );
    this.emit();
  }

  toggleCorrect(index: number): void {
    this.options.update(opts =>
      opts.map((opt, i) => i === index ? { ...opt, correct: !opt.correct } : opt)
    );
    this.emit();
  }

  reset(): void {
    this.options.set([
      { text: '', correct: false },
      { text: '', correct: false },
    ]);
  }

  private emit(): void {
    this.valueChange.emit(this.getValue());
  }
}
