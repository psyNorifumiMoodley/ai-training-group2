## Context

Spring Boot 3.4.1 ships `RestClient` (Spring 6.1+), the modern synchronous HTTP client, with no additional Maven dependency — it is part of `spring-boot-starter-web`. This is the preferred client over `RestTemplate` for new code.

`ResponseService.upsertResponse()` already handles MCQ, Text, Doc, Group, and McqPlus via pattern-matching `instanceof` branches, but throws `UnsupportedOperationException` for `CodingResponseRequest`. Fixing this is the primary unblock for the `PUT /responses/{questionId}` endpoint with CODING payloads.

The execute endpoint (`POST /responses/{questionId}/execute`) is a hardcoded stub from Slice 0. It has no request body — the candidate's code is loaded from the previously saved `CodingResponse`. The stub must be replaced with a real implementation that calls Judge0 and persists results.

Auto-execution on submission was defined in the `coding-response` spec (Slice 0 scope, Slice B ownership): when a candidate submits, all `CodingResponse` rows in the assessment must be executed against Judge0 and results persisted before the submission completes. Failure of Judge0 must not block submission.

## Goals / Non-Goals

**Goals:**
- `PUT /api/assessments/{id}/responses/{questionId}` with `{ "code": "..." }` persists a `CodingResponse` row (upsert)
- `POST /api/assessments/{id}/responses/{questionId}/execute` loads the saved code, calls Judge0 synchronously, persists `TestCaseResult` rows (delete-and-reinsert), returns `CodeExecuteResponse`
- `POST /api/assessments/{id}/submit` auto-executes all `CodingResponse` rows before transitioning to `SUBMITTED`
- `Judge0CodeExecutionService` reads `base-url` and `api-key` from properties — no hardcoded values
- Judge0 language IDs: JAVA→91, PYTHON→71, CSHARP→51
- Per-test-case Judge0 errors are surfaced as `passed=false` with `errorMessage` set — they do not throw

**Non-Goals:**
- Angular UI — Slice C
- No new Liquibase changesets — schema is complete from Slice A
- No async Judge0 poll pattern — synchronous batch only (Decision 3 from Slice 0)
- No scoring of coding responses — that is Slice C (or a future marking slice)

## Decisions

### Decision 1: `CodeExecutionService` interface takes `CodingQuestion` + `String code`, returns `List<TestCaseResultResponse>`

The interface boundary sits at the Judge0 adapter level. It receives the raw ingredients (question with its test cases and language, and the candidate code) and returns the per-test-case DTO results. `ResponseService` handles all entity coordination before and after: loading the `CodingResponse`, calling the service, persisting `TestCaseResult` entities, updating `executed_at`. This keeps `Judge0CodeExecutionService` thin and unit-testable without the JPA stack.

**Alternative considered:** Interface takes assessment + question IDs and handles everything internally. Rejected — blurs the boundary between HTTP adapter and business logic; makes the service harder to test without a full Spring context.

---

### Decision 2: `Judge0CodeExecutionService` uses `RestClient` (synchronous); no new Maven dependency

`RestClient` is available in `spring-boot-starter-web` since Spring Boot 3.2. It is instantiated in the `@PostConstruct` or constructor via `RestClient.builder().baseUrl(baseUrl).build()`. No `spring-boot-starter-webflux` or `spring-boot-starter-web-services` is needed.

**Alternative considered:** `RestTemplate`. Rejected — deprecated in favour of `RestClient` for new Spring 6 code.

---

### Decision 3: Judge0 batch endpoint `POST /submissions/batch?base64_encoded=false&wait=true`

The synchronous (`wait=true`) batch mode returns all results in one HTTP response. This matches Decision 3 from Slice 0 (synchronous, no polling). One HTTP call per `executeCode()` invocation covers all test cases for a question.

The request body shape:
```json
{
  "submissions": [
    { "source_code": "...", "language_id": 91, "stdin": "input", "expected_output": "expected", "cpu_time_limit": 10, "memory_limit": 262144 }
  ]
}
```
The response body shape (`submissions` array in the same order):
```json
{
  "submissions": [
    { "stdout": "...", "time": "0.10", "memory": 1024, "stderr": null, "compile_output": null, "status": { "id": 3, "description": "Accepted" } }
  ]
}
```
Judge0 status ID 3 = Accepted (passed). All other IDs = failed.

`memory_limit` in Judge0 is in **kilobytes**. `TestCase.memoryMb` is in megabytes → multiply by 1024 when sending to Judge0.
`TestCaseResult.memoryUsedMb` is also in MB → divide the Judge0 `memory` (KB) by 1024.

---

### Decision 4: `testCaseId` in `TestCaseResultResponse` is mapped by position

`Judge0CodeExecutionService.execute()` submits test cases in the order returned by `codingQuestion.getTestCases()`. The response array from Judge0 is in the same order. The service maps `index i` → `testCases.get(i).getId()` to populate `testCaseId` in each `TestCaseResultResponse`. No secondary lookup needed.

---

### Decision 5: Auto-execution on submit is wrapped in try-catch; errors are logged, not re-thrown

`AssessmentService.submit()` already has this pattern for email sending. The same approach applies to `responseService.autoExecuteCodingResponses()`. If Judge0 is unreachable at submission time, the assessment is still marked `SUBMITTED`; the `TestCaseResult` rows for that submission will be absent or partial. The spec requires this behaviour.

---

### Decision 6: `executeCode` in `ResponseService` takes `requestingUserId` for ownership validation

The controller passes `currentUser.getId()` from `@AuthenticationPrincipal`. The service checks `assessment.getCandidate().getId().equals(requestingUserId)`, matching the pattern used in `submit()`. This enforces the spec scenario: non-owner candidate gets HTTP 403.

---

### Decision 7: `@Transactional` on `executeCode` — lazy `testCases` collection is safe within one transaction

`CodingQuestion.testCases` is `FetchType.LAZY`. Since `executeCode()` is `@Transactional`, the Hibernate session stays open and accessing `codingQuestion.getTestCases()` within the method is safe without an `@EntityGraph`. For auto-execution on submission, the same transaction covers the call.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Judge0 `wait=true` blocks the request thread for the full execution time (up to N × timeout_seconds) | Acceptable for dev/test; document that long-running test cases block the execute endpoint. Async pattern deferred to future work. |
| `CodingQuestion` testCases list is loaded lazily — out-of-transaction access could cause `LazyInitializationException` | `executeCode` is `@Transactional`, so session is open for the duration; safe |
| Memory unit mismatch (Judge0 KB vs entity MB) | Convert explicitly in `Judge0CodeExecutionService`; comment at the conversion site |
| Judge0 returns results array shorter than submissions array (partial failure) | Map by index with bounds check; pad missing results with `passed=false, errorMessage="No result returned"` |
| `autoExecuteCodingResponses` shares the same `@Transactional` as `submit()` — a Judge0 timeout could delay submission | `autoExecuteCodingResponses` is called in its own try-catch; if it throws, submission proceeds. Consider making it async in a later slice if timeouts become a problem. |

## Open Questions

- **Judge0 deployment for dev**: Docker Compose (`judge0/judge0-all-in-one`) or Judge0 CE with separate containers? Affects the `docker-compose.yml` update (out of scope for this slice — task 1.1 only sets the property; the Docker Compose update is a separate infra task).
- **`api-key` usage**: Judge0 Cloud (RapidAPI) requires `X-RapidAPI-Key` header. Self-hosted Judge0 ignores it. The implementation sends the header only when the key is non-blank — handles both cases.
