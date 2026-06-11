import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { TestCase, TestCaseRequest } from '../../../../core/models/question.model';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

const DEFAULT_TIMEOUT_SECONDS = 10;
const DEFAULT_MEMORY_MB = 256;

@Component({
  selector: 'dap-test-case-builder',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './test-case-builder.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TestCaseBuilderComponent implements OnInit {
  readonly initialTestCases = input<TestCase[]>([]);
  readonly testCasesChange = output<TestCaseRequest[]>();

  readonly rows = signal<TestCaseRequest[]>([]);
  readonly touched = signal(false);

  ngOnInit(): void {
    const init = this.initialTestCases();
    if (init.length > 0) {
      this.rows.set(init.map(tc => ({
        input: tc.input,
        expectedOutput: tc.expectedOutput,
        timeoutSeconds: tc.timeoutSeconds,
        memoryMb: tc.memoryMb,
      })));
    }
  }

  get isValid(): boolean {
    return this.rows().every(row => this.isRowValid(row));
  }

  isRowValid(row: TestCaseRequest): boolean {
    return row.expectedOutput.trim().length > 0
      && row.timeoutSeconds >= 1 && row.timeoutSeconds <= 60
      && row.memoryMb >= 64 && row.memoryMb <= 1024;
  }

  markTouched(): void {
    this.touched.set(true);
  }

  addRow(): void {
    this.rows.update(rows => [...rows, {
      input: '',
      expectedOutput: '',
      timeoutSeconds: DEFAULT_TIMEOUT_SECONDS,
      memoryMb: DEFAULT_MEMORY_MB,
    }]);
    this.emit();
  }

  removeRow(index: number): void {
    this.rows.update(rows => rows.filter((_, i) => i !== index));
    this.emit();
  }

  updateRow(index: number, partial: Partial<TestCaseRequest>): void {
    this.rows.update(rows => rows.map((row, i) => i === index ? { ...row, ...partial } : row));
    this.emit();
  }

  private emit(): void {
    this.testCasesChange.emit(this.rows());
  }
}
