## Why

Phase 1 (Coding Question, Jira epic ATG-58) introduces a new `CODING` question type with an inline test-case list and a soft-deprecated `DOC` type. The shared types (`CodingQuestionRequest`, `CodingQuestionResponse`, `CodingQuestionLanguage`, `TestCase`, `TestCaseRequest`) and `QuestionService` union signatures already exist in the Angular codebase (`question.model.ts`, `question.service.ts`), but `QuestionFormComponent` — the single form used to create and edit every question type — has no UI to select `CODING`, choose a language, or manage test cases. Slice C (ATG-62) closes that gap so markers can author coding questions from the question bank UI, and retires "Doc Question" as a creatable type.

## What Changes

- Add `'CODING'` to the type selector in `QuestionFormComponent` and remove `'DOC'` — markers can no longer create new Doc questions from the UI (existing Doc questions remain editable in edit mode, where the type is locked)
- Add a required `language` dropdown (Java / Python / C#) shown when type is `CODING`
- New `TestCaseBuilderComponent` (standalone, `OnPush`, reactive) — add/remove/edit inline test case rows (input, expected output, timeout 1–60s, memory 64–1024MB); rendered only once a language is selected, following the same inline-builder pattern as `GroupChildrenBuilderComponent` and `McqOptionBuilderComponent`
- Wire `submit()` to build a `CodingQuestionRequest` (`questionBankIds`, `question`, `language`, `testCases`) and call the existing `QuestionService.createQuestion()` / `updateQuestion()`
- Wire `ngOnInit()` edit-mode population for `CODING`: pre-select `language` and populate the test case builder from `CodingQuestionResponse.testCases`

## Capabilities

### New Capabilities
- `coding-question-form`: Angular question editor support for the `CODING` question type — type selector entry, required language dropdown, inline test case builder (add/remove/edit rows with input, expected output, timeout, memory), and removal of the "Doc Question" creation option

### Modified Capabilities
(none — `question-form` is not yet a published capability spec; this slice's UI changes are scoped entirely within the new `coding-question-form` capability)

## Impact

- `dap-frontend/src/app/features/question-management/components/question-form/question-form.component.ts` and `.html` — add `CODING` to `questionTypes`, remove `'DOC'`, add `language` form control and conditional sections, wire submit/edit-mode handling for `CodingQuestionRequest`/`CodingQuestionResponse`
- New `dap-frontend/src/app/features/question-management/components/test-case-builder/` — `TestCaseBuilderComponent` (`.ts` + `.html`)
- No backend changes — `CodingQuestionRequest`/`Response` DTOs already exist (Slice 0); `QuestionService.create()` does not yet branch on `CodingQuestionRequest` (Slice B, ATG-61, separate ticket), so end-to-end submission will only work once that slice merges. This slice's "Done When" criteria are UI/unit-test scoped and do not depend on Slice B.
- No new routes, no Liquibase changesets, no entity changes
