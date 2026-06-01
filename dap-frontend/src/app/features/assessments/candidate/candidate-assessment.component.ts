import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { QuestionNavDotComponent } from '../../../shared/components/question-nav-dot/question-nav-dot.component';
import { ProgressBarComponent } from '../../../shared/components/progress-bar/progress-bar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { Question } from '../../../core/models/assessment.model';

// TODO: replace with API call
const STUB_QUESTIONS: Question[] = [
  { id: 'q1', type: 'MCQ',  bankId: 'b1', questionText: 'Which of the following is true about Java interfaces?', options: ['They can have constructors', 'They can have instance variables', 'They support multiple inheritance', 'They cannot have default methods'], correctOptionIndexes: [2], marks: 2, difficulty: 'MEDIUM', tags: [] },
  { id: 'q2', type: 'MCQ',  bankId: 'b1', questionText: 'What does the "final" keyword mean when applied to a class?', options: ['The class is abstract', 'The class cannot be instantiated', 'The class cannot be extended', 'The class is singleton'], correctOptionIndexes: [2], marks: 1, difficulty: 'EASY', tags: [] },
  { id: 'q3', type: 'TEXT', bankId: 'b1', questionText: 'Describe the SOLID principles and give a brief example of each.', marks: 10, difficulty: 'HARD', tags: [] },
  { id: 'q4', type: 'MCQ',  bankId: 'b1', questionText: 'Which data structure uses LIFO ordering?', options: ['Queue', 'Stack', 'Deque', 'Priority Queue'], correctOptionIndexes: [1], marks: 1, difficulty: 'EASY', tags: [] },
  { id: 'q5', type: 'DOC',  bankId: 'b1', questionText: 'Upload a UML class diagram for a simple bank account system.', marks: 8, difficulty: 'HARD', tags: [] },
];

@Component({
  selector: 'dap-candidate-assessment',
  standalone: true,
  imports: [QuestionNavDotComponent, ProgressBarComponent, ButtonComponent],
  templateUrl: './candidate-assessment.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateAssessmentComponent {
  readonly assessmentTitle = 'Java Core — Backend Engineer';
  readonly questions       = STUB_QUESTIONS;
  readonly currentIndex    = signal(0);
  readonly answers         = signal<Record<string, unknown>>({});

  readonly currentQuestion = computed(() => this.questions[this.currentIndex()]);
  readonly isLast          = computed(() => this.currentIndex() === this.questions.length - 1);
  readonly answeredCount   = computed(() => Object.keys(this.answers()).length);

  readonly selectedOption  = signal<number | null>(null);

  navState(index: number): 'done' | 'active' | 'todo' {
    if (this.answers()[this.questions[index].id] !== undefined) return 'done';
    if (index === this.currentIndex()) return 'active';
    return 'todo';
  }

  selectOption(index: number): void {
    this.selectedOption.set(index);
  }

  prev(): void {
    if (this.currentIndex() > 0) this.currentIndex.update(i => i - 1);
  }

  next(): void {
    if (this.selectedOption() !== null) {
      this.answers.update(a => ({ ...a, [this.currentQuestion().id]: this.selectedOption() }));
    }
    if (!this.isLast()) {
      this.currentIndex.update(i => i + 1);
      this.selectedOption.set(null);
    }
  }
}
