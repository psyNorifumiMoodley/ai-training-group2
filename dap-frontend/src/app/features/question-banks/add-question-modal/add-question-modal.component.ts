import { ChangeDetectionStrategy, Component, input, output, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, FormArray } from '@angular/forms';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { QuestionBank, NewQuestionRequest, QuestionType } from '../../../core/models/assessment.model';

// TODO: replace with API call
const STUB_BANKS: QuestionBank[] = [
  { id: 'b1', name: 'Java Core',   description: '', questionCount: 24 },
  { id: 'b2', name: 'Angular',     description: '', questionCount: 18 },
  { id: 'b3', name: 'Spring Boot', description: '', questionCount: 31 },
  { id: 'b4', name: 'SQL Basics',  description: '', questionCount: 15 },
];

@Component({
  selector: 'dap-add-question-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent],
  templateUrl: './add-question-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddQuestionModalComponent {
  private readonly fb = inject(FormBuilder);

  readonly bankId    = input<string | undefined>(undefined);
  readonly submitted = output<NewQuestionRequest>();
  readonly cancelled = output<void>();

  readonly banks = STUB_BANKS;

  readonly form = this.fb.nonNullable.group({
    type:         ['' as QuestionType,      Validators.required],
    bankId:       [this.bankId() ?? '',     Validators.required],
    questionText: ['',                       [Validators.required, Validators.minLength(10)]],
    options:      this.fb.array([this.fb.control(''), this.fb.control('')]),
    correctOptions: [[] as number[]],
    marks:        [1,  [Validators.required, Validators.min(1)]],
    difficulty:   ['' as 'EASY' | 'MEDIUM' | 'HARD', Validators.required],
    tags:         [''],
  });

  get optionsArray(): FormArray { return this.form.controls.options as FormArray; }
  get isMcq(): boolean { return this.form.controls.type.value === 'MCQ'; }

  addOption(): void {
    this.optionsArray.push(this.fb.control(''));
  }

  cancel(): void {
    this.cancelled.emit();
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // TODO: replace with API call
    this.submitted.emit({
      type:                 v.type,
      bankId:               v.bankId,
      questionText:         v.questionText,
      options:              this.isMcq ? v.options.filter((o): o is string => !!o) : undefined,
      correctOptionIndexes: this.isMcq ? v.correctOptions : undefined,
      marks:                v.marks,
      difficulty:           v.difficulty,
      tags:                 v.tags.split(',').map(t => t.trim()).filter(Boolean),
    });
  }
}
