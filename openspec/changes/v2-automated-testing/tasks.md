## 1. Phase 6 — Slice 0: API Contracts *(merge first)*

- [ ] 1.1 Add `DocQuestionExtendedRequest` record: fields `language` (nullable `Language` enum), `testCases` (nullable list of `TestCaseRequest`)
- [ ] 1.2 Add `TestCaseRequest` record: fields `input` (String), `expectedOutput` (String), `timeoutSeconds` (int), `memoryMb` (int) with `@Valid` constraints
- [ ] 1.3 Add `TestCaseResponse` record: fields `id` (UUID), `input`, `expectedOutput`, `timeoutSeconds`, `memoryMb`, `ordinal`
- [ ] 1.4 Add `DocQuestionResponse` updated to include `language` (nullable) and `testCases` (list of `TestCaseResponse`)
- [ ] 1.5 Add stub endpoints to `QuestionController`: `POST /api/questions/{questionId}/test-cases`, `PUT /api/questions/{questionId}/test-cases/{testCaseId}`, `DELETE /api/questions/{questionId}/test-cases/{testCaseId}`, `GET /api/questions/{questionId}/test-cases` — all return hardcoded 200/201/204
- [ ] 1.6 Add `Language` enum (`JAVA`, `PYTHON`, `CSHARP`) to the domain package
- [ ] 1.7 Define frontend TypeScript models: `Language`, `TestCase`, `TestCaseRequest`, updated `DocQuestionResponse` with `language?: Language` and `testCases?: TestCase[]`
- [ ] 1.8 Add stub Angular service methods in `QuestionService`: `addTestCase()`, `updateTestCase()`, `deleteTestCase()`, `getTestCases()` — all return `EMPTY`
- [ ] 1.9 Verify stubs compile (backend and frontend) with no errors; merge Slice 0 to main

## 2. Phase 6 — Slice A: Schema Migration & Test Case CRUD (Backend)

- [ ] 2.1 Write Liquibase changeset `2026-06-05-001-add-language-to-doc-question`: add nullable `language` VARCHAR column to `doc_question` table; include rollback
- [ ] 2.2 Write Liquibase changeset `2026-06-05-002-create-test-case-table`: create `test_case` table with columns `id` UUID PK, `doc_question_id` UUID FK → `doc_question`, `input` TEXT, `expected_output` TEXT, `timeout_seconds` INT default 10, `memory_mb` INT default 256, `ordinal` INT; include rollback
- [ ] 2.3 Update `DocQuestion` entity: add `@Enumerated(EnumType.STRING) Language language` (nullable); add `@OneToMany(mappedBy="docQuestion", cascade=CascadeType.ALL, orphanRemoval=true) List<TestCase> testCases`
- [ ] 2.4 Create `TestCase` entity: extends `BaseEntity`; fields `@ManyToOne(fetch=LAZY) DocQuestion docQuestion`, `String input`, `String expectedOutput`, `int timeoutSeconds`, `int memoryMb`, `int ordinal`
- [ ] 2.5 Create `TestCaseRepository extends JpaRepository<TestCase, UUID>`; add `List<TestCase> findByDocQuestionIdOrderByOrdinalAsc(UUID docQuestionId)`
- [ ] 2.6 Create `TestCaseService`: implement `addTestCase(UUID questionId, TestCaseRequest request)` — validates question is a coding question (language not null), sets ordinal, saves; `updateTestCase(UUID questionId, UUID testCaseId, TestCaseRequest request)`; `deleteTestCase(UUID questionId, UUID testCaseId)`; `getTestCases(UUID questionId)`
- [ ] 2.7 Wire `TestCaseService` into `QuestionController` stub endpoints (replace hardcoded responses)
- [ ] 2.8 Write `TestCaseServiceTest` (`@SpringBootTest`, Testcontainers): add test case → 201; add to plain doc question → 409; timeoutSeconds = 0 → 400; timeoutSeconds > 60 → 400; memoryMb < 64 → 400; memoryMb > 1024 → 400; delete → 204 and row removed; update → 200 and fields reflect new values

## 3. Phase 6 — Slice B: Language Validation & Question Bank Service Updates (Backend)

- [ ] 3.1 Update `QuestionService.createDocQuestion()` to accept `language` from `DocQuestionExtendedRequest` and persist it on the `DocQuestion` entity
- [ ] 3.2 Add validation in `QuestionService`: if `language` is not null and is not a value in the `Language` enum, throw `ValidationException` (400)
- [ ] 3.3 Confirm doc question limit check in assessment generation (`AssessmentService`) operates on all `DocQuestion` rows regardless of language — verify no code change needed and add a comment if the logic is non-obvious
- [ ] 3.4 Update `QuestionService.getDocQuestion()` to eagerly load `testCases` (use `@EntityGraph` or `JOIN FETCH`) and include them in `DocQuestionResponse`
- [ ] 3.5 Write `DocQuestionLanguageTest` (`@SpringBootTest`, Testcontainers): create with `language=JAVA` → 201 and language persisted; create with `language=null` → 201 and no test cases; create with `language="RUBY"` → 400; doc question limit: two doc questions in one assessment → 409

## 4. Phase 6 — Slice C: Angular Question Editor — Language & Test Cases

- [ ] 4.1 Update the doc question creation/edit form component to add a language dropdown (options: None, Java, Python, C#); use `input()` signal API; `OnPush` change detection
- [ ] 4.2 Add a test case editor panel below the language dropdown — visible only when language is selected; rows: input textarea, expected output textarea, timeout (number input, 1–60), memory MB (number input, 64–1024); add/remove row buttons
- [ ] 4.3 Wire the `addTestCase()`, `updateTestCase()`, `deleteTestCase()` service stubs to real API calls in `QuestionService`
- [ ] 4.4 On form submit, include `language` in the doc question create/update request; after question is saved, persist any test case rows via the test case API
- [ ] 4.5 Write unit tests: language dropdown renders correct options; test case panel hidden when no language selected; add row appends a new blank row; remove row deletes that row; form submit with language calls service with correct payload

## 5. Phase 6 — Slice D: Angular Question Bank — Language Badge & Test Case Count

- [ ] 5.1 Update the question bank question list/card component to display a language badge ("Java", "Python", "C#") on any doc question with a non-null language
- [ ] 5.2 Display test case count alongside the language badge (e.g., "Java · 3 test cases"); hide both elements if language is null
- [ ] 5.3 Update the question detail view to show a read-only list of test cases (input, expected output, timeout, memory) when the question is a coding question
- [ ] 5.4 Write unit tests: badge renders for coding question; badge absent for plain doc question; count displays correct number; zero test cases shows "0 test cases"

## 6. Phase 7 — Slice 0: ExecutionEngine Interface & Result Schema *(merge first)*

- [ ] 6.1 Define `ExecutionEngine` interface in `service/` package: `ExecutionResult execute(ExecutionRequest request)` — one method only
- [ ] 6.2 Define `ExecutionRequest` record: `language`, `sourceCode` (String), `input` (String), `timeoutSeconds` (int), `memoryMb` (int)
- [ ] 6.3 Define `ExecutionResult` record: `passed` (boolean), `actualOutput` (String), `stderr` (String), `executionTimeMs` (long), `errorType` (enum: `NONE`, `TIMEOUT`, `MEMORY`, `COMPILE_ERROR`, `RUNTIME_ERROR`)
- [ ] 6.4 Write Liquibase changeset `2026-06-05-003-create-execution-result-table`: create `execution_result` table with columns `id` UUID PK, `submission_id` UUID FK → `submission`, `test_case_id` UUID FK → `test_case`, `doc_question_id` UUID FK → `doc_question`, `passed` BOOLEAN, `actual_output` TEXT, `stderr` TEXT, `execution_time_ms` BIGINT, `error_type` VARCHAR; add UNIQUE constraint on `(submission_id, test_case_id)`; include rollback
- [ ] 6.5 Create `ExecutionResultEntity` (JPA entity for `execution_result` table); create `ExecutionResultRepository`
- [ ] 6.6 Define `UnsupportedLanguageException` in `domain/` (extends `RuntimeException`)
- [ ] 6.7 Verify interface and schema compile; merge Slice 0 to main

## 7. Phase 7 — Slice A: DockerExecutionEngine Core

- [ ] 7.1 Add `com.github.docker-java:docker-java-core` and `com.github.docker-java:docker-java-transport-httpclient5` dependencies to `pom.xml`
- [ ] 7.2 Create `DockerExecutionEngineConfig` (`@Configuration`): define `DockerClient` bean using `DefaultDockerClientConfig` (socket path from `application.properties` key `execution.docker.socket`, default `/var/run/docker.sock`)
- [ ] 7.3 Create `DockerExecutionEngine implements ExecutionEngine`: constructor-inject `DockerClient` and `List<LanguageRunner>`; implement `execute()` — select runner by language, build container, copy source code, run with input piped to stdin, capture stdout/stderr, enforce timeout via `HostConfig.withNanoCPUs()` and memory via `HostConfig.withMemory()`, return `ExecutionResult`
- [ ] 7.4 Implement image pre-pull on startup: create `DockerImageWarmer implements ApplicationRunner`; on `run()` pull all language images in parallel using `CompletableFuture`; log WARN and continue if Docker is unreachable
- [ ] 7.5 Ensure containers are always removed: use `HostConfig.withAutoRemove(true)` on every container create call; add `finally` block to force-remove if `autoRemove` fails
- [ ] 7.6 Write `DockerExecutionEngineIntegrationTest` (`@SpringBootTest`, requires Docker): Python "hello world" → passed=true, actualOutput="hello world"; Python infinite loop with timeout=1s → passed=false, errorType=TIMEOUT; container count unchanged after test (no orphans)

## 8. Phase 7 — Slice B: Java Runner

- [ ] 8.1 Define `LanguageRunner` interface: `String image()`, `List<String> compileCommand(String sourceFile)` (empty list if no compilation), `List<String> runCommand(String sourceFile)`, `String sourceFileExtension()`
- [ ] 8.2 Create `JavaRunner implements LanguageRunner`: image = `eclipse-temurin:17-jdk-alpine`; compileCommand = `["javac", <sourceFile>]`; runCommand = `["java", <className>]` where className is derived by stripping `.java`; extension = `.java`
- [ ] 8.3 Register `JavaRunner` as a `@Component` so `DockerExecutionEngine` picks it up via `List<LanguageRunner>` injection
- [ ] 8.4 Write `JavaRunnerIntegrationTest` (`@SpringBootTest`, requires Docker): simple `System.out.println("Hello")` → passed=true; compilation error → passed=false, errorType=COMPILE_ERROR, stderr non-empty; runtime `NullPointerException` → passed=false, errorType=RUNTIME_ERROR

## 9. Phase 7 — Slice C: Python Runner

- [ ] 9.1 Create `PythonRunner implements LanguageRunner`: image = `python:3.12-slim`; compileCommand = `[]` (empty — no compilation); runCommand = `["python3", <sourceFile>]`; extension = `.py`
- [ ] 9.2 Register `PythonRunner` as a `@Component`
- [ ] 9.3 Write `PythonRunnerIntegrationTest` (`@SpringBootTest`, requires Docker): `print("Hello")` → passed=true; `raise Exception("oops")` → passed=false, errorType=RUNTIME_ERROR, stderr contains "Exception"; timeout enforcement: `while True: pass` with timeoutSeconds=1 → errorType=TIMEOUT

## 10. Phase 7 — Slice D: C# Runner

- [ ] 10.1 Create `CsharpRunner implements LanguageRunner`: image = `mcr.microsoft.com/dotnet/sdk:8.0-alpine`; compileCommand = `[]` (dotnet run handles compile+run); runCommand = `["dotnet", "script", <sourceFile>]` or `["dotnet-script", <sourceFile>]` depending on chosen approach; extension = `.cs`
- [ ] 10.2 Evaluate and decide between `dotnet-script` (single-file execution) and creating a minimal project scaffold per execution; document the decision in a code comment
- [ ] 10.3 Register `CsharpRunner` as a `@Component`
- [ ] 10.4 Write `CsharpRunnerIntegrationTest` (`@SpringBootTest`, requires Docker): `Console.WriteLine("Hello")` → passed=true; invalid C# syntax → passed=false, errorType=COMPILE_ERROR; timeout enforcement → errorType=TIMEOUT

## 11. Phase 8 — Slice 0: Grading Interface & Result API Contracts *(merge first)*

- [ ] 11.1 Define `GradingService` interface: `void gradeSubmission(UUID submissionId)` — one method
- [ ] 11.2 Add `CodingResultResponse` record: fields `docQuestionId` (UUID), `passed` (int), `total` (int)
- [ ] 11.3 Add `CodingResultDetailResponse` record: fields `testCaseId` (UUID), `docQuestionId` (UUID), `passed` (boolean), `input` (String), `expectedOutput` (String), `actualOutput` (String), `stderr` (String), `executionTimeMs` (long), `errorType` (String)
- [ ] 11.4 Add stub endpoints to `AssessmentController`: `GET /api/assessments/{id}/coding-results` → 200 empty array (CANDIDATE role required); `GET /api/assessments/{id}/coding-results/detail` → 200 empty array (MARKER/ADMIN role required)
- [ ] 11.5 Add stub Angular service methods in `AssessmentService` (or new `CodingResultService`): `getCodingResults(assessmentId)` → `Observable<CodingResultResponse[]>` returning `EMPTY`; `getCodingResultDetail(assessmentId)` → `Observable<CodingResultDetailResponse[]>` returning `EMPTY`
- [ ] 11.6 Define TypeScript models: `CodingResultResponse`, `CodingResultDetailResponse`
- [ ] 11.7 Verify stubs compile; merge Slice 0 to main

## 12. Phase 8 — Slice A: GradingService Implementation

- [ ] 12.1 Create `GradingServiceImpl implements GradingService`: constructor-inject `ExecutionEngine`, `SubmissionRepository`, `TestCaseRepository`, `ExecutionResultRepository`, `AssessmentRepository`
- [ ] 12.2 Implement `gradeSubmission(UUID submissionId)`: load submission; for each `DocQuestion` in the assessment where `language IS NOT NULL`, load all test cases; execute each test case concurrently using `CompletableFuture.supplyAsync()` with a bounded thread pool; compare `actualOutput.trim()` to `expectedOutput.trim()`; persist one `ExecutionResultEntity` per test case
- [ ] 12.3 Call `gradingService.gradeSubmission()` from `AssessmentService.submit()` after status is set to `SUBMITTED` — run asynchronously using `@Async` so the submit response is not blocked
- [ ] 12.4 Ensure `gradeSubmission` is `@Transactional` only for its own DB writes; execution engine calls are outside any transaction boundary
- [ ] 12.5 Write `GradingServiceIntegrationTest` (`@SpringBootTest`, Testcontainers + Docker): submit assessment with one Python coding question (2 test cases, 1 correct) → 2 `execution_result` rows; `passed=true` for correct, `passed=false` for incorrect; second submit does not create duplicate rows (UNIQUE constraint)

## 13. Phase 8 — Slice B: Result Query Endpoints (Backend)

- [ ] 13.1 Create `ExecutionResultService`: `List<CodingResultResponse> getCodingResults(UUID assessmentId, UUID requestingUserId)` — verifies candidate owns assessment (403 if not); aggregates execution_result rows into per-question pass/fail counts; `List<CodingResultDetailResponse> getCodingResultDetail(UUID assessmentId)` — returns full rows
- [ ] 13.2 Wire `ExecutionResultService` into `AssessmentController` stub endpoints (replace hardcoded responses)
- [ ] 13.3 Add security: `GET /api/assessments/{id}/coding-results` requires CANDIDATE role and ownership check; `GET /api/assessments/{id}/coding-results/detail` requires MARKER or ADMIN role
- [ ] 13.4 Write `CodingResultEndpointTest` (`@SpringBootTest`, Testcontainers): candidate fetches own results → 200 with `{ docQuestionId, passed, total }`; candidate fetches another's results → 403; candidate fetches detail endpoint → 403; marker fetches detail → 200 with full rows including `input` and `expectedOutput`; response for candidate endpoint does NOT contain `input` or `expectedOutput` fields

## 14. Phase 8 — Slice C: Angular Candidate Result View

- [ ] 14.1 Update `getCodingResults()` in the Angular service with the real `GET /api/assessments/{id}/coding-results` API call
- [ ] 14.2 In the candidate assessment result component, for each question that is a coding question (detected by `type === 'DOC_QUESTION' && language !== null`), display a pass/fail badge: "X / Y passed"
- [ ] 14.3 Apply colour coding: all passed = green badge; some passed = amber badge; none passed = red badge; no test cases = grey badge
- [ ] 14.4 Write unit tests: badge renders "3 / 5 passed" given mocked `CodingResultResponse`; badge colour is green when `passed === total`; badge absent for non-coding questions

## 15. Phase 8 — Slice D: Angular Marker Review — Coding Result Accordion

- [ ] 15.1 Update `getCodingResultDetail()` in the Angular service with the real `GET /api/assessments/{id}/coding-results/detail` API call
- [ ] 15.2 In the marker submission review component, add a coding result accordion panel for each coding question: one expandable row per test case, showing pass/fail icon, input, expected output, actual output, stderr, and execution time in ms
- [ ] 15.3 For `errorType = COMPILE_ERROR`: display "Compile Error" status label and show stderr
- [ ] 15.4 For `errorType = TIMEOUT`: display "Timeout" label with the configured timeout value
- [ ] 15.5 For `errorType = MEMORY`: display "Memory Limit Exceeded" label
- [ ] 15.6 Write unit tests: accordion renders one row per test case from mocked data; COMPILE_ERROR row shows "Compile Error" label and stderr; TIMEOUT row shows "Timeout" label; all-pass shows no error labels
