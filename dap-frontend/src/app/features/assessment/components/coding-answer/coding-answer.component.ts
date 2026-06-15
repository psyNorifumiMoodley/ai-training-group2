import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { CodingQuestionResponse } from '../../../../core/models/question.model';
import { CodingResponseRequest, TestCaseResult } from '../../../../core/models/assessment-session.model';
import { CandidateAssessmentService } from '../../../../core/services/candidate-assessment.service';
import { AnswerChangedEvent } from '../question-renderer/question-renderer.component';
import { CODING_SCAFFOLDS } from '../../../question-management/components/coding-question-preview/coding-question-preview.component';

@Component({
  selector: 'dap-coding-answer',
  standalone: true,
  imports: [],
  templateUrl: './coding-answer.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CodingAnswerComponent {
  readonly question = input.required<CodingQuestionResponse>();
  readonly savedAnswer = input<CodingResponseRequest | undefined>(undefined);
  readonly assessmentId = input.required<string>();
  readonly answerChanged = output<AnswerChangedEvent>();

  private readonly service = inject(CandidateAssessmentService);

  readonly code = signal('');
  readonly running = signal(false);
  readonly results = signal<TestCaseResult[] | null>(null);

  constructor() {
    effect(() => {
      const saved = this.savedAnswer();
      if (saved?.code) {
        this.code.set(saved.code);
      } else {
        this.code.set(CODING_SCAFFOLDS[this.question().language]);
      }
    }, { allowSignalWrites: true });
  }

  onCodeChange(value: string): void {
    this.code.set(value);
    this.answerChanged.emit({ questionId: this.question().id, request: { code: value } });
  }

  runCode(): void {
    if (this.running()) return;
    this.running.set(true);
    this.service.executeCode(this.assessmentId(), this.question().id, this.code())
      .subscribe({
        next: (response) => {
          this.results.set(response.results);
          this.running.set(false);
        },
        error: () => {
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
