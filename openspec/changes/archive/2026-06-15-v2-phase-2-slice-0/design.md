## Context

V2 Phase 1 introduced `CodingQuestion` with inline `TestCase` rows. The assessment-taking flow (`PUT /api/assessments/{id}/responses/{questionId}`, `POST /api/assessments/{id}/submit`) handles MCQ, Text, Doc, Group, and McqPlus response types via the `ResponseRequest` sealed interface — but has no branch for `CODING`. Similarly, the Angular `assessment-session.model.ts` and `CandidateAssessmentService` have no types or methods for coding responses.

Slice 0 is a pure contract slice: it delivers only stubs and type definitions. No real persistence, no Judge0 calls, no schema migrations — those belong in Slices A–C. The goal is a compiling backend and a type-safe Angular build that Slices A, B, and C can branch off in parallel.

Four capabilities are being established by this slice:
- **`coding-response`** (owned by Slice B) — response persistence layer
- **`code-execution`** (owned by Slice B) — Judge0 integration, on-demand and on-submit execution
- **`coding-assessment-ui`** (owned by Slice C) — Angular candidate UI
- **`assessment-generation`** (owned by Slice C) — CODING support in question picker UI

## Goals / Non-Goals

**Goals:**
- `ResponseRequest` sealed interface includes `CodingResponseRequest` so the existing `PUT` endpoint compiles and accepts CODING payloads (stub → real in Slice B)
- `POST /api/assessments/{id}/responses/{questionId}/execute` stub exists and returns hardcoded `CodeExecuteResponse` with 200 (real execution in Slice B)
- Angular `ResponseRequest` union includes `CodingResponseRequest`; `TestCaseResult` and `CodeExecuteResponse` interfaces defined (used in Slice C)
- `CandidateAssessmentService.executeCode()` stub exists returning `EMPTY` (wired to real endpoint in Slice C)
- All of the above compile with no errors before any implementation slice begins

**Non-Goals:**
- No `CodingResponse` JPA entity or `coding_response` Liquibase changeset (Slice A)
- No `TestCaseResult` JPA entity or `test_case_result` table (Slice A)
- No Judge0 HTTP client or `CodeExecutionService` (Slice B)
- No Angular code editor component or "Run" button (Slice C)
- No auto-execution on submission (Slice B)

## Decisions

### Decision 1: Use `@JsonTypeInfo(Id.DEDUCTION)` — no discriminator field on `CodingResponseRequest`

The existing `ResponseRequest` uses `Id.DEDUCTION`, which infers the concrete type from the presence of unique fields rather than a `type` discriminator. `CodingResponseRequest` has only a `code: String` field. Jackson's deduction works here because no existing request type has a `code` field. No change to the deduction strategy is needed.

**Alternative considered:** Add a `type` discriminator to all response requests. Rejected — breaking change to the existing client contract for MCQ, Text, Doc, Group, and McqPlus payloads.

---

### Decision 2: Separate `POST .../execute` endpoint for on-demand execution

Code execution is triggered by a dedicated `POST /api/assessments/{id}/responses/{questionId}/execute` endpoint rather than piggybacking on the `PUT` response save. This keeps save and execute separate concerns: candidates can save code silently (auto-save debounce) and explicitly trigger execution only when they click "Run".

**Alternative considered:** Piggyback execution on the `PUT` response save (execute every save). Rejected — auto-save fires frequently; executing on every keystroke/debounce would overwhelm Judge0 and slow the save cycle.

---

### Decision 3: `CodeExecuteResponse` returned synchronously from the execute endpoint

Judge0 supports both synchronous (immediate) and asynchronous (token-poll) submission modes. The stub and the real implementation in Slice B will use Judge0's **synchronous** batch submission. This simplifies the Angular client (one HTTP call, no polling) and is acceptable for the expected test case counts (1–5 test cases per question, typical execution under 10 seconds).

**Alternative considered:** Async token-poll pattern. Rejected — adds polling complexity to Angular and an additional endpoint pair to the contract. Revisit if Judge0 rate limits or timeout windows become an issue in Slice B.

---

### Decision 4: `TestCaseResultResponse` contains `testCaseId` (UUID) not ordinal-only

Each test case result is linked back to the `TestCase` entity UUID from the question. This lets the Angular UI correlate execution results against the question's test case definitions (to show input/expected output alongside actual output) without re-fetching the question.

**Alternative considered:** Return results ordered by ordinal only, no UUID. Rejected — the UI would need to zip by array position, which breaks if Judge0 returns partial results or results out of order.

---

### Decision 5: Judge0 is the code execution provider

Judge0 (self-hosted or cloud) is the selected external execution service. Language IDs needed: Java 17 (91), Python 3 (71), C# Mono (51). The `CodeExecutionService` interface (Slice B) will wrap these IDs. Slice 0 does not need to define the interface — it only needs the response DTO shape.

**Alternative considered:** Abstract interface, provider chosen later. Deferred to Slice B where the real wiring happens.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| `Id.DEDUCTION` fails if a future request type also has a `code` field | `CodingResponseRequest` field is named `code` — sufficiently unique within the response hierarchy; document this as a constraint in `ResponseRequest.java` |
| Slice B or C adds fields to `CodingResponseRequest` that this slice didn't define | Slices must not break Slice 0 DTOs — field additions are additive and safe; removals are a breaking change requiring a new contract ticket |
| Judge0 batch API changes between Slice 0 and Slice B implementation | Slice 0 has no Judge0 dependency; Slice B owns the integration and absorbs any API surface changes |
| `CodingResponseRequest` with blank `code` accepted by stub | `@NotBlank` on `code` ensures bean validation rejects blank payloads even in stub mode |

## Open Questions

- **Judge0 deployment**: Self-hosted (Docker Compose alongside backend) or Judge0 cloud (RapidAPI)? Slice B needs to configure `code-execution.judge0.base-url`. Self-hosted avoids API key management for local dev.
- **Execution timeout**: What is the maximum wall-clock seconds allowed per Judge0 submission before we treat it as a timeout? Informs `TestCaseRequest.timeoutSeconds` upper bound and Slice B retry logic.
- **Partial execution results**: If Judge0 returns results for 3 of 5 test cases (compile error on first), should the execute endpoint return partial results or surface a top-level error? Slice B decision, but the response shape (`CodeExecuteResponse`) should accommodate it via nullable fields on `TestCaseResultResponse`.
