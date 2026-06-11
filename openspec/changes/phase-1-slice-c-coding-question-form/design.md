## Context

`QuestionFormComponent` (`dap-frontend/src/app/features/question-management/components/question-form/`) is a single standalone, `OnPush` form that handles every question type via a `selectedType` signal and a type-specific conditional template region. Each prior question-type addition (`MCQ_PLUS`, `GROUP`) followed the same pattern: a new inline builder sub-component (`McqOptionBuilderComponent`, `GroupChildrenBuilderComponent`) that takes `initial*` data via `input()`, emits a `*Change` output on every edit, and is read by the parent's `submit()`/`ngOnInit()`. `question.model.ts` and `question.service.ts` already define `CodingQuestionRequest`, `CodingQuestionResponse`, `CodingQuestionLanguage`, `TestCase`, `TestCaseRequest`, and include `'CODING'` in `QuestionType` and the service's request/response unions — this slice only adds the missing UI.

## Goals / Non-Goals

**Goals:**
- Let a marker create or edit a `CODING` question: select a language, write the question text, pick question banks, and add/edit/remove inline test cases
- Remove `'DOC'` from the creatable type list, per the Phase 1 soft-deprecation of Doc Question
- Follow the existing `QuestionFormComponent` conditional-section + inline-builder pattern exactly (no new architectural pattern)

**Non-Goals:**
- Backend wiring (`QuestionService.create()` branch for `CodingQuestionRequest`, `CodingQuestion`/`TestCase` entities) — that is Slices A/B (ATG-60/61), separate tickets
- Code execution, sandboxed running, or any test-case validation beyond client-side field constraints
- Migrating or relabeling existing `DOC` questions — they remain visible and editable (type locked in edit mode); only *creation* of new `DOC` questions is removed from the UI

## Decisions

**1. New `TestCaseBuilderComponent`, mirroring `GroupChildrenBuilderComponent`**
- Standalone, `OnPush`, selector `dap-test-case-builder`
- `input()`: `initialTestCases: TestCase[]` (default `[]`)
- `output()`: `testCasesChange: TestCaseRequest[]`
- Internal `signal<TestCaseRequest[]>` list; `addRow()` / `removeRow(index)` / `updateRow(index, partial)` each call `emit()`
- Each row renders: input textarea (optional), expected output textarea (required), timeout number input (1–60, default 10), memory MB number input (64–1024, default 256)
- Row-level validation (required expected output, numeric ranges) is enforced via `min`/`max`/`required` attributes and a per-row inline error message, consistent with how `GroupChildrenBuilderComponent` and `McqOptionBuilderComponent` handle validation (no separate `FormArray` — plain signal-backed rows, validated on submit by the parent)

**2. Language as a plain reactive form control on `QuestionFormComponent.form`**
- Add `language: ['' as CodingQuestionLanguage | '', Validators.required]` style control to the existing `FormBuilder.nonNullable.group(...)`, gated by `selectedType() === 'CODING'`
- Dropdown options are a static `readonly codingLanguages: { value: CodingQuestionLanguage; label: string }[]` array: `JAVA → 'Java'`, `PYTHON → 'Python'`, `CSHARP → 'C#'` — avoids a magic-string template and gives a single place to extend if more languages are added later
- Test case builder section is shown only when `language` has a truthy value (`@if (selectedType() === 'CODING' && form.controls.language.value)`)

**3. Type selector: swap `'DOC'` for `'CODING'` in `questionTypes`**
- `readonly questionTypes: QuestionType[] = ['MCQ', 'MCQ_PLUS', 'TEXT', 'GROUP', 'CODING']` (DOC removed, CODING added)
- In edit mode the type badge is rendered from `selectedType()` regardless of whether it's in `questionTypes` — so an existing `DOC` question opened for edit still shows `DOC` as a locked badge and the existing `marks`-only `DOC` section in the template is left in place (read/edit support, not creation)

**4. `submit()` / `ngOnInit()` wiring for `CODING`**
- `submit()`: when `selectedType() === 'CODING'`, validate `language` is set and (if any rows exist) each row passes its constraints, then call `this.save({ type: 'CODING', questionBankIds, question: questionText, language, testCases })` — `testCases` may be an empty array (optional per `CodingQuestionRequest`)
- `ngOnInit()`: when editing a `CodingQuestionResponse`, `form.patchValue({ language: q.language })` and pass `q.testCases` to `TestCaseBuilderComponent` via a new `codingInitialTestCases` computed signal (same shape as `groupInitialChildren`)
- `resolveType()` needs no change — `q.type` is always present on `CodingQuestionResponse`

## Risks / Trade-offs

- **[Risk]** Marker creates a `CODING` question via this UI before Slice B (ATG-61) merges → `POST /api/questions` has no `CodingQuestionRequest` branch in `QuestionService.create()` and will fall through to an unhandled-type error.
  → **Mitigation**: none required for this slice — per the Phase 1 plan, Slices run in parallel and merge order is not guaranteed, but `Done When` for this slice is UI/unit-test scoped (mocked `QuestionService`). This is a known, accepted cross-slice integration gap closed when ATG-61 merges; no flag or guard is added to avoid over-engineering a temporary state.
- **[Risk]** Removing `'DOC'` from `questionTypes` could orphan the `@if (selectedType() === 'TEXT' || selectedType() === 'DOC')` marks block and the dedicated `DOC` template branches if `DOC` becomes fully unreachable.
  → **Mitigation**: those branches stay reachable via edit mode (`selectedType()` is set from `existingQuestion().type`, independent of `questionTypes`), so no template changes needed beyond the type-selector array.

## Migration Plan

Frontend-only, additive UI change behind no feature flag — ships as a normal merge to `main`. No data migration. Rollback is a plain revert of the component/template changes.
