## 1. Phase 1 — Slice 0: API Contracts *(merge first)*

> **Prerequisite:** the Question Model Refactor (`.claude/specs/v1-assessment-platform/phase-6-question-model-refactor.md`) must be merged first. It removes `category` from every question DTO/table in favour of `questionBankIds` (request) / `questionBanks: List<QuestionBankResponse>` (response), and adds `McqPlusQuestionRequest`/`McqPlusQuestionResponse` (`"MCQ_PLUS"`) to the `QuestionRequest`/`QuestionResponse` sealed interfaces. The tasks below assume that state already exists.

- [ ] 1.1 Add `CodingQuestionRequest` record implementing the existing `QuestionRequest` sealed interface: fields `questionBankIds` (`List<UUID>`, `@NotEmpty`), `question` (String, `@NotBlank`), `language` (**required**, `@NotNull CodingQuestionLanguage` enum), `testCases` (nullable list of `TestCaseRequest`); register in `QuestionRequest` `@JsonSubTypes` with name `"CODING"` and in the `permits` clause (alongside `McqPlusQuestionRequest`/`"MCQ_PLUS"` already added by the Question Model Refactor)
- [ ] 1.2 Add `TestCaseRequest` record: fields `input` (String), `expectedOutput` (String, `@NotBlank`), `timeoutSeconds` (int, `@Min(1) @Max(60)`), `memoryMb` (int, `@Min(64) @Max(1024)`) with `@Valid` constraints
- [ ] 1.3 Add `TestCaseResponse` record: fields `id` (UUID), `input`, `expectedOutput`, `timeoutSeconds`, `memoryMb`, `ordinal`
- [ ] 1.4 Add `CodingQuestionResponse` record implementing the existing `QuestionResponse` sealed interface: fields `id` (UUID), `questionBanks` (`List<QuestionBankResponse>`), `question` (String), `language` (`CodingQuestionLanguage`), `testCases` (list of `TestCaseResponse`); register in `QuestionResponse` `@JsonSubTypes` with name `"CODING"` and in the `permits` clause (alongside `McqPlusQuestionResponse`/`"MCQ_PLUS"`)
- [ ] 1.5 No new controller — test cases are embedded in `CodingQuestionRequest.testCases` and created/updated together with the question via `POST /api/questions` and `PUT /api/questions/{id}`; no separate `CodingQuestionController` or test-case sub-resource
- [ ] 1.6 Block doc question creation: in the existing `QuestionController.createQuestion()`, add a guard — if `request instanceof DocQuestionRequest`, return HTTP 410 with message "Doc question creation is deprecated. Use POST /api/questions with type CODING instead."
- [ ] 1.7 Add `CodingQuestionLanguage` enum (`JAVA`, `PYTHON`, `CSHARP`) to the domain package
- [ ] 1.8 Update frontend TypeScript types in `question.model.ts`: add `'CODING'` to `QuestionType` union (alongside `'MCQ_PLUS'` already added by the Question Model Refactor); add `CodingQuestionLanguage` type, `TestCase`, `TestCaseRequest` interfaces, and `CodingQuestionRequest extends BaseQuestionRequest` / `CodingQuestionResponse extends BaseQuestionResponse` (no `category` field — question bank scoping comes from the shared base types) inline — no separate `coding-question.model.ts` file; update `QuestionResponse` union to include `CodingQuestionResponse`
- [ ] 1.9 Update `question.service.ts` union types to include `CodingQuestionRequest`; no separate `CodingQuestionService` — test case management uses the existing `QuestionService.updateQuestion()` with the full `CodingQuestionRequest` including the updated `testCases` list
- [ ] 1.10 Verify stubs compile (backend and frontend) with no errors; merge Slice 0 to main

## 2. Phase 1 — Slice A: Schema Migration & Test Case CRUD (Backend)

- [ ] 2.1 Write Liquibase changeset `2026-06-05-001-create-coding-question-table`: create `coding_question` table (TABLE_PER_CLASS) with columns `id` UUID PK FK → `assessment_question`; `language` VARCHAR NOT NULL; no `category` column — question bank scoping is via the `question_question_bank` join table created by the Question Model Refactor (no FK on `question_id`, same `TABLE_PER_CLASS` pattern as other question types); include rollback (`dropTable`)
- [ ] 2.2 Write Liquibase changeset `2026-06-05-002-create-test-case-table`: create `test_case` table with columns `id` UUID PK, `coding_question_id` UUID FK → `coding_question`, `input` TEXT, `expected_output` TEXT, `timeout_seconds` INT default 10, `memory_mb` INT default 256, `ordinal` INT; include rollback
- [ ] 2.3 Create `CodingQuestion` entity: extends `AssessmentQuestion`; fields `@Enumerated(EnumType.STRING) @Column(nullable=false) CodingQuestionLanguage language`; `@OneToMany(mappedBy="codingQuestion", cascade=CascadeType.ALL, orphanRemoval=true) List<TestCase> testCases`
- [ ] 2.4 Create `TestCase` entity: extends `BaseEntity`; fields `@ManyToOne(fetch=LAZY) CodingQuestion codingQuestion`, `String input`, `String expectedOutput`, `int timeoutSeconds`, `int memoryMb`, `int ordinal`
- [ ] 2.5 Create `TestCaseRepository extends JpaRepository<TestCase, UUID>`; add `List<TestCase> findByCodingQuestionIdOrderByOrdinalAsc(UUID codingQuestionId)`
- [ ] 2.6 No separate `TestCaseService` or `CodingQuestionController` — test cases are persisted as part of `CodingQuestion` via `CascadeType.ALL` when `QuestionService` creates or updates a coding question; `QuestionService` replaces the `testCases` collection from the request on each `PUT`
- [ ] 2.7 Write `CodingQuestionSchemaTest` (`@SpringBootTest`, Testcontainers): verify `coding_question` and `test_case` tables pass `ddl-auto=validate`; verify cascade save persists test case rows when saving a `CodingQuestion`

## 3. Phase 1 — Slice B: Coding Question in QuestionService & Language Validation (Backend)

- [ ] 3.1 Extend `QuestionService.create()` with a new `instanceof CodingQuestionRequest` branch: add a private `createCodingQuestion(CodingQuestionRequest)` method that persists a `CodingQuestion` entity, setting `questionBanks` via the shared `resolveQuestionBanks(request.questionBankIds())` helper added by the Question Model Refactor (throws `ValidationException` if any ID is unknown); `language` validation is handled by `@NotNull` bean validation at the controller level — no manual check needed; constructor-inject `CodingQuestionRepository` into `QuestionService`
- [ ] 3.2 Extend `QuestionService.toResponse()` with a new `instanceof CodingQuestion` branch: add a private `toCodingQuestionResponse(CodingQuestion)` method that maps entity fields (including `testCases` list and `questionBanks` via the shared `toQbResponses()` helper) to `CodingQuestionResponse`; use `@Transactional(readOnly = true)` on `toResponse()` to support lazy association traversal
- [ ] 3.3 No new controller changes — `POST /api/questions` in `QuestionController` already routes to `QuestionService.create()`; creation of coding questions flows through the existing endpoint automatically once the service branch is added
- [ ] 3.4 Update assessment generation doc limit check (`AssessmentService`): count both `DocQuestion` and `CodingQuestion` rows toward the limit — add a comment explaining the multi-subtype count if the query is non-obvious
- [ ] 3.5 Write `CodingQuestionServiceTest` (`@SpringBootTest`, Testcontainers): `POST /api/questions` with `"type": "CODING"`, `language=JAVA`, and a valid `questionBankIds` → 201, language and question banks persisted; `POST /api/questions` with `"type": "CODING"` and missing `language` → 400; `POST /api/questions` with `"type": "CODING"` and an unknown or empty `questionBankIds` → 400; `GET /api/questions/{id}` for a coding question returns `CodingQuestionResponse` with `questionBanks` populated and `testCases: []`; doc+coding limit: one legacy doc question + one coding question in one assessment → 409; two coding questions in one assessment → 409

## 4. Phase 1 — Slice C: Angular Question Editor — Coding Question Form

- [ ] 4.1 Add a new "Coding Question" creation/edit form component: `QuestionBankSelectorComponent` (from the Question Model Refactor, supports inline QB creation; at least one required) plus a language dropdown (required, options: Java, Python, C#); use `input()` signal API; `OnPush` change detection
- [ ] 4.2 Add a test case editor panel below the language dropdown: rows with input textarea, expected output textarea, timeout (number input, 1–60), memory MB (number input, 64–1024); add/remove row buttons
- [ ] 4.3 Remove the "Doc Question" option from the question bank question creation menu
- [ ] 4.4 On form submit, call `QuestionService.createQuestion()` with the full `CodingQuestionRequest` including `questionBankIds` and the `testCases` list — all test cases are persisted in a single request; no separate service or endpoint for test cases
- [ ] 4.5 On edit/save, call `QuestionService.updateQuestion(id, request)` with the full updated `CodingQuestionRequest` including `questionBankIds` and the current `testCases` list
- [ ] 4.6 Write unit tests: language dropdown renders Java/Python/C# options; language is required (form invalid without it); form invalid until at least one question bank is selected via `QuestionBankSelectorComponent`; test case panel visible when language is selected; add row appends a blank row; remove row deletes that row; submit calls service with correct payload including `questionBankIds`

## 5. Phase 1 — Slice D: Angular Question Bank — Language Badge & Test Case Count

- [ ] 5.1 Update the question bank question list/card component to display a language badge ("Java", "Python", "C#") on every coding question
- [ ] 5.2 Display test case count alongside the language badge (e.g., "Java · 3 test cases"); show "0 test cases" if none configured
- [ ] 5.3 Update the question detail view to show a read-only list of test cases (input, expected output, timeout, memory) for coding questions
- [ ] 5.4 Write unit tests: badge renders for coding question; count displays correct number; zero test cases shows "0 test cases"

## 6. Phase 2 — Slice 0: ExecutionEngine Interface & Result Schema *(merge first)*

- [ ] 6.1 Define `ExecutionEngine` interface in `service/` package: `ExecutionResult execute(ExecutionRequest request)` — one method only
- [ ] 6.2 Define `ExecutionRequest` record: `language`, `sourceCode` (String), `input` (String), `timeoutSeconds` (int), `memoryMb` (int)
- [ ] 6.3 Define `ExecutionResult` record: `passed` (boolean), `actualOutput` (String), `stderr` (String), `executionTimeMs` (long), `errorType` (enum: `NONE`, `TIMEOUT`, `MEMORY`, `COMPILE_ERROR`, `RUNTIME_ERROR`)
- [ ] 6.4 Write Liquibase changeset `2026-06-05-003-create-execution-result-table`: create `execution_result` table with columns `id` UUID PK, `submission_id` UUID FK → `submission`, `test_case_id` UUID FK → `test_case`, `coding_question_id` UUID FK → `coding_question`, `passed` BOOLEAN, `actual_output` TEXT, `stderr` TEXT, `execution_time_ms` BIGINT, `error_type` VARCHAR; add UNIQUE constraint on `(submission_id, test_case_id)`; include rollback
- [ ] 6.5 Create `ExecutionResultEntity` (JPA entity for `execution_result` table, with `codingQuestionId` field); create `ExecutionResultRepository`
- [ ] 6.6 Define `UnsupportedLanguageException` in `domain/` (extends `RuntimeException`)
- [ ] 6.7 Verify interface and schema compile; merge Slice 0 to main

## 7. Phase 2 — Slice A: DockerExecutionEngine Core

- [ ] 7.1 Add `com.github.docker-java:docker-java-core` and `com.github.docker-java:docker-java-transport-httpclient5` dependencies to `pom.xml`
- [ ] 7.2 Create `DockerExecutionEngineConfig` (`@Configuration`): define `DockerClient` bean using `DefaultDockerClientConfig` (socket path from `application.properties` key `execution.docker.socket`, default `/var/run/docker.sock`)
- [ ] 7.3 Create `DockerExecutionEngine implements ExecutionEngine`: constructor-inject `DockerClient` and `List<LanguageRunner>`; implement `execute()` — select runner by language, build container, copy source code, run with input piped to stdin, capture stdout/stderr, enforce timeout via `HostConfig.withNanoCPUs()` and memory via `HostConfig.withMemory()`, return `ExecutionResult`
- [ ] 7.4 Implement image pre-pull on startup: create `DockerImageWarmer implements ApplicationRunner`; on `run()` pull all language images in parallel using `CompletableFuture`; log WARN and continue if Docker is unreachable
- [ ] 7.5 Ensure containers are always removed: use `HostConfig.withAutoRemove(true)` on every container create call; add `finally` block to force-remove if `autoRemove` fails
- [ ] 7.6 Write `DockerExecutionEngineIntegrationTest` (`@SpringBootTest`, requires Docker): Python "hello world" → passed=true, actualOutput="hello world"; Python infinite loop with timeout=1s → passed=false, errorType=TIMEOUT; container count unchanged after test (no orphans)

## 8. Phase 2 — Slice B: Java Runner

- [ ] 8.1 Define `LanguageRunner` interface: `String image()`, `List<String> compileCommand(String sourceFile)` (empty list if no compilation), `List<String> runCommand(String sourceFile)`, `String sourceFileExtension()`
- [ ] 8.2 Create `JavaRunner implements LanguageRunner`: image = `eclipse-temurin:17-jdk-alpine`; compileCommand = `["javac", <sourceFile>]`; runCommand = `["java", <className>]` where className is derived by stripping `.java`; extension = `.java`
- [ ] 8.3 Register `JavaRunner` as a `@Component` so `DockerExecutionEngine` picks it up via `List<LanguageRunner>` injection
- [ ] 8.4 Write `JavaRunnerIntegrationTest` (`@SpringBootTest`, requires Docker): simple `System.out.println("Hello")` → passed=true; compilation error → passed=false, errorType=COMPILE_ERROR, stderr non-empty; runtime `NullPointerException` → passed=false, errorType=RUNTIME_ERROR

## 9. Phase 2 — Slice C: Python Runner

- [ ] 9.1 Create `PythonRunner implements LanguageRunner`: image = `python:3.12-slim`; compileCommand = `[]` (empty — no compilation); runCommand = `["python3", <sourceFile>]`; extension = `.py`
- [ ] 9.2 Register `PythonRunner` as a `@Component`
- [ ] 9.3 Write `PythonRunnerIntegrationTest` (`@SpringBootTest`, requires Docker): `print("Hello")` → passed=true; `raise Exception("oops")` → passed=false, errorType=RUNTIME_ERROR, stderr contains "Exception"; timeout enforcement: `while True: pass` with timeoutSeconds=1 → errorType=TIMEOUT

## 10. Phase 2 — Slice D: C# Runner

- [ ] 10.1 Create `CsharpRunner implements LanguageRunner`: image = `mcr.microsoft.com/dotnet/sdk:8.0-alpine`; compileCommand = `[]` (dotnet run handles compile+run); runCommand = `["dotnet", "script", <sourceFile>]` or `["dotnet-script", <sourceFile>]` depending on chosen approach; extension = `.cs`
- [ ] 10.2 Evaluate and decide between `dotnet-script` (single-file execution) and creating a minimal project scaffold per execution; document the decision in a code comment
- [ ] 10.3 Register `CsharpRunner` as a `@Component`
- [ ] 10.4 Write `CsharpRunnerIntegrationTest` (`@SpringBootTest`, requires Docker): `Console.WriteLine("Hello")` → passed=true; invalid C# syntax → passed=false, errorType=COMPILE_ERROR; timeout enforcement → errorType=TIMEOUT

## 11. Phase 3 — Slice 0: Grading Interface & Result API Contracts *(merge first)*

- [ ] 11.1 Define `GradingService` interface: `void gradeSubmission(UUID submissionId)` — one method
- [ ] 11.2 Add `CodingResultResponse` record: fields `codingQuestionId` (UUID), `passed` (int), `total` (int)
- [ ] 11.3 Add `CodingResultDetailResponse` record: fields `testCaseId` (UUID), `codingQuestionId` (UUID), `passed` (boolean), `input` (String), `expectedOutput` (String), `actualOutput` (String), `stderr` (String), `executionTimeMs` (long), `errorType` (String)
- [ ] 11.4 Add stub endpoints to `AssessmentController`: `GET /api/assessments/{id}/coding-results` → 200 empty array (CANDIDATE role required); `GET /api/assessments/{id}/coding-results/detail` → 200 empty array (MARKER/ADMIN role required)
- [ ] 11.5 Add stub Angular service methods in a new `CodingResultService`: `getCodingResults(assessmentId)` → `Observable<CodingResultResponse[]>` returning `EMPTY`; `getCodingResultDetail(assessmentId)` → `Observable<CodingResultDetailResponse[]>` returning `EMPTY`
- [ ] 11.6 Define TypeScript models: `CodingResultResponse` (with `codingQuestionId`), `CodingResultDetailResponse` (with `codingQuestionId`)
- [ ] 11.7 Verify stubs compile; merge Slice 0 to main

## 12. Phase 3 — Slice A: GradingService Implementation

- [ ] 12.1 Create `GradingServiceImpl implements GradingService`: constructor-inject `ExecutionEngine`, `SubmissionRepository`, `TestCaseRepository`, `ExecutionResultRepository`, `AssessmentRepository`
- [ ] 12.2 Implement `gradeSubmission(UUID submissionId)`: load submission; for each `CodingQuestion` in the assessment, load all test cases; execute each test case concurrently using `CompletableFuture.supplyAsync()` with a bounded thread pool; compare `actualOutput.trim()` to `expectedOutput.trim()`; persist one `ExecutionResultEntity` per test case with `codingQuestionId` set
- [ ] 12.3 Call `gradingService.gradeSubmission()` from `AssessmentService.submit()` after status is set to `SUBMITTED` — run asynchronously using `@Async` so the submit response is not blocked
- [ ] 12.4 Ensure `gradeSubmission` is `@Transactional` only for its own DB writes; execution engine calls are outside any transaction boundary
- [ ] 12.5 Write `GradingServiceIntegrationTest` (`@SpringBootTest`, Testcontainers + Docker): submit assessment with one Python coding question (2 test cases, 1 correct) → 2 `execution_result` rows; `passed=true` for correct, `passed=false` for incorrect; second submit does not create duplicate rows (UNIQUE constraint)

## 13. Phase 3 — Slice B: Result Query Endpoints (Backend)

- [ ] 13.1 Create `ExecutionResultService`: `List<CodingResultResponse> getCodingResults(UUID assessmentId, UUID requestingUserId)` — verifies candidate owns assessment (403 if not); aggregates execution_result rows into per-question pass/fail counts grouped by `codingQuestionId`; `List<CodingResultDetailResponse> getCodingResultDetail(UUID assessmentId)` — returns full rows
- [ ] 13.2 Wire `ExecutionResultService` into `AssessmentController` stub endpoints (replace hardcoded responses)
- [ ] 13.3 Add security: `GET /api/assessments/{id}/coding-results` requires CANDIDATE role and ownership check; `GET /api/assessments/{id}/coding-results/detail` requires MARKER or ADMIN role
- [ ] 13.4 Write `CodingResultEndpointTest` (`@SpringBootTest`, Testcontainers): candidate fetches own results → 200 with `{ codingQuestionId, passed, total }`; candidate fetches another's results → 403; candidate fetches detail endpoint → 403; marker fetches detail → 200 with full rows including `input` and `expectedOutput`; response for candidate endpoint does NOT contain `input` or `expectedOutput` fields

## 14. Phase 3 — Slice C: Angular Candidate Result View

- [ ] 14.1 Update `getCodingResults()` in `CodingResultService` with the real `GET /api/assessments/{id}/coding-results` API call
- [ ] 14.2 In the candidate assessment result component, for each question that is a coding question (detected by `type === 'CODING'`), display a pass/fail badge: "X / Y passed"
- [ ] 14.3 Apply colour coding: all passed = green badge; some passed = amber badge; none passed = red badge; no test cases = grey badge
- [ ] 14.4 Write unit tests: badge renders "3 / 5 passed" given mocked `CodingResultResponse`; badge colour is green when `passed === total`; badge absent for non-coding questions

## 15. Phase 3 — Slice D: Angular Marker Review — Coding Result Accordion

- [ ] 15.1 Update `getCodingResultDetail()` in `CodingResultService` with the real `GET /api/assessments/{id}/coding-results/detail` API call
- [ ] 15.2 In the marker submission review component, add a coding result accordion panel for each coding question: one expandable row per test case, showing pass/fail icon, input, expected output, actual output, stderr, and execution time in ms
- [ ] 15.3 For `errorType = COMPILE_ERROR`: display "Compile Error" status label and show stderr
- [ ] 15.4 For `errorType = TIMEOUT`: display "Timeout" label with the configured timeout value
- [ ] 15.5 For `errorType = MEMORY`: display "Memory Limit Exceeded" label
- [ ] 15.6 Write unit tests: accordion renders one row per test case from mocked data; COMPILE_ERROR row shows "Compile Error" label and stderr; TIMEOUT row shows "Timeout" label; all-pass shows no error labels
