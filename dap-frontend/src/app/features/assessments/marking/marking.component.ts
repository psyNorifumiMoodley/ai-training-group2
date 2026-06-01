import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { AvatarComponent } from '../../../shared/components/avatar/avatar.component';
import { ProgressBarComponent } from '../../../shared/components/progress-bar/progress-bar.component';
import { ScoreInputComponent } from '../../../shared/components/score-input/score-input.component';
import { QuestionNavDotComponent } from '../../../shared/components/question-nav-dot/question-nav-dot.component';
import { TagComponent } from '../../../shared/components/tag/tag.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { Question, QuestionResponse } from '../../../core/models/assessment.model';

// TODO: replace with API call
const STUB_QUESTIONS: Question[] = [
  { id: 'q1', type: 'MCQ',  bankId: 'b1', questionText: 'What is the difference between an abstract class and an interface in Java?',        options: ['Abstract classes can have state; interfaces cannot', 'Interfaces can have constructors', 'Both are identical', 'None of the above'], correctOptionIndexes: [0], marks: 2, difficulty: 'MEDIUM', tags: ['oop'] },
  { id: 'q2', type: 'MCQ',  bankId: 'b1', questionText: 'Which collection type maintains insertion order and allows duplicates?',              options: ['HashSet', 'TreeSet', 'ArrayList', 'HashMap'], correctOptionIndexes: [2], marks: 1, difficulty: 'EASY', tags: ['collections'] },
  { id: 'q3', type: 'TEXT', bankId: 'b1', questionText: 'Explain how the Java garbage collector works and describe the generational GC model.', marks: 5, difficulty: 'HARD', tags: ['gc'] },
];

const STUB_RESPONSES: QuestionResponse[] = [
  { questionId: 'q1', type: 'MCQ',  selectedOptionIndexes: [0], autoMarked: true,  autoScore: 2 },
  { questionId: 'q2', type: 'MCQ',  selectedOptionIndexes: [1], autoMarked: true,  autoScore: 0 },
  { questionId: 'q3', type: 'TEXT', textAnswer: 'Java uses generational garbage collection, dividing the heap into Young Generation (Eden + Survivor spaces) and Old Generation. Most objects are short-lived and collected in Young GC (Minor GC). Surviving objects are promoted to Old Generation, where Major GC runs less frequently.', autoMarked: false },
];

@Component({
  selector: 'dap-marking',
  standalone: true,
  imports: [AvatarComponent, ProgressBarComponent, ScoreInputComponent, QuestionNavDotComponent, TagComponent, ButtonComponent],
  templateUrl: './marking.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkingComponent {
  readonly candidateName = 'Sara Patel';
  readonly assessmentMeta = 'Java Core · 3 questions · submitted 2026-05-26';

  readonly questions  = STUB_QUESTIONS;
  readonly responses  = STUB_RESPONSES;
  readonly activeIndex = signal(2);

  navState(index: number): 'done' | 'active' | 'todo' {
    if (index < this.activeIndex()) return 'done';
    if (index === this.activeIndex()) return 'active';
    return 'todo';
  }

  responseFor(questionId: string): QuestionResponse | undefined {
    return this.responses.find(r => r.questionId === questionId);
  }

  isCorrect(q: Question, r: QuestionResponse): boolean {
    if (q.type !== 'MCQ' || !r.selectedOptionIndexes || !q.correctOptionIndexes) return false;
    return JSON.stringify([...r.selectedOptionIndexes].sort()) === JSON.stringify([...q.correctOptionIndexes].sort());
  }

  markedCount(): number {
    return this.responses.filter(r => r.autoMarked || r.markerScore !== undefined).length;
  }

  mcqTotal(): number {
    return this.responses.filter(r => r.autoMarked).reduce((s, r) => s + (r.autoScore ?? 0), 0);
  }
}
