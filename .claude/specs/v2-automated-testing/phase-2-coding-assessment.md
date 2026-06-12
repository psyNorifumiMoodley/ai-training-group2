# Phase 2 — Coding Assessment Engine

> **Supersedes:** `phase-2-execution-engine.md` and `phase-3-grading-and-results.md`
> **Delivery:** Slice 0 merges first; Slices A, B, and C run in **parallel** (one dev each).
> **Dependency:** Phase 1 fully merged (`CodingQuestion`, `TestCase`, `CodingQuestionLanguage` must exist).
> **Jira Epic:** ATG-55

### Key Design Decisions
- Code execution is delegated to **Judge0 CE** (Community Edition), running as a sidecar service in Docker Compose — the Spring Boot app never spawns containers itself
- Judge0 handles sandboxing, resource limits, and all language runtimes; no per-language runner code is needed in this codebase
- The Spring Boot app calls the Judge0 REST API via Spring's `RestClient`; requests use `?wait=true` for synchronous results and `?base64_encoded=true` to safely transport source code and I/O
- Judge0 language IDs are externalised to `application.properties` so they can be updated without a code change
- Output comparison (pass/fail per test case) happens in `CodeExecutionServiceImpl`, not inside Judge0 — `expected_output` is never sent to Judge0
- Candidates click **"Run Test Cases"** during an assessment to see a live pass/fail count; the result is stored in `ExecutionResultEntity` with overwrite semantics (each run replaces the previous)
- At submission, the candidate's source code is saved as a `CodingResponse`; `GradingServiceImpl` re-runs all test cases automatically via `CodeExecutionService`, overwriting the candidate's last manual run result
- `ExecutionResultEntity` stores aggregate pass/fail per `(assessment_id, coding_question_id)`; drop the unique constraint to enable run history in future
- Markers see the candidate's submitted source code and aggregate pass/fail count — no per-test-case breakdown
- Angular services are **fully implemented in Slice 0** (they are simple HTTP wrappers); Slices B and C only touch component files — eliminating any risk of frontend merge conflicts

---

## Slice 0: Contracts, Schema & Angular Services *(one dev, merge first)*

### Agent Brief
Create every contract needed to unblock Slices A, B, and C working in parallel. This includes all Java interfaces, DTOs, domain objects, JPA entities, Liquibase changesets, repositories, and stub backend endpoints. It also includes Angular model interfaces and Angular services — **implemented as real HTTP calls, not stubs** — so that Slices B and C can write components without ever touching a service file.

> All files in this slice are created here and are not modified by any other slice.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    ExecutionEngine.java                   ← interface
    CodeExecutionService.java              ← interface
    GradingService.java                    ← interface
    CodingResultService.java               ← interface
  domain/
    ExecutionRequest.java                  ← record
    ExecutionResult.java                   ← record
    ErrorType.java                         ← enum
    NotFoundException.java                 ← extends RuntimeException
    UnsupportedLanguageException.java      ← extends RuntimeException
    ExecutionResultEntity.java             ← JPA entity
    CodingResponse.java                    ← JPA entity
  repository/
    ExecutionResultRepository.java
    CodingResponseRepository.java
  dto/
    CodeExecutionRequest.java              ← record
    CodeExecutionResponse.java             ← record
    CodingResponseRequest.java             ← record
    CodingResultResponse.java              ← record
  controller/
    CodeExecutionController.java           ← stub
src/main/resources/db/changelog/changesets/
  2026-06-12-001-create-execution-result-table.xml
  2026-06-12-002-create-coding-response-table.xml
```

**`AssessmentController`** — one stub endpoint added (existing file, modified once here, not touched again by A/B/C):
```
GET /api/assessments/{id}/coding-results  → 200 [] (hardcoded)
```

**Frontend**
```
src/app/
  core/
    models/
      coding-result.model.ts             ← CodingResultResponse interface
    services/
      coding-result.service.ts           ← real HTTP implementation
  features/
    assessment/
      models/
        code-execution.model.ts          ← CodeExecutionResponse interface
      services/
        code-execution.service.ts        ← real HTTP implementation
```

### Entities

**`ExecutionResultEntity`**
- Table: `execution_result`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assessment_id", nullable = false) Assessment assessment`
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coding_question_id", nullable = false) CodingQuestion codingQuestion`
  - `@Column(nullable = false) int testCasesPassed`
  - `@Column(nullable = false) int testCasesTotal`
  - `@Column(columnDefinition = "TEXT") String stderr`
  - `@Column(nullable = false) long executionTimeMs`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false) ErrorType errorType`

> **Design note:** The unique constraint on `(assessment_id, coding_question_id)` enforces overwrite semantics — each "Run" and each grading pass replaces the previous result. To enable run history in future, drop this constraint and add a `runNumber` or `createdAt` ordering column.

**`CodingResponse`** — extends `Response` (JOINED inheritance)
- Table: `coding_response`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@Column(columnDefinition = "TEXT", nullable = false) String sourceCode`
- Inherits from `Response`: `assessment`, `question`, `score`

**`NotFoundException`** — `extends RuntimeException`; mapped to `404 Not Found` by `GlobalExceptionHandler`. Follow the same pattern as `UnprocessableException`.

**`UnsupportedLanguageException`** — `extends RuntimeException`; mapped to `422 Unprocessable Entity` by `GlobalExceptionHandler`.

### Liquibase Changesets

**`2026-06-12-001-create-execution-result-table.xml`**
```xml
<changeSet id="2026-06-12-001-create-execution-result-table" author="developer">
  <createTable tableName="execution_result">
    <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
    <column name="assessment_id" type="UUID"><constraints nullable="false"/></column>
    <column name="coding_question_id" type="UUID"><constraints nullable="false"/></column>
    <column name="test_cases_passed" type="INTEGER"><constraints nullable="false"/></column>
    <column name="test_cases_total" type="INTEGER"><constraints nullable="false"/></column>
    <column name="stderr" type="TEXT"/>
    <column name="execution_time_ms" type="BIGINT"><constraints nullable="false"/></column>
    <column name="error_type" type="VARCHAR(20)"><constraints nullable="false"/></column>
    <column name="created_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
    <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
  </createTable>
  <addForeignKeyConstraint
    baseTableName="execution_result" baseColumnNames="assessment_id"
    referencedTableName="assessment" referencedColumnNames="id"
    constraintName="fk_execution_result_assessment"/>
  <addForeignKeyConstraint
    baseTableName="execution_result" baseColumnNames="coding_question_id"
    referencedTableName="coding_question" referencedColumnNames="id"
    constraintName="fk_execution_result_coding_question"/>
  <addUniqueConstraint
    tableName="execution_result" columnNames="assessment_id, coding_question_id"
    constraintName="uq_execution_result_assessment_question"/>
  <rollback><dropTable tableName="execution_result"/></rollback>
</changeSet>
```

**`2026-06-12-002-create-coding-response-table.xml`**
```xml
<changeSet id="2026-06-12-002-create-coding-response-table" author="developer">
  <createTable tableName="coding_response">
    <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
    <column name="source_code" type="TEXT"><constraints nullable="false"/></column>
  </createTable>
  <addForeignKeyConstraint
    baseTableName="coding_response" baseColumnNames="id"
    referencedTableName="response" referencedColumnNames="id"
    constraintName="fk_coding_response_response"/>
  <rollback><dropTable tableName="coding_response"/></rollback>
</changeSet>
```

### Repositories

**`ExecutionResultRepository extends JpaRepository<ExecutionResultEntity, UUID>`**
```java
Optional<ExecutionResultEntity> findByAssessmentIdAndCodingQuestionId(UUID assessmentId, UUID codingQuestionId);
List<ExecutionResultEntity> findByAssessmentId(UUID assessmentId);
```

**`CodingResponseRepository extends JpaRepository<CodingResponse, UUID>`**
```java
Optional<CodingResponse> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);
List<CodingResponse> findByAssessmentId(UUID assessmentId);
```

### Value Objects & Interfaces

**`ErrorType`** — enum: `NONE, TIMEOUT, MEMORY, COMPILE_ERROR, RUNTIME_ERROR`

**`ExecutionRequest`** — record
```java
public record ExecutionRequest(
    CodingQuestionLanguage language,
    String sourceCode,
    String input,
    int timeoutSeconds,
    int memoryMb
) {}
```

**`ExecutionResult`** — record
```java
public record ExecutionResult(
    String actualOutput,
    String stderr,
    long executionTimeMs,
    ErrorType errorType
) {}
```

**`ExecutionEngine`** — interface
```java
public interface ExecutionEngine {
    ExecutionResult execute(ExecutionRequest request);
}
```

**`CodeExecutionService`** — interface
```java
CodeExecutionResponse runCode(UUID assessmentId, UUID questionId, String sourceCode);
```

**`GradingService`** — interface
```java
void gradeAssessment(UUID assessmentId);
```

**`CodingResultService`** — interface
```java
List<CodingResultResponse> getCodingResults(UUID assessmentId, UUID requestingUserId, Role requestingUserRole);
```

**`CodeExecutionRequest`** — record
```java
public record CodeExecutionRequest(@NotBlank String sourceCode) {}
```

**`CodeExecutionResponse`** — record
```java
public record CodeExecutionResponse(int testCasesPassed, int testCasesTotal, ErrorType errorType) {}
```

**`CodingResponseRequest`** — record
```java
public record CodingResponseRequest(@NotNull UUID questionId, @NotBlank String sourceCode) {}
```

**`CodingResultResponse`** — record
```java
public record CodingResultResponse(UUID codingQuestionId, String sourceCode, int passed, int total) {}
```

### Services
None in this slice.

### Controllers

**`CodeExecutionController`** — stub
```
POST /api/assessments/{assessmentId}/questions/{questionId}/execute
  → 200 OK with hardcoded CodeExecutionResponse { testCasesPassed: 3, testCasesTotal: 5, errorType: NONE }
  @PreAuthorize("hasRole('CANDIDATE')")
```

**`AssessmentController`** — add stub endpoint
```
GET /api/assessments/{id}/coding-results
  → 200 [] (hardcoded empty list)
  @PreAuthorize("hasAnyRole('CANDIDATE', 'MARKER', 'ADMIN')")
```

### Frontend Type Definitions

```typescript
// core/models/coding-result.model.ts
export interface CodingResultResponse {
  codingQuestionId: string;
  sourceCode: string | null;
  passed: number;
  total: number;
}
```

```typescript
// features/assessment/models/code-execution.model.ts
export interface CodeExecutionResponse {
  testCasesPassed: number;
  testCasesTotal: number;
  errorType: 'NONE' | 'TIMEOUT' | 'COMPILE_ERROR' | 'RUNTIME_ERROR' | 'MEMORY';
}
```

### Frontend Services

Both services are implemented as real HTTP wrappers here — no stub phase needed, since they are simple one-liners. Slices B and C inject and use these without touching the service files.

**`CodingResultService`**
```typescript
getCodingResults(assessmentId: string): Observable<CodingResultResponse[]> {
  return this.http.get<CodingResultResponse[]>(`/api/assessments/${assessmentId}/coding-results`);
}
```

**`CodeExecutionService`**
```typescript
runTestCases(assessmentId: string, questionId: string, sourceCode: string): Observable<CodeExecutionResponse> {
  return this.http.post<CodeExecutionResponse>(
    `/api/assessments/${assessmentId}/questions/${questionId}/execute`,
    { sourceCode }
  );
}
```

### Frontend Components
None.

### Route Additions
None.

### Testing
- `execution_result` and `coding_response` tables exist in Testcontainers DB and pass `ddl-auto=validate`
- All Java interfaces, records, and enums compile without errors
- Stub endpoints return correct HTTP status for valid role JWTs
- Angular services and model interfaces compile with no TypeScript errors

### Done When
- All contracts compile
- Both Liquibase changesets apply cleanly
- Stub endpoints respond correctly
- Angular services compile
- **Merge to `main` before Slices A, B, or C begin**

---

## Slice A: Backend Implementation *(parallel with B and C)*

### Agent Brief
Implement the full backend: Judge0 execution engine, code execution service, grading service, and coding result service. Extend the submission flow to save `CodingResponse` entities and trigger async grading. Replace both stub endpoints with real implementations.

### File Ownership
> ⚠️ Files exclusively owned by Slice A — **Slices B and C must not modify these.**

**New files:**
```
src/main/java/com/psybergate/dap/
  service/
    execution/
      Judge0ExecutionEngine.java
    impl/
      CodeExecutionServiceImpl.java
      GradingServiceImpl.java
      CodingResultServiceImpl.java
  config/
    Judge0Config.java
  dto/
    Judge0SubmissionResponse.java          ← internal, not exposed via API
src/test/java/com/psybergate/dap/
  service/execution/
    Judge0ExecutionEngineIntegrationTest.java
  service/
    GradingServiceIntegrationTest.java
  controller/
    CodeExecutionControllerIntegrationTest.java
    CodingResultEndpointTest.java
```

**Modified files:**
```
controller/CodeExecutionController.java    ← replace stub with real implementation
controller/AssessmentController.java       ← replace coding-results stub with real service call
service/AssessmentService.java             ← extend submit() to save CodingResponse + call gradeAssessment()
docker-compose.yml                         ← add Judge0 CE services and Redis
application.properties                     ← add Judge0 config
```

### `application.properties` additions
```properties
execution.judge0.base-url=http://judge0:2358
execution.judge0.language-ids.java=62
execution.judge0.language-ids.python=71
execution.judge0.language-ids.csharp=51
```

> **Note on language IDs:** Verify against the running Judge0 instance via `GET /languages` and update `application.properties` — no code change required.

### `docker-compose.yml` additions
```yaml
judge0:
  image: judge0/judge0:latest
  depends_on: [postgres, redis]
  environment:
    REDIS_HOST: redis
    POSTGRES_HOST: postgres
    POSTGRES_DB: judge0
    POSTGRES_USER: dap
    POSTGRES_PASSWORD: dap
  ports:
    - "2358:2358"

judge0-workers:
  image: judge0/judge0:latest
  command: ["./scripts/workers"]
  depends_on: [judge0, redis]
  environment:
    REDIS_HOST: redis
    POSTGRES_HOST: postgres
    POSTGRES_DB: judge0
    POSTGRES_USER: dap
    POSTGRES_PASSWORD: dap

redis:
  image: redis:7-alpine
```

### Entities
None new.

### Liquibase Changesets
None.

### Repositories
None new.

### Services

**`Judge0Config`** (`@Configuration`)
```java
@Bean
public RestClient judge0RestClient(@Value("${execution.judge0.base-url}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
}
```

**`Judge0SubmissionResponse`** — internal record for deserialising the Judge0 API response
```java
public record Judge0SubmissionResponse(
    String stdout,         // base64-encoded, nullable
    String stderr,         // base64-encoded, nullable
    String compileOutput,  // base64-encoded, nullable; JSON field: "compile_output"
    Judge0Status status,
    String time,           // execution seconds as string, e.g. "0.123"
    Long memory            // KB, nullable
) {
    public record Judge0Status(int id, String description) {}
}
```

**`Judge0ExecutionEngine implements ExecutionEngine`** (`@Component`)
- Constructor-inject `RestClient judge0RestClient` and the language ID map from `@Value("${execution.judge0.language-ids.*}")`
- On construction, build `Map<CodingQuestionLanguage, Integer>` from the properties; throw `IllegalStateException` at startup if any `CodingQuestionLanguage` value is unmapped
- `execute(ExecutionRequest request)`:
  1. Look up `languageId` — throw `UnsupportedLanguageException` if absent
  2. POST to `/submissions?wait=true&base64_encoded=true` with body:
     - `language_id`: mapped integer
     - `source_code`: Base64-encoded `request.sourceCode()`
     - `stdin`: Base64-encoded `request.input()` (empty string if null)
     - `cpu_time_limit`: `request.timeoutSeconds()`
     - `memory_limit`: `request.memoryMb() * 1024L` (Judge0 uses KB)
  3. Decode `stdout` from Base64 — this is `actualOutput`
  4. Decode `stderr`; for status 6 (`COMPILE_ERROR`), use `compileOutput` instead
  5. Map `status.id()` to `ErrorType`: `3 → NONE`; `5 → TIMEOUT`; `6 → COMPILE_ERROR`; `7–12 → RUNTIME_ERROR`; any other `→ RUNTIME_ERROR`
  6. Parse `time` string to milliseconds for `executionTimeMs`
  7. Return `new ExecutionResult(actualOutput, stderr, executionTimeMs, errorType)`

**`CodeExecutionServiceImpl implements CodeExecutionService`**
1. Load `Assessment` by `assessmentId` — throw `UnprocessableException` if status is not `IN_PROGRESS`
2. Verify the question (by `questionId`) belongs to that assessment — throw `NotFoundException` if not
3. Load the `CodingQuestion` and its `TestCase` list
4. For each test case in order: call `executionEngine.execute(new ExecutionRequest(question.language(), sourceCode, testCase.input(), testCase.timeoutSeconds(), testCase.memoryMb()))`
5. Aggregate: count passes where `result.actualOutput().trim().equals(testCase.expectedOutput().trim())`; track total `executionTimeMs`; capture `stderr` and `errorType` from first non-`NONE` result; stop early on `COMPILE_ERROR`
6. Upsert `ExecutionResultEntity` by `(assessmentId, codingQuestionId)` — update all fields if present, create if absent
7. Return `CodeExecutionResponse`

**`GradingServiceImpl implements GradingService`**
- `@Async` — does not block the HTTP response
- `gradeAssessment(UUID assessmentId)`:
  1. Load `Assessment` — throw `NotFoundException` if absent
  2. For each `CodingQuestion` in `assessment.getQuestions()`:
     a. Load `CodingResponse` by `(assessmentId, questionId)` — skip this question if absent (candidate submitted without code)
     b. Call `codeExecutionService.runCode(assessmentId, questionId, codingResponse.getSourceCode())`; `runCode` handles execution and upserts `ExecutionResultEntity` internally

**`CodingResultServiceImpl implements CodingResultService`**
- `getCodingResults(UUID assessmentId, UUID requestingUserId, Role requestingUserRole)`:
  1. Load `Assessment` — throw `NotFoundException` if absent
  2. If `CANDIDATE`: verify `assessment.candidate.user.id == requestingUserId` — throw `UnauthorizedException` if not
  3. Load all `ExecutionResultEntity` rows by `assessmentId`
  4. For each, load matching `CodingResponse` by `(assessmentId, codingQuestionId)` to get `sourceCode`
  5. Candidates receive `sourceCode = null`; `MARKER` and `ADMIN` receive the actual source code
- `@Transactional(readOnly = true)`

**`AssessmentService`** — extend `submit()`:
- Add `List<CodingResponseRequest> codingResponses` field to the submission request DTO (optional; empty list if no coding questions)
- Before setting status to `SUBMITTED`: upsert a `CodingResponse` for each `CodingResponseRequest`
- After persisting `SUBMITTED` status: call `gradingService.gradeAssessment(assessment.getId())`

### Controllers

**`CodeExecutionController`** — replace stub
- `POST /api/assessments/{assessmentId}/questions/{questionId}/execute`
  - Auth: `CANDIDATE` — JWT subject must match the assessment's candidate
  - Request body: `@Valid CodeExecutionRequest`
  - Delegates to `codeExecutionService.runCode(...)`
  - Returns `200 OK` with `CodeExecutionResponse`

**`AssessmentController`** — replace coding-results stub
```
GET /api/assessments/{id}/coding-results
  → codingResultService.getCodingResults(assessmentId, currentUserId, currentUserRole)
  @PreAuthorize("hasAnyRole('CANDIDATE', 'MARKER', 'ADMIN')")
```

### Frontend Type Definitions
None.

### Frontend Services
None.

### Frontend Components
None.

### Route Additions
None.

### Testing

**`Judge0ExecutionEngineIntegrationTest`** (`@SpringBootTest`, requires Judge0 via Docker Compose)
- Python `print("hello world")` with `input = ""` → `actualOutput = "hello world\n"`, `errorType = NONE`
- Python `while True: pass` with `timeoutSeconds = 1` → `errorType = TIMEOUT`
- Java missing semicolon → `errorType = COMPILE_ERROR`, `stderr` non-empty

**`CodeExecutionControllerIntegrationTest`** (`@SpringBootTest`, Testcontainers + Judge0)
- Valid source matching expected output → `200 OK`, `testCasesPassed = testCasesTotal`, `errorType = NONE`
- Source with compile error → `200 OK`, `testCasesPassed = 0`, `errorType = COMPILE_ERROR`
- Non-`IN_PROGRESS` assessment → `422 Unprocessable Entity`
- Wrong candidate JWT → `403 Forbidden`

**`GradingServiceIntegrationTest`** (`@SpringBootTest`, Testcontainers + Judge0)
- Assessment with one Java coding question (2 test cases, 1 correct output) → `ExecutionResultEntity` has `testCasesPassed = 1`, `testCasesTotal = 2`
- Re-grading same assessment → `ExecutionResultEntity` overwritten, not duplicated
- Coding question with no `CodingResponse` → question skipped, no error thrown
- Assessment with no coding questions → no execution calls, no `ExecutionResultEntity` rows created

**`CodingResultEndpointTest`** (`@SpringBootTest`, Testcontainers)
- Candidate fetches own results → `200` with `sourceCode: null`
- Candidate fetches another candidate's results → `401`
- Marker fetches → `200` with `sourceCode` populated
- No graded questions → `200` with empty list

### Done When
- Judge0 integration tests pass with Judge0 available
- Application starts without error when Judge0 is unreachable
- `POST .../execute` and `GET .../coding-results` return real data
- `CodingResponse` saved at submission; grading fires in the background
- Language IDs configurable via `application.properties` with no code change required

---

## Slice B: Candidate Frontend *(parallel with A and C)*

### Agent Brief
Build `CodingQuestionComponent` (code editor + "Run Test Cases" button + live result panel). Update the candidate result view to show a pass/fail badge per coding question. Extend the submission payload to include source code for each coding question.

### File Ownership
> ⚠️ Files exclusively owned by Slice B — **Slices A and C must not modify these.**

**New files:**
```
src/app/features/assessment/
  components/
    coding-question/
      coding-question.component.ts
      coding-question.component.html
```

**Modified files:**
```
src/app/features/assessment/
  components/
    candidate-result/
      candidate-result.component.ts      ← add coding pass/fail badge
      candidate-result.component.html
```
- Wherever the assessment submission HTTP call is built: add `codingResponses` to the request payload

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
None — `CodeExecutionService` and `CodingResultService` are already fully implemented in Slice 0. Inject and use them; **do not modify the service files.**

### Controllers
None.

### Frontend Type Definitions
No changes.

### Frontend Components

**`CodingQuestionComponent`** — standalone, `OnPush`
- Inputs: `assessmentId = input<string>()`, `questionId = input<string>()`, `language = input<string>()`
- Contains a `<textarea>` bound to a `sourceCode` reactive form control
- "Run Test Cases" button: disabled while a request is in flight; calls `codeExecutionService.runTestCases(assessmentId(), questionId(), sourceCode)` on click
- Result panel (hidden until first run):
  - Badge: `"X / Y test cases passed"` — green if all pass, amber if partial, red if none
  - `COMPILE_ERROR` → show `"Compile error — check your syntax"` above the badge
  - `TIMEOUT` → show `"One or more test cases timed out"`
  - `RUNTIME_ERROR` → show `"One or more test cases threw a runtime error"`
- Uses `takeUntilDestroyed()` for subscription cleanup

**`CandidateResultComponent`** — updates
- Calls `codingResultService.getCodingResults(assessmentId)` via the `async` pipe
- For each question where `type === 'CODING'`: look up `CodingResultResponse` by `codingQuestionId` and display badge: `"X / Y passed"`
- Badge colours: all pass → green; partial → amber; none → red; `total === 0` → grey
- Badge absent for MCQ, text, and group questions

**Submission payload extension** — wherever the assessment submit call is built:
- Collect `sourceCode` from each `CodingQuestionComponent` on the page
- Include `codingResponses: [{ questionId, sourceCode }]` in the submission request body

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- `CodingQuestionComponent`: "Run Test Cases" button calls service with correct args; result panel hidden before first run; green badge on all-pass; red badge on zero-pass; button disabled while request is in flight
- `CandidateResultComponent`: badge renders `"3 / 5 passed"` from mocked service response; correct colours for all states; badge absent for non-coding questions

### Done When
- Candidate can type code, click "Run Test Cases", and see the live pass/fail result
- Candidate result view shows a badge for each coding question
- Submission payload includes `codingResponses`
- All unit tests pass

---

## Slice C: Marker Frontend *(parallel with A and B)*

### Agent Brief
Update the marker submission review component to display the candidate's submitted source code and aggregate pass/fail badge for each coding question.

### File Ownership
> ⚠️ Files exclusively owned by Slice C — **Slices A and B must not modify these.**

**Modified files:**
```
src/app/features/marking/
  components/
    submission-review/
      submission-review.component.ts     ← add source code block + badge
      submission-review.component.html
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
None — `CodingResultService` is already fully implemented in Slice 0. Inject and use it; **do not modify the service file.**

### Controllers
None.

### Frontend Type Definitions
No changes.

### Frontend Components

**`SubmissionReviewComponent`** — updates
- Calls `codingResultService.getCodingResults(assessmentId)` via the `async` pipe
- For each coding question: look up the matching `CodingResultResponse` by `codingQuestionId`
- Display `sourceCode` in a read-only, syntax-highlighted code block
- Display aggregate pass/fail badge below the code block (same colour rules as candidate view)
- If no `CodingResultResponse` exists (candidate never submitted code): show a `"No execution result"` placeholder instead of the badge

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Source code block renders `sourceCode` from mocked `CodingResultResponse`
- Badge renders `"4 / 5 passed"` with amber styling when `passed = 4, total = 5`
- Green badge when `passed === total`; red when `passed === 0 && total > 0`
- `"No execution result"` placeholder renders when no matching result exists

### Done When
- Marker sees submitted source code and aggregate pass/fail badge for each coding question
- `"No execution result"` placeholder renders when candidate never submitted code
- All unit tests pass
