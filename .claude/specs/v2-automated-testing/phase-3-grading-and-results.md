# Phase 3 — Grading & Results

> **Epic:** Phase 3 — Grading & Results
> **Delivery:** Slice 0 merges first; Slices A–D run in parallel (one dev each).
> **Dependency:** Phase 1 (`CodingQuestion`, `TestCase`) and Phase 2 (`ExecutionEngine`, `ExecutionResultEntity`) fully merged.
> **Jira Epic:** ATG-55

### Key Design Decisions
- Grading is triggered **asynchronously** from `AssessmentService.submit()` using `@Async` — the candidate's HTTP response is not blocked
- All test cases for a single submission are executed **concurrently** via `CompletableFuture.supplyAsync()` with a bounded thread pool
- Output comparison uses **trimmed string equality** — `actualOutput.trim().equals(expectedOutput.trim())`
- Candidates see **pass/fail count only** (`{ codingQuestionId, passed, total }`) — test case inputs and expected outputs are never exposed to candidates
- Markers see **full per-test detail** including `input`, `expectedOutput`, `actualOutput`, `stderr`, `executionTimeMs`, and `errorType`

---

## Slice 0: Grading Interface & Result API Contracts *(merge first)*

### Agent Brief
Define the `GradingService` interface. Add result DTOs and stub the two result endpoints in `AssessmentController`. Add stub methods in a new Angular `CodingResultService`. No grading logic — hardcoded responses only.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    GradingService.java                  ← interface
  dto/
    CodingResultResponse.java            ← record { UUID codingQuestionId, int passed, int total }
    CodingResultDetailResponse.java      ← record { UUID testCaseId, UUID codingQuestionId, boolean passed, String input, String expectedOutput, String actualOutput, String stderr, long executionTimeMs, String errorType }
```

**Frontend**
```
src/app/
  core/
    services/
      coding-result.service.ts           ← stub
    models/
      coding-result.model.ts
```

### Entities
None in this slice.

### Liquibase Changesets
None.

### Repositories
None in this slice.

### Value Objects

**`GradingService`** — interface
```java
public interface GradingService {
    void gradeSubmission(UUID submissionId);
}
```

### Services
None in this slice.

### Controllers

**`AssessmentController`** additions — stubs
```
GET /api/assessments/{id}/coding-results         → 200 [] (hardcoded)
  @PreAuthorize("hasRole('CANDIDATE')")

GET /api/assessments/{id}/coding-results/detail  → 200 [] (hardcoded)
  @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
```

### Frontend Type Definitions
```typescript
// core/models/coding-result.model.ts
export interface CodingResultResponse {
  codingQuestionId: string;
  passed: number;
  total: number;
}

export interface CodingResultDetailResponse {
  testCaseId: string;
  codingQuestionId: string;
  passed: boolean;
  input: string;
  expectedOutput: string;
  actualOutput: string;
  stderr: string;
  executionTimeMs: number;
  errorType: string;
}
```

### Frontend Services

**`CodingResultService`** — stubs returning `EMPTY`
```typescript
getCodingResults(assessmentId: string): Observable<CodingResultResponse[]>
getCodingResultDetail(assessmentId: string): Observable<CodingResultDetailResponse[]>
```

### Frontend Components
None.

### Route Additions
None.

### Testing
- Stubs return correct HTTP status codes with correct role JWT
- CANDIDATE calling detail endpoint → 403
- MARKER/ADMIN calling candidate endpoint → permitted (MARKER can view candidate results too)
- Angular `CodingResultService` compiles with correct type signatures

### Done When
- All stubs compile and return hardcoded responses
- Angular `CodingResultService` and `coding-result.model.ts` compile with no TypeScript errors
- Merge Slice 0 to `main` before any other slice begins

---

## Slice A: GradingService Implementation

### Agent Brief
Implement `GradingServiceImpl`. On assessment submission, asynchronously grade all `CodingQuestion` entries in the assessment: execute each test case concurrently, compare output, and persist one `ExecutionResultEntity` per test case. Wire the async call into `AssessmentService.submit()`.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    GradingServiceImpl.java
src/main/resources/
  application.properties              ← add grading thread pool config
src/test/java/com/psybergate/dap/service/
  GradingServiceIntegrationTest.java
```

### Entities
None new.

### Liquibase Changesets
None.

### Repositories
No new repositories. Uses `SubmissionRepository`, `TestCaseRepository`, `ExecutionResultRepository`, `AssessmentRepository` (all from prior phases).

### Services

**`GradingServiceImpl implements GradingService`**
- Constructor-inject: `ExecutionEngine`, `SubmissionRepository`, `TestCaseRepository`, `ExecutionResultRepository`, `AssessmentRepository`
- `gradeSubmission(UUID submissionId)`:
  1. Load `Submission` by `submissionId` (throw if not found)
  2. For each `CodingQuestion` in `submission.getAssessment().getQuestions()`:
     - Load all `TestCase` rows for the question
     - Submit each to `CompletableFuture.supplyAsync(() -> executeAndPersist(submission, testCase), gradingExecutor)`
  3. `CompletableFuture.allOf(futures).join()` — wait for all to complete
  4. Each `executeAndPersist`: call `executionEngine.execute(request)`; compare `actualOutput.trim()` to `testCase.getExpectedOutput().trim()`; build and save `ExecutionResultEntity`
- `@Async("gradingExecutor")` — method itself is async from the caller's perspective
- DB writes inside `executeAndPersist` use a separate `@Transactional(propagation = REQUIRES_NEW)` — execution engine calls are **outside** any transaction boundary

**`application.properties`** additions
```properties
grading.thread-pool.core-size=4
grading.thread-pool.max-size=8
grading.thread-pool.queue-capacity=50
```
Define a `ThreadPoolTaskExecutor` bean named `gradingExecutor` in a `@Configuration` class using these values.

**`AssessmentService`** — wire async grading
```java
// In submit(): after setting status = SUBMITTED and persisting:
gradingService.gradeSubmission(submission.getId());
// @Async on GradingServiceImpl.gradeSubmission() ensures this does not block the HTTP response
```

### Controllers
None.

### Frontend Type Definitions
None.

### Frontend Services
None.

### Frontend Components
None.

### Route Additions
None.

### Testing

**`GradingServiceIntegrationTest`** (`@SpringBootTest`, Testcontainers + requires Docker)
- Submit assessment with one Python coding question (2 test cases, 1 correct) → 2 `execution_result` rows created; `passed = true` for the matching output, `passed = false` for the non-matching
- Submitting the same assessment again does not create duplicate rows — UNIQUE constraint on `(submission_id, test_case_id)` is enforced
- Assessment with no coding questions → no `execution_result` rows created and no execution engine calls made

### Done When
- `gradeSubmission()` executes all test cases for a submission and persists results
- Submission HTTP response returns immediately; grading runs in the background
- Duplicate execution results are prevented by the UNIQUE constraint

---

## Slice B: Result Query Endpoints

### Agent Brief
Implement `ExecutionResultService` — the two query methods for coding results. Wire into `AssessmentController`. Enforce ownership on the candidate endpoint (a candidate may only fetch their own assessment's results).

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    ExecutionResultService.java
src/test/java/com/psybergate/dap/controller/
  CodingResultEndpointTest.java
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
No new repositories.

### Services

**`ExecutionResultService`**
```java
List<CodingResultResponse> getCodingResults(UUID assessmentId, UUID requestingUserId);
// Verify the candidate owns the assessment (assessment.candidate.user.id == requestingUserId)
// If not, throw AccessDeniedException → 403
// Load all execution_result rows for the assessment's submission grouped by coding_question_id
// Return List<CodingResultResponse { codingQuestionId, passed (count of true), total (count of all) }>

List<CodingResultDetailResponse> getCodingResultDetail(UUID assessmentId);
// Load all execution_result rows for the assessment's submission
// Return full detail including input and expectedOutput (loaded from test_case via JOIN)
```

Both methods `@Transactional(readOnly = true)`.

### Controllers

**`AssessmentController`** — replace stubs with real service calls
```
GET /api/assessments/{id}/coding-results
  → executionResultService.getCodingResults(assessmentId, currentUserId)
  @PreAuthorize("hasRole('CANDIDATE')")

GET /api/assessments/{id}/coding-results/detail
  → executionResultService.getCodingResultDetail(assessmentId)
  @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
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

**`CodingResultEndpointTest`** (`@SpringBootTest`, Testcontainers)
- Candidate fetches own results → 200 with `{ codingQuestionId, passed, total }` per question
- Candidate fetches another candidate's results → 403
- Candidate calls detail endpoint → 403
- Marker fetches detail → 200 with full rows including `input` and `expectedOutput`
- Response for candidate endpoint does **not** contain `input` or `expectedOutput` fields

### Done When
- Candidate result endpoint returns aggregated pass/fail counts
- Detail endpoint returns full rows to MARKER/ADMIN only
- Ownership check returns 403 for wrong candidate
- All `CodingResultEndpointTest` cases pass

---

## Slice C: Angular Candidate Result View

### Agent Brief
Wire `CodingResultService.getCodingResults()` to the real API. Update the candidate assessment result component to display a pass/fail badge per coding question. Badges are colour-coded by outcome.

### Package Tree Additions

**Frontend**
```
src/app/features/
  assessment/
    components/
      candidate-result/
        candidate-result.component.ts    ← updated
        candidate-result.component.html  ← updated
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
Wire `getCodingResults()` in `CodingResultService` to real API call:
- `getCodingResults(assessmentId)` → `GET /api/assessments/{assessmentId}/coding-results`

### Controllers
None.

### Frontend Type Definitions
No changes.

### Frontend Components

**`CandidateResultComponent`** — updates
- For each question with `type === 'CODING'`: look up the `CodingResultResponse` by `codingQuestionId` and display a badge: `"X / Y passed"`
- Badge colours:
  - `passed === total` → green
  - `passed > 0 && passed < total` → amber
  - `passed === 0 && total > 0` → red
  - `total === 0` (no test cases configured) → grey
- Badge is absent for MCQ, text, and group questions

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Badge renders `"3 / 5 passed"` given mocked `CodingResultResponse` with `passed = 3`, `total = 5`
- Badge class is green when `passed === total`
- Badge class is amber when `passed > 0 && passed < total`
- Badge class is red when `passed === 0 && total > 0`
- Badge class is grey when `total === 0`
- Badge is absent for a non-coding question in the same result list

### Done When
- Candidate sees pass/fail badges on coding questions in their result view
- Badge colours reflect outcome correctly
- All unit tests pass

---

## Slice D: Angular Marker Review — Coding Result Accordion

### Agent Brief
Wire `CodingResultService.getCodingResultDetail()` to the real API. Update the marker submission review component to display a per-test-case accordion on each coding question, with full execution detail including error type labels.

### Package Tree Additions

**Frontend**
```
src/app/features/
  marking/
    components/
      submission-review/
        submission-review.component.ts    ← updated
        submission-review.component.html  ← updated
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
Wire `getCodingResultDetail()` in `CodingResultService` to real API call:
- `getCodingResultDetail(assessmentId)` → `GET /api/assessments/{assessmentId}/coding-results/detail`

### Controllers
None.

### Frontend Type Definitions
No changes.

### Frontend Components

**`SubmissionReviewComponent`** — updates
- For each coding question in the submission: render a collapsible accordion panel
- Each accordion row corresponds to one test case result from `CodingResultDetailResponse`
- Row content: pass/fail indicator icon, `input`, `expectedOutput`, `actualOutput`, `stderr` (if non-empty), `executionTimeMs` in milliseconds
- `errorType = COMPILE_ERROR` → show `"Compile Error"` status label + display `stderr`
- `errorType = TIMEOUT` → show `"Timeout"` label + configured timeout value in seconds
- `errorType = MEMORY` → show `"Memory Limit Exceeded"` label
- `errorType = RUNTIME_ERROR` → show `"Runtime Error"` label + display `stderr`
- `errorType = NONE`, `passed = true` → show green pass icon, no error label

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Accordion renders one row per test case from mocked `CodingResultDetailResponse[]`
- `COMPILE_ERROR` row shows `"Compile Error"` label and `stderr` content
- `TIMEOUT` row shows `"Timeout"` label
- `MEMORY` row shows `"Memory Limit Exceeded"` label
- `RUNTIME_ERROR` row shows `"Runtime Error"` label
- All-pass row shows green pass icon and no error labels

### Done When
- Marker sees per-test-case accordion on coding questions in the submission review
- All error type labels render correctly
- All unit tests pass
