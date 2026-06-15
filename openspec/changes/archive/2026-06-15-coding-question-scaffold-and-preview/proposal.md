## Why

Coding question authors have no way to verify their test cases work before a question goes live, and candidates start with a blank editor even when the test harness expects a specific class or method signature. Both gaps cause wasted assessment time and silent failures.

## What Changes

- **Per-language code scaffold**: The coding answer editor is pre-populated with a language-specific boilerplate (Java class + main, Python main block, C# class + Main) when the candidate has no previously saved code, so candidates always start from a valid structural skeleton
- **"Test question" button on coding questions**: A button appears on each coding question card in the question management UI (visible to ADMIN and MARKER only); clicking it opens an inline test panel where the user can enter code, run it against the question's own test cases, and see pass/fail results — identical UX to the candidate run panel
- **New backend preview endpoint**: `POST /api/questions/{id}/execute` accepts `{ code }` from ADMIN/MARKER, delegates to `CodeExecutionService.execute(question, code)`, and returns `CodeExecuteResponse` — no assessment, no persisted response

## Capabilities

### New Capabilities
- `coding-question-preview`: The admin/marker test-run panel in question management and the backend `POST /api/questions/{id}/execute` preview endpoint

### Modified Capabilities
- `coding-assessment-ui`: Scaffold initialisation — when `CodingAnswerComponent` has no saved code, seed the editor with the per-language template instead of an empty string

## Impact

- **Backend**: New method `QuestionService.executeQuestion(id, code)` delegating to `CodeExecutionService`; new `POST /api/questions/{id}/execute` endpoint in `QuestionController` restricted to ADMIN + MARKER
- **Frontend**: `CodingAnswerComponent` gains scaffold logic; new `CodingQuestionPreviewComponent` in `question-management`; `QuestionListComponent` updated to show the test button and host the preview panel; `QuestionService` extended with `executeQuestion()`
- No DB schema changes, no new dependencies
