## 1. TestCaseBuilderComponent

- [x] 1.1 Scaffold `dap-frontend/src/app/features/question-management/components/test-case-builder/test-case-builder.component.ts` (standalone, `OnPush`, selector `dap-test-case-builder`) with `initialTestCases = input<TestCase[]>([])` and `testCasesChange = output<TestCaseRequest[]>()`
- [x] 1.2 Implement internal `signal<TestCaseRequest[]>` row list with `addRow()`, `removeRow(index)`, `updateRow(index, partial)`, each calling a private `emit()`
- [x] 1.3 In `ngOnInit()`, seed rows from `initialTestCases()` (map `TestCase` → `TestCaseRequest`, dropping `id`/`ordinal`)
- [x] 1.4 Build `test-case-builder.component.html`: one card per row with input textarea (optional), expected output textarea (required, inline error when blank and touched), timeout number input (1–60, default 10), memory MB number input (64–1024, default 256), and a "Remove" button; "Add row" button at the bottom (mirror `group-children-builder` styling)
- [x] 1.5 Add per-row inline validation messages for missing expected output and out-of-range timeout/memory

## 2. QuestionFormComponent — type selector & language

- [x] 2.1 Update `questionTypes` array: remove `'DOC'`, add `'CODING'`
- [x] 2.2 Add `language` control to `form` (`FormBuilder.nonNullable.group`) with `Validators.required`, typed as `CodingQuestionLanguage | ''`
- [x] 2.3 Add `readonly codingLanguages: { value: CodingQuestionLanguage; label: string }[]` constant (`JAVA`/`Java`, `PYTHON`/`Python`, `CSHARP`/`C#`)
- [x] 2.4 Add language dropdown to `question-form.component.html`, shown when `selectedType() === 'CODING'`, bound to `formControlName="language"`, with inline required-error message

## 3. QuestionFormComponent — test case integration

- [x] 3.1 Import `TestCaseBuilderComponent` into `QuestionFormComponent`'s `imports` array
- [x] 3.2 Add `codingInitialTestCases` computed signal (mirrors `groupInitialChildren`): returns `existingQuestion().testCases` when type is `CODING`, else `[]`
- [x] 3.3 Add `currentTestCases` field + `onTestCasesChange(testCases: TestCaseRequest[])` handler, following the `currentMcqValue`/`onMcqValueChange` pattern
- [x] 3.4 Render `<dap-test-case-builder>` in the template when `selectedType() === 'CODING' && form.controls.language.value` is truthy, passing `[initialTestCases]="codingInitialTestCases()"` and binding `(testCasesChange)="onTestCasesChange($event)"`

## 4. QuestionFormComponent — submit & edit-mode wiring

- [x] 4.1 In `ngOnInit()`, when editing a `CodingQuestionResponse` (`type === 'CODING'`), `form.patchValue({ language: q.language })`
- [x] 4.2 In `submit()`, add a `CODING` branch: validate `language` is non-empty (error message if not), then call `this.save({ type: 'CODING', questionBankIds, question: questionText, language, testCases: this.currentTestCases })`
- [x] 4.3 Confirm `resolveType()` requires no change (CODING responses always carry `type`)

## 5. Tests

> **Blocked**: `dap-frontend` has no unit-test infrastructure (no Jasmine/Karma devDependencies, no `test` architect target in `angular.json`, zero `*.spec.ts` files anywhere in the project). This is a pre-existing, project-wide gap, not introduced by this slice. Tasks 5.1–5.5 are deferred until test infrastructure is set up under a separate ticket.

- [ ] 5.1 `TestCaseBuilderComponent` unit tests: add row appends blank row; remove row deletes only that row; expected-output-required validation; timeout/memory range validation
- [ ] 5.2 `QuestionFormComponent` unit tests: `CODING` and not `DOC` appear in the type selector; language dropdown renders Java/Python/C#; form invalid without language; test case builder hidden until language selected and visible after
- [ ] 5.3 `QuestionFormComponent` submit tests: valid `CODING` submission calls `QuestionService.createQuestion()` with `type: 'CODING'`, `questionBankIds`, `language`, and `testCases` (including the empty-`testCases` case); missing language does not call the service
- [ ] 5.4 `QuestionFormComponent` edit-mode test: opening an existing `CODING` question pre-populates the language dropdown and test case builder rows from `CodingQuestionResponse`
- [ ] 5.5 `QuestionFormComponent` edit-mode test: opening an existing `DOC` question still renders the locked `DOC` type badge and editable marks field
