import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { TagComponent } from '../../../shared/components/tag/tag.component';
import { AddQuestionModalComponent } from '../add-question-modal/add-question-modal.component';
import { QuestionBank, Question, QuestionType } from '../../../core/models/assessment.model';

// TODO: replace with API call
const STUB_BANKS: QuestionBank[] = [
  { id: 'b1', name: 'Java Core',   description: 'OOP, collections, concurrency', questionCount: 24 },
  { id: 'b2', name: 'Angular',     description: 'Components, RxJS, signals',     questionCount: 18 },
  { id: 'b3', name: 'Spring Boot', description: 'REST, JPA, security',           questionCount: 31 },
  { id: 'b4', name: 'SQL Basics',  description: 'Queries, joins, aggregates',    questionCount: 15 },
];

// TODO: replace with API call
const STUB_QUESTIONS: Question[] = [
  { id: 'q1', type: 'MCQ',  bankId: 'b1', questionText: 'What is the difference between an abstract class and an interface in Java?', marks: 2, difficulty: 'MEDIUM', tags: ['oop', 'interfaces'] },
  { id: 'q2', type: 'MCQ',  bankId: 'b1', questionText: 'Which collection type maintains insertion order and allows duplicates?', marks: 1, difficulty: 'EASY',   tags: ['collections'] },
  { id: 'q3', type: 'TEXT', bankId: 'b1', questionText: 'Explain how the Java garbage collector works and describe the generational GC model.', marks: 5, difficulty: 'HARD', tags: ['gc', 'memory'] },
  { id: 'q4', type: 'MCQ',  bankId: 'b1', questionText: 'What does the volatile keyword guarantee in a multithreaded context?', marks: 2, difficulty: 'MEDIUM', tags: ['concurrency'] },
];

@Component({
  selector: 'dap-bank-list',
  standalone: true,
  imports: [ButtonComponent, TagComponent, AddQuestionModalComponent],
  templateUrl: './bank-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankListComponent {
  readonly banks     = STUB_BANKS;
  readonly questions = STUB_QUESTIONS;

  readonly activeBank    = signal<QuestionBank>(STUB_BANKS[0]);
  readonly showModal     = signal(false);
  readonly searchQuery   = signal('');
  readonly activeFilter  = signal<QuestionType | 'ALL'>('ALL');

  readonly filterTypes: Array<QuestionType | 'ALL'> = ['ALL', 'MCQ', 'TEXT', 'DOC'];

  filteredQuestions(): Question[] {
    return this.questions.filter(q => {
      const matchBank   = q.bankId === this.activeBank().id;
      const matchFilter = this.activeFilter() === 'ALL' || q.type === this.activeFilter();
      const matchSearch = !this.searchQuery() || q.questionText.toLowerCase().includes(this.searchQuery().toLowerCase());
      return matchBank && matchFilter && matchSearch;
    });
  }

  typeVariant(type: QuestionType): 'mcq' | 'text' | 'doc' {
    const map: Record<QuestionType, 'mcq' | 'text' | 'doc'> = { MCQ: 'mcq', TEXT: 'text', DOC: 'doc' };
    return map[type];
  }
}
