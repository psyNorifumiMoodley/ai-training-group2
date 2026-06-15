## Why

Slice A delivered the `CodingResponse` and `TestCaseResult` schema and JPA layer, but the system cannot yet save, execute, or auto-mark candidate code — `CodingResponseRequest` hits a `UnsupportedOperationException` in `ResponseService`, and the execute endpoint returns a hardcoded empty stub. Slice B wires the real backend logic: Judge0 integration, response persistence for coding questions, on-demand code execution, and auto-execution on assessment submission.

## What Changes

- **New service interface** — `CodeExecutionService`: `List<TestCaseResultResponse> execute(CodingQuestion question, String code)` — contracts the Judge0 adapter
- **New service impl** — `Judge0CodeExecutionService`: HTTP client (Spring `RestClient`) calling Judge0 synchronous batch API; maps `CodingQuestionLanguage` → Judge0 language IDs (JAVA=91, PYTHON=71, CSHARP=51); handles errors gracefully per test case
- **New config properties** — `code-execution.judge0.base-url` and `code-execution.judge0.api-key` in `application.properties`
- **Modified `ResponseService`** — adds `CodingResponseRequest` branch to `upsertResponse()` (save/upsert `CodingResponse`); new `executeCode(UUID assessmentId, UUID questionId, UUID requestingUserId)` method (load saved code → call Judge0 → persist `TestCaseResult` rows → update `executed_at`); new `autoExecuteCodingResponses(UUID assessmentId)` for use on submission
- **Modified `AssessmentController`** — `executeCode` stub replaced with real `responseService.executeCode()` call; adds `@AuthenticationPrincipal` for candidate ownership check
- **Modified `AssessmentService.submit()`** — calls `responseService.autoExecuteCodingResponses(assessmentId)` after MCQ auto-marking; errors logged and swallowed so submission never blocks on Judge0 failure
- **New unit tests** — `ResponseService` tests for `CodingResponseRequest` save and `executeCode` (success, 404 no-response, 409 submitted assessment)

## Capabilities

### New Capabilities

- `code-execution`: Judge0 HTTP integration — `CodeExecutionService` interface + `Judge0CodeExecutionService` implementation; language ID mapping; batch submission; per-test-case error handling

### Modified Capabilities

- `coding-response`: `ResponseService` now handles `CodingResponseRequest` persistence (save/upsert) and `executeCode` coordination; auto-execution triggered on submission — new requirements on top of Slice A's schema layer
- `code-execution`: Requirements for the execute endpoint moving from stub to real behaviour (status checks, candidate ownership, 404 when no code saved, result persistence)

## Impact

- **New files**: `CodeExecutionService.java` (interface), `Judge0CodeExecutionService.java` (service impl) in `service/`
- **Modified files**: `ResponseService.java`, `AssessmentController.java`, `AssessmentService.java`, `application.properties`
- **New test**: additions to `ResponseServiceTest` or a new `CodingResponseServiceTest`
- **No schema changes** — all DDL was delivered in Slice A
- **No Angular changes** — those belong in Slice C
- **External dependency**: Judge0 must be reachable at `code-execution.judge0.base-url` for integration; tests mock the HTTP client
