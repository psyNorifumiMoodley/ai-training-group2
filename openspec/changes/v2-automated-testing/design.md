## Context

v1 of the Developer Assessment Platform supports `doc_question` as a plain file-upload question type. Candidates upload a file (code, design doc, etc.) and a marker manually reviews it. There is no automated validation of code correctness. The domain model already has `TABLE_PER_CLASS` inheritance on `assessment_question`, with `doc_question` as one subtype. This design extends `doc_question` rather than introducing a new entity subtype to keep the schema change minimal and backward-compatible.

The backend is a single Spring Boot 3.x application. No separate execution service is introduced — the execution engine runs as a component within the same process, communicating with Docker via the Docker Java client library.

## Goals / Non-Goals

**Goals:**
- Extend `doc_question` (nullable fields) so existing questions are unaffected
- Define test cases at question-bank time; execute them at submission time
- Run candidate code in isolated Docker containers with enforced CPU time and memory limits
- Auto-grade coding submissions and store per-test-case execution results
- Expose results at different detail levels: count only for candidates, full detail for markers

**Non-Goals:**
- Multi-tenant Docker isolation (single-tenant deployment only in v2)
- Custom judge / diff comparison (string equality with trim is sufficient)
- Support for interactive programs or stdin-driven test cases beyond single-shot input
- Streaming execution output to the frontend during execution
- A separate execution microservice — this runs in-process

## Decisions

### D1: Extend `doc_question` rather than adding a new entity subtype

**Decision:** Add `language` (nullable enum) and `test_case` (separate child table) to `doc_question`. A doc question with `language IS NULL` is a plain file-upload question; with `language NOT NULL` it is a coding question.

**Rationale:** Introducing a new `coding_question` TABLE_PER_CLASS subtype would require a new DB table, new DTOs, new repository methods, and significant updates to all assessment generation and submission logic that currently handles `doc_question`. Extending `doc_question` with nullable fields is a backward-compatible Liquibase migration — existing rows stay valid, existing code paths continue to work, and the doc question limit rule continues to apply uniformly.

**Alternative considered:** New `coding_question` subtype. Rejected because it duplicates assessment-question handling logic and bloats the inheritance hierarchy.

---

### D2: Docker on the backend host (no separate execution microservice)

**Decision:** Use `com.github.docker-java:docker-java` to manage containers from within the Spring Boot process. The backend host must have Docker installed and the backend process must have access to the Docker socket (`/var/run/docker.sock`).

**Rationale:** Keeps the system as a single deployable unit — no inter-service networking, no separate service to maintain, no extra Docker Compose services for local dev beyond the existing PostgreSQL container. The team of 4 is building a training platform, not a production judge system; operational simplicity outweighs execution isolation purity.

**Alternative considered:** Separate Spring Boot execution microservice. Rejected because it adds operational overhead, a second service to develop and run locally, and inter-service auth complexity without commensurate benefit at this scale.

---

### D3: `test_case` as a separate child table (not JSON on `doc_question`)

**Decision:** `test_case` rows live in their own table with a FK to `doc_question`. Fields: `id` (UUID), `doc_question_id` (UUID FK), `input` (TEXT), `expected_output` (TEXT), `timeout_seconds` (INT, default 10), `memory_mb` (INT, default 256), `ordinal` (INT for display order).

**Rationale:** Separate rows support CRUD (add, edit, delete individual test cases) without deserializing and re-serializing a JSON blob. They also allow future per-test-case metadata (e.g., visibility flags, point weights) without a schema change.

**Alternative considered:** `@JdbcTypeCode(SqlTypes.JSON) List<TestCase>` on `doc_question`. Rejected because editing individual test cases requires loading and rewriting the entire list, and future extensibility is constrained.

---

### D4: Strategy pattern for language runners

**Decision:** Define an `ExecutionEngine` interface with a single method `ExecutionResult execute(ExecutionRequest request)`. Provide `DockerExecutionEngine` as the implementation, which delegates to a `LanguageRunner` strategy (`JavaRunner`, `PythonRunner`, `CsharpRunner`) selected by the `Language` enum on the request.

**Rationale:** Each language has a different container lifecycle (Java requires compilation; Python does not; C# uses `dotnet run`). The strategy pattern keeps each runner independently testable and allows new language support by implementing one interface. `DockerExecutionEngine` owns container lifecycle (create, start, exec, stop, remove) while runners provide only the container image and command sequence.

---

### D5: Per-test-case containers (not one container per submission)

**Decision:** Spin up a fresh container per test case, run it, capture output, then remove it.

**Rationale:** A fresh container per test case prevents state leakage between test cases (e.g., a test that modifies a file cannot affect the next test). Container startup overhead is acceptable for a training platform — assessments are not graded in real time during the taking experience; grading happens post-submission.

**Alternative considered:** Reuse one container per submission with test cases run sequentially. Rejected because state leakage between tests could produce non-deterministic results.

---

### D6: `execution_result` table for per-test-case outcomes

**Decision:** Each test case execution produces one `execution_result` row: `id`, `submission_id`, `test_case_id`, `doc_question_id`, `passed` (boolean), `actual_output` (TEXT), `stderr` (TEXT), `execution_time_ms` (INT), `error_type` (enum: NONE, TIMEOUT, MEMORY, COMPILE_ERROR, RUNTIME_ERROR).

**Rationale:** Persisting results allows the marker review UI to load them on demand without re-executing code. Results are immutable after grading — stored once at submission time. `submission_id` + `test_case_id` is a unique pair (UNIQUE constraint).

---

### D7: Candidate sees pass/fail count; marker sees full detail

**Decision:** `GET /api/assessments/{id}/coding-results` (candidate JWT required, role = CANDIDATE) returns `{ docQuestionId, passed, total }` per coding question. `GET /api/assessments/{id}/coding-results/detail` (marker/admin JWT required) returns the full `execution_result` rows including input, expected output, actual output, and stderr.

**Rationale:** Hiding test case inputs and expected outputs from candidates prevents hardcoding. Markers need full detail to understand why a test failed and to make informed marking decisions on partial credit.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Docker socket access is a security concern — a compromised backend could control the host Docker daemon | Acceptable for a training platform deployment; document the requirement; scope to internal networks only |
| Slow container startup increases submission processing time | Run test cases for a single submission concurrently using `CompletableFuture`; document expected grading latency (seconds, not milliseconds) |
| Container image pull on first execution adds latency | Pre-pull images on application startup via `DockerExecutionEngine.warmUp()` called from `ApplicationRunner` |
| Runaway containers if kill/timeout logic fails | Always set `HostConfig.withAutoRemove(true)` and keep a fallback `docker kill` via scheduled cleanup of containers older than N minutes |
| Compilation errors in Java/C# leave no stdout — `actual_output` is empty | Map to `error_type = COMPILE_ERROR`; store stderr for marker review; `passed = false` |
| `doc_question` limit rule: coding questions count toward the limit | No code change needed — the existing limit check operates on `DocQuestion` rows; extending with nullable fields does not change how the check counts |

## Migration Plan

1. Liquibase changesets (Phase 6): add `language` column to `doc_question`; create `test_case` table; create `execution_result` table (Phase 7 Slice 0)
2. Existing `doc_question` rows get `language = NULL` automatically — no data migration needed
3. Add `docker-java` dependency to `pom.xml` (Phase 7 Slice A)
4. Pre-pull Docker images on startup — first boot with the new version may take 1–2 minutes per language image
5. No rollback required for the Liquibase changes (addColumn and createTable are non-destructive); rollback blocks provided for completeness

## Open Questions

- Should partial credit be awarded for coding questions (e.g., 3/5 test cases passed = 60%)? Decision deferred to Phase 8 — the score field on `submission` can store a float; the grading service records the ratio.
- Should Docker image pre-pulling be mandatory on startup or lazy (pull on first use)? Recommended: eager pre-pull via `ApplicationRunner` to avoid first-submission latency spikes.
