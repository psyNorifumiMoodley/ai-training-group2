import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core';

@Component({
  selector: 'dap-keyword-list',
  standalone: true,
  templateUrl: './keyword-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KeywordListComponent {
  readonly keywordsChange = output<string[]>();

  readonly keywords = signal<string[]>([]);
  readonly inputValue = signal('');

  addKeyword(): void {
    const val = this.inputValue().trim();
    if (val && !this.keywords().includes(val)) {
      this.keywords.update(kws => [...kws, val]);
      this.keywordsChange.emit(this.keywords());
    }
    this.inputValue.set('');
  }

  removeKeyword(kw: string): void {
    this.keywords.update(kws => kws.filter(k => k !== kw));
    this.keywordsChange.emit(this.keywords());
  }

  onInputKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addKeyword();
    }
  }

  onInputChange(value: string): void {
    this.inputValue.set(value);
  }

  reset(): void {
    this.keywords.set([]);
    this.inputValue.set('');
  }
}
