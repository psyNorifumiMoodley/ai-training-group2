## Why

V2 Phase 1 delivered the `CodingQuestion` type with inline test cases, but there are no contracts for the candidate-facing experience: no `CodingResponseRequest` DTO, no execute endpoint, and no Angular model types. Without these contracts in place, the three implementation slices of Phase 2 (schema migration, backend service wiring, Angular UI) cannot develop in parallel — they have no shared API surface to build against. Slice 0 establishes all stubs, DTOs, and Angular type definitions so that Slice A (schema), Slice B (backend logic), and Slice C (Angular UI) can proceed simultaneously.

## What Changes

- **New DTO** — `CodingResponseRequest` record implementing `ResponseRequest`: `@NotBlank String code`
- **New DTO** — `TestCaseResultResponse` record: `UUID testCaseId`, `boolean passed`, `String actualOutput`, `Long executionTimeMs`, `Long memoryUsedMb`, `String errorMessage`, `int ordinal`
- **New DTO** — `CodeExecuteResponse` record: `List<TestCaseResultResponse> results`, `Instant executedAt`
- **Updated sealed interface** — `ResponseRequest`: add `CodingResponseRequest` to `permits` and `@JsonSubTypes`
- **New stub endpoint** — `POST /api/assessments/{id}/responses/{questionId}/execute` on `AssessmentController` returning hardcoded `CodeExecuteResponse`
- **Angular model** — `CodingResponseRequest { code: string }` added to `assessment-session.model.ts`; `TestCaseResult` and `CodeExecuteResponse` interfaces added to `assessment-session.model.ts`; `CodingResponseRequest` added to `ResponseRequest` union type
- **Angular stub** — `executeCode(assessmentId, questionId, code)` stub method added to `CandidateAssessmentService` returning `EMPTY`

## Capabilities

### New Capabilities

- `coding-response`: `CodingResponse` entity/DTO persistence — candidate saves code for a CODING question; `TestCaseResult` rows stored per execution run; response auto-executed against Judge0 on submission
- `code-execution`: Judge0 integration via `CodeExecutionService`; on-demand `POST /execute` endpoint; auto-execution of all `CodingResponse` rows on assessment submission; per-test-case pass/fail, actual output, timing, and memory stored
- `coding-assessment-ui`: Angular candidate-facing CODING question answer panel — code editor, language display, "Run" button with loading state, test case results grid (input/expected/actual/pass), integrated into the assessment-taking flow

### Modified Capabilities

- `assessment-generation`: CODING question type is now eligible in assessment composition; combined doc+coding slot limit documented; CODING type display added to question picker in the Angular generation UI

## Impact

- **Backend DTOs** — `ResponseRequest.java` updated (sealed interface); three new DTO records added
- **Backend controller** — `AssessmentController.java`: one new stub endpoint added
- **Frontend models** — `assessment-session.model.ts`: three new interfaces, `ResponseRequest` union extended
- **Frontend service** — `candidate-assessment.service.ts`: one new stub method added
- **Downstream slices blocked until this merges** — Slice A (schema: `coding_response` + `test_case_result` tables), Slice B (backend logic: entity, Judge0, service), Slice C (Angular UI: coding answer panel)
- No entity changes, no Liquibase changesets, no breaking changes to existing response types
