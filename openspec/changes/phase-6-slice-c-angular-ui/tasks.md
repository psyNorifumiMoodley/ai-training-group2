## 1. Wire QuestionBankService

- [x] 1.1 Implement `createQuestionBank(name: string)`: `POST /api/question-banks` with body `{ name }`; return `Observable<QuestionBankResponse>`
- [x] 1.2 Implement `renameQuestionBank(id: string, name: string)`: `PUT /api/question-banks/{id}` with body `{ name }`; return `Observable<QuestionBankResponse>`
- [x] 1.3 Implement `deleteQuestionBank(id: string)`: `DELETE /api/question-banks/{id}`; return `Observable<void>`

## 2. Delete AddQuestionModalComponent

- [x] 2.1 Verify `AddQuestionModalComponent` is not imported or used in any template or route outside `question-banks/` (search for `dap-add-question-modal` and the import in any component)
- [x] 2.2 Delete `dap-frontend/src/app/features/question-banks/add-question-modal/add-question-modal.component.ts` and `.html`
- [x] 2.3 Remove any import of `AddQuestionModalComponent` from `BankListComponent` if present

## 3. New GroupChildrenBuilderComponent

- [x] 3.1 Create `dap-frontend/src/app/features/question-management/components/group-children-builder/group-children-builder.component.ts` — standalone, `OnPush`; `input()` signal `initialChildren: GroupChildResponse[]` (default `[]`); `output()` `childrenChange: GroupChildRequest[]`; internal signal `children` of `{ questionText: string; keywords: string[]; marks: number }[]`
- [x] 3.2 `addChild()` method: appends `{ questionText: '', keywords: [], marks: 1 }` to `children`; emits updated list
- [x] 3.3 `removeChild(index: number)` method: removes entry at index; emits updated list
- [x] 3.4 `updateChildText(index: number, text: string)`: updates `questionText` at index; emits
- [x] 3.5 `updateChildKeywords(index: number, keywords: string[])`: updates `keywords` at index; emits
- [x] 3.6 `updateChildMarks(index: number, marks: number)`: updates `marks` at index; emits
- [x] 3.7 On `ngOnInit`: if `initialChildren()` is non-empty, map to internal shape and set `children` signal
- [x] 3.8 Create `group-children-builder.component.html`: render each child as a card with a `<textarea>` for question text, `<dap-keyword-list>` for keywords, a number input for marks (min 1), and a remove button; "Add child" button at the bottom; import `KeywordListComponent` and `ButtonComponent`

## 4. Overhaul QuestionFormComponent

- [x] 4.1 Inject `QuestionBankService`; on init call `getQuestionBanks()` and store result in `availableBanks` signal (`QuestionBankResponse[]`)
- [x] 4.2 Add `selectedBankIds` signal (`Set<string>`); initialise to empty set
- [x] 4.3 Remove `category: ['', Validators.required]` from `this.fb.nonNullable.group()`
- [x] 4.4 Add `toggleBank(id: string)` method: adds or removes `id` from `selectedBankIds`; `isBankSelected(id: string): boolean` helper
- [x] 4.5 In `ngOnInit` edit mode: set `selectedBankIds` from `existingQuestion.questionBanks.map(b => b.id)`
- [x] 4.6 Add `marks` `FormControl<number>` to the form group (`Validators.required`, `Validators.min(1)`, default `1`)
- [x] 4.7 In `ngOnInit` edit mode for TEXT: patch `marks` from `(existingQuestion as TextQuestionResponse).marks`
- [x] 4.8 In `ngOnInit` edit mode for DOC: patch `marks` from `(existingQuestion as DocQuestionResponse).marks`
- [x] 4.9 Add `MCQ_PLUS` to the `questionTypes` array; add MCQ_PLUS form fields: `followUpQuestion` (`FormControl<string>`, required), `followUpMarks` (`FormControl<number>`, required, min 1); add `followUpKeywords` signal (`string[]`, default `[]`)
- [x] 4.10 In `ngOnInit` edit mode for MCQ_PLUS: patch MCQ fields + `followUpQuestion`, `followUpMarks`; set `followUpKeywords` from response
- [x] 4.11 Remove `selectedFollowUpIds` signal, `loadTextQuestions()` method, and `textQuestions` signal (obsolete group follow-up logic)
- [x] 4.12 Add `groupChildren` signal (`GroupChildRequest[]`, default `[]`); `onGroupChildrenChange(children: GroupChildRequest[])` method
- [x] 4.13 In `ngOnInit` edit mode for GROUP: set `groupChildren` signal from `(existingQuestion as GroupQuestionResponse).children` (map to `GroupChildRequest` shape)
- [x] 4.14 Import `GroupChildrenBuilderComponent` in component `imports` array
- [x] 4.15 Update `submit()` — add bank validation: if `selectedBankIds().size === 0`, set `errorMsg` to "Select at least one question bank." and return
- [x] 4.16 Update `submit()` MCQ branch: pass `questionBankIds: Array.from(selectedBankIds())` (remove hardcoded `[]`)
- [x] 4.17 Update `submit()` MCQ_PLUS branch: build `McqPlusQuestionRequest` with `questionBankIds`, `followUpQuestion`, `followUpKeywords`, `followUpMarks`; validate MCQ builder is valid and `followUpQuestion` non-empty
- [x] 4.18 Update `submit()` TEXT branch: pass `questionBankIds` and `marks` from form control (remove hardcoded `marks: 1`)
- [x] 4.19 Update `submit()` DOC branch: pass `questionBankIds` and `marks` from form control
- [x] 4.20 Update `submit()` GROUP branch: guard `groupChildren().length === 0` with error "Add at least one child question."; pass `questionBankIds` and `children: groupChildren()`
- [x] 4.21 Update `question-form.component.html`: replace `category` input with a checkbox group iterating `availableBanks()`; add marks input shown when type is TEXT or DOC; add MCQ_PLUS section (followUpQuestion textarea, followUpKeywords keyword-list, followUpMarks number input) shown when type is MCQ_PLUS; replace old follow-up picker section with `<dap-group-children-builder>` shown when type is GROUP

## 5. Overhaul BankListComponent

- [x] 5.1 Inject `QuestionBankService`; replace `banks = computed(...)` with `banks = signal<QuestionBankResponse[]>([])`; load on init via `questionBankService.getQuestionBanks().pipe(takeUntilDestroyed())`
- [x] 5.2 Change `selectedCategory` signal type from `string` to `string` (bank ID); update `selectBank()` to accept a bank ID and set `selectedBankId` signal
- [x] 5.3 Update `loadQuestions()` to call `questionService.getQuestions(0, 100, this.selectedBankId())` when a bank is selected, or `getQuestions(0, 100)` when none is selected; set `allQuestions` signal from `page.content`
- [x] 5.4 Update `banks` display in template to use `bank.id` and `bank.name` from `QuestionBankResponse`
- [x] 5.5 Add `showCreateBankForm` signal (boolean, default false); `newBankName` signal (string, default ''); `creatingBank` signal (boolean)
- [x] 5.6 Add `submitCreateBank()` method: call `questionBankService.createQuestionBank(newBankName())`; on success reload banks via `loadBanks()`, reset form signals, call `toastService.success()`; on 409 set a `createBankError` signal to "A bank with this name already exists"; on other error call `toastService.error()`
- [x] 5.7 Add `renamingBankId` signal (`string | null`); `renameDraft` signal (string); `renamingBank` signal (boolean)
- [x] 5.8 Add `submitRenameBank(bank: QuestionBankResponse)` method: call `questionBankService.renameQuestionBank(bank.id, renameDraft())`; on success reload banks, clear rename state, `toastService.update()`; on 409 set `renameBankError` signal; on other error call `toastService.error()`
- [x] 5.9 Add `deletingBank` signal (`QuestionBankResponse | null`); `deletingBankInProgress` signal (boolean)
- [x] 5.10 Add `confirmDeleteBank()` method: call `questionBankService.deleteQuestionBank(deletingBank()!.id)`; on success reload banks, clear deleting state, `toastService.success()`; on 409 `toastService.error('This bank still has questions — remove them first')`; on other error `toastService.error()`
- [x] 5.11 Update `bank-list.component.html`: add Create Bank button and inline form; add rename icon per bank row that sets `renamingBankId` and shows an editable input in place of the bank name; add delete icon per bank row that sets `deletingBank`; use existing `ConfirmModalComponent` for bank delete confirmation

## 6. Update QuestionPickerComponent

- [x] 6.1 Inject `QuestionBankService`; replace `banks = computed(...)` with `banks = signal<QuestionBankResponse[]>([])`; load banks on init via `questionBankService.getQuestionBanks().pipe(takeUntilDestroyed())`
- [x] 6.2 Change `selectedCategory` signal from `string` (bank name) to `selectedBankId` signal of type `string` (bank ID)
- [x] 6.3 Update `selectBank(id: string)` to set `selectedBankId` to bank ID and call `loadQuestionsForBank(id)`
- [x] 6.4 Add `loadQuestionsForBank(bankId: string)` method: call `questionService.getQuestions(0, 100, bankId)`; set `allQuestions` from `page.content`
- [x] 6.5 On init: after banks load, auto-select `banks()[0].id` and call `loadQuestionsForBank()` if banks are non-empty
- [x] 6.6 Remove the old constructor `getQuestions(0, 1000)` full-dump call
- [x] 6.7 Update template: iterate `banks()` using `bank.id` and `bank.name`; update `selectBank()` call to pass `bank.id`

## 7. Spec files

- [x] 7.1 Write `specs/question-bank-management/spec.md` — Angular-side requirements for bank CRUD UI
- [x] 7.2 Write `specs/question-form/spec.md` — Angular-side requirements for the updated question form (bank selector, marks, inline children, MCQ_PLUS)
