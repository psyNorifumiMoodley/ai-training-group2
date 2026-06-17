import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { CodingQuestionLanguage, CodingQuestionResponse } from '../../../../core/models/question.model';
import { CodeExecuteResponse, TestCaseResult } from '../../../../core/models/assessment-session.model';
import { QuestionService } from '../../../../core/services/question.service';

export const CODING_SCAFFOLDS: Record<CodingQuestionLanguage, string> = {
  JAVA: `public class Solution {\n    public static void main(String[] args) {\n        \n    }\n}`,
  PYTHON: `def main():\n    pass\n\nif __name__ == '__main__':\n    main()`,
  CSHARP: `using System;\n\nclass Solution {\n    static void Main(string[] args) {\n        \n    }\n}`,
};

@Component({
  selector: 'dap-coding-question-preview',
  standalone: true,
  imports: [],
  templateUrl: './coding-question-preview.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CodingQuestionPreviewComponent {
  readonly question = input.required<CodingQuestionResponse>();

  private readonly questionService = inject(QuestionService);

  readonly code = signal('');
  readonly running = signal(false);
  readonly results = signal<TestCaseResult[] | null>(null);
  readonly runError = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.code.set(CODING_SCAFFOLDS[this.question().language]);
      this.results.set(null);
    }, { allowSignalWrites: true });
  }

  onCodeChange(value: string): void {
    this.code.set(value);
  }

  runCode(): void {
    if (this.running()) return;
    this.running.set(true);
    this.runError.set(null);
    this.questionService.executeQuestion(this.question().id, this.code())
      .subscribe({
        next: (response: CodeExecuteResponse) => {
          this.results.set(response.results);
          this.running.set(false);
        },
        error: (err) => {
          this.runError.set(err?.error?.message ?? 'Code execution failed. Please try again.');
          this.running.set(false);
        },
      });
  }

  truncate(text: string | null, max = 500): string {
    if (!text) return '';
    return text.length > max ? text.slice(0, max) + '…' : text;
  }

  allPassed(): boolean {
    const r = this.results();
    return r !== null && r.length > 0 && r.every(t => t.passed);
  }

  passedCount(): number {
    return this.results()?.filter(t => t.passed).length ?? 0;
  }
}
