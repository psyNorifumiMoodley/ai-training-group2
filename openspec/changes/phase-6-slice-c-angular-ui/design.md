## Context

Phase 6 Slice B is complete: the backend exposes real CRUD for `/api/question-banks`, persists `questionBankIds` on all question types, stores `GroupQuestionChild` rows inline, and accepts `MCQ_PLUS` questions. The Angular layer has not caught up:

- `QuestionBankService.createQuestionBank()`, `renameQuestionBank()`, `deleteQuestionBank()` all return `EMPTY`
- `BankListComponent` derives the bank list by loading all questions and reading `questionBanks[0].name` — no server-side filtering, no bank CRUD
- `QuestionFormComponent` has a `category` text field (removed from the API in Slice 0) and hardcodes `questionBankIds: []` in every save call
- GROUP questions are still built by picking existing `TextQuestion` IDs (`selectedFollowUpIds` + `loadTextQuestions()`) — the `group_question_follow_up` table was dropped in Slice A
- MCQ_PLUS is not in the `questionTypes` array; no form fields exist for follow-up content
- `AddQuestionModalComponent` references stub data and `assessment.model.ts` types that predate Phase 6 — it is dead code

## Goals / Non-Goals

**Goals:**
- Wire the three unimplemented `QuestionBankService` methods to real HTTP calls
- Replace `BankListComponent`'s client-side bank derivation with real service calls; add create/rename/delete bank UX
- Overhaul `QuestionFormComponent`: bank multi-select, marks, inline GROUP children, MCQ_PLUS support
- Introduce `GroupChildrenBuilderComponent` for managing inline children
- Update `QuestionPickerComponent` to load banks from service and filter questions by bank ID server-side
- Delete `AddQuestionModalComponent` (obsolete)

**Non-Goals:**
- `McqPlusResponse` marking UI — that is a separate marking flow concern
- Pagination of the question bank list (banks are expected to be few; `getQuestionBanks()` loads all)
- Coding question type UI — out of scope for Phase 6

## Decisions

### 1. QuestionBankService — HTTP calls match the backend contract exactly

`createQuestionBank(name)` → `POST /api/question-banks` with body `{ name }`; returns `Observable<QuestionBankResponse>`.
`renameQuestionBank(id, name)` → `PUT /api/question-banks/{id}` with body `{ name }`; returns `Observable<QuestionBankResponse>`.
`deleteQuestionBank(id)` → `DELETE /api/question-banks/{id}`; returns `Observable<void>`.

The existing `getQuestionBanks()` call targets `/question-banks` without pagination params — this is intentional, returning all banks in one call. The API defaults `size=20` but bank counts are small enough in practice; this can be revisited later if needed.

---

### 2. BankListComponent loads banks from QuestionBankService; loads questions via ?questionBankId=

**Decision:** On init, call `QuestionBankService.getQuestionBanks()` to populate the `banks` signal. When a bank is selected, call `QuestionService.getQuestions(0, 100, bank.id)` to load that bank's questions. This replaces the current approach of loading all 1000 questions and filtering client-side.

**Rationale:** The derived-banks pattern breaks if a question bank has no questions yet (the bank would not appear in the list). Server-side filtering also removes the 1000-question dump which will not scale.

---

### 3. Bank CRUD inline in BankListComponent — no separate route

**Decision:** Create bank: a small inline form (name input + save button) shown by toggling a `showCreateForm` signal. Rename: inline in the bank card — clicking a rename icon puts the bank name into an editable input. Delete: existing `ConfirmModalComponent` pattern.

**Rationale:** A dedicated bank management route would require a new Angular route and an extra navigation step. The bank list page already renders the bank names as a sidebar; inline editing keeps the mental model simple and consistent with how questions are edited on the same page.

---

### 4. 409 conflict handling for bank create, rename, and delete

**Decision:** Catch the HTTP 409 in the `error` handler and display the error message from the API response body as a field-level error (create/rename: "A bank with this name already exists"; delete: "This bank still has questions — reassign or delete them first"). Use `ToastService.error()` as a fallback if the body is unavailable.

**Rationale:** The backend returns a descriptive error message in the `{ status, error, message, timestamp }` envelope. Reading `error.error.message` surfaces this to the user without coupling the frontend to hardcoded strings.

---

### 5. QuestionFormComponent — questionBankIds multi-select via checkboxes

**Decision:** Remove the `category` `FormControl`. Add a `questionBankIds` control as a `FormArray` of `FormControl<boolean>` indexed by position in the loaded bank list, or more simply, maintain a `selectedBankIds` signal (`Set<string>`) and validate that it is non-empty before saving. Render the banks as a labelled checkbox group loaded from `QuestionBankService.getQuestionBanks()` on component init.

**Rationale:** A `FormArray<FormControl<boolean>>` approach is idiomatic but couples the bank list order to the control structure. A signal-based `Set<string>` is simpler to populate in edit mode (iterate `existingQuestion.questionBanks` and add their IDs) and simpler to validate.

**Alternative considered:** A `<select multiple>` element. Rejected: Tailwind styling on multi-selects is poor; a checkbox group is more accessible and matches the UI patterns already in the codebase.

---

### 6. GroupChildrenBuilderComponent — standalone component, owned by QuestionFormComponent

**Decision:** Create `GroupChildrenBuilderComponent` at `question-management/components/group-children-builder/`. It accepts `initialChildren = input<GroupChildResponse[]>([])` and emits `childrenChange = output<GroupChildRequest[]>()` whenever the list changes. Internally it holds a `children` signal of `{ questionText: string; keywords: string[]; marks: number }[]`. It renders a list of child editors, each with a text input, a `KeywordListComponent` instance, and a marks number input. An "Add child" button appends a blank entry; a remove button deletes one.

**Rationale:** Making it a separate component keeps `QuestionFormComponent` from growing further and allows the children builder to be independently testable. The `output()` pattern is consistent with `KeywordListComponent` and `McqOptionBuilderComponent` already used in the form.

---

### 7. MCQ_PLUS form fields alongside MCQ fields

**Decision:** When `selectedType()` is `MCQ_PLUS`, render the same MCQ section (option builder, correct answers) AND an additional section below it: `followUpQuestion` textarea, `followUpKeywords` via `KeywordListComponent`, and `followUpMarks` number input. Add `MCQ_PLUS` to the `questionTypes` array in `QuestionFormComponent`.

**Rationale:** MCQ_PLUS is an MCQ with extras — reusing the same option builder avoids duplicating that logic. The follow-up section renders only when type is `MCQ_PLUS`, so the form stays uncluttered for plain MCQ.

---

### 8. QuestionPickerComponent — load banks first, then questions per bank

**Decision:** On init, call `QuestionBankService.getQuestionBanks()`. Auto-select the first bank. When a bank is selected, call `QuestionService.getQuestions(0, 100, bank.id)` to load that bank's questions. Replace the current `allQuestions` dump approach.

**Rationale:** The current approach loads up to 1000 questions on every assessment generation. Loading per bank is more efficient and consistent with the `BankListComponent` pattern established in decision 2.

## Risks / Trade-offs

- **`AddQuestionModalComponent` deletion** — The component is imported nowhere else in the routing tree (it was only used in `BankListComponent` template which already uses `QuestionFormComponent` instead). Verify before deleting.
- **bank-list loads questions per selected bank** — If a bank has > 100 questions, the current `size=100` cap means questions beyond 100 won't show. Acceptable for now; pagination within a bank can be added post-Phase 6.
- **`selectedBankIds` signal vs `FormControl`** — The signal approach means bank selection doesn't participate in `form.invalid` / `form.markAllAsTouched()`. The `submit()` method must explicitly check `selectedBankIds().size > 0` and set an `errorMsg` if not.
- **GROUP form: children validation** — At least one child is required. `GroupChildrenBuilderComponent` emits an empty array initially; `QuestionFormComponent.submit()` must guard against saving with zero children.

## Open Questions

None — API contracts and entity structure are fixed by Slices 0, A, and B. Column names, request/response shapes, and error codes are all known.
