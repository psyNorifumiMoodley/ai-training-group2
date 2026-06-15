## 1. Configuration

- [ ] 1.1 Add `code-execution.judge0.base-url=http://localhost:2358` and `code-execution.judge0.api-key=` to `dap-backend/src/main/resources/application.properties`

## 2. CodeExecutionService Interface

- [ ] 2.1 Create `CodeExecutionService.java` in `service/` — interface with single method: `List<TestCaseResultResponse> execute(CodingQuestion question, String code)`; import `CodingQuestion` from `domain/`, `TestCaseResultResponse` from `dto/`

## 3. Judge0CodeExecutionService Implementation

- [ ] 3.1 Create `Judge0CodeExecutionService.java` in `service/` — `@Service` implementing `CodeExecutionService`; `@Value("${code-execution.judge0.base-url}")` and `@Value("${code-execution.judge0.api-key:}")` fields; instantiate `RestClient` in constructor via `RestClient.builder().baseUrl(baseUrl).build()`
- [ ] 3.2 Implement private `languageId(CodingQuestionLanguage lang)` method: `JAVA` → 91, `PYTHON` → 71, `CSHARP` → 51; throw `UnsupportedOperationException` for unknown values
- [ ] 3.3 Implement the `execute()` method — build the Judge0 batch request body (inner record/class `SubmissionRequest` with fields `sourceCode`, `languageId`, `stdin`, `expectedOutput`, `cpuTimeLimit`, `memoryLimit`), one entry per `testCase` in `question.getTestCases()` (memory: `testCase.getMemoryMb() * 1024` KB; `memoryLimit` in KB); send `POST /submissions/batch?base64_encoded=false&wait=true` via `RestClient` with `Content-Type: application/json`; add `X-RapidAPI-Key` header only when `apiKey` is non-blank
- [ ] 3.4 Parse the Judge0 response (inner record `SubmissionResult` with `stdout`, `time`, `memory`, `stderr`, `compileOutput`, `status` (with `id`)); map each result at index `i` to `TestCaseResultResponse`: `testCaseId = testCases.get(i).getId()`, `passed = (status.id == 3)`, `actualOutput = stdout`, `executionTimeMs = parseMillis(time)`, `memoryUsedMb = memory / 1024`, `errorMessage = firstNonBlank(stderr, compileOutput)`, `ordinal = testCases.get(i).getOrdinal()`; if result at index `i` is missing (shorter response than expected), produce `passed=false, errorMessage="No result returned"`

## 4. Wire CodingResponseRequest in ResponseService

- [ ] 4.1 Add `CodingResponseRepository codingResponseRepository` constructor parameter to `ResponseService`; update constructor body
- [ ] 4.2 Add `CodingResponseRequest` branch to `upsertResponse()` — before the final `throw`: `if (request instanceof CodingResponseRequest codingReq) { CodingResponse r = existing instanceof CodingResponse c ? c : new CodingResponse(); r.setAssessment(assessment); r.setQuestion(question); r.setCode(codingReq.code()); return r; }`

## 5. Add executeCode to ResponseService

- [ ] 5.1 Add constructor parameters to `ResponseService`: `CodeExecutionService codeExecutionService`, `TestCaseResultRepository testCaseResultRepository`, `CodingQuestionRepository codingQuestionRepository`; update constructor body
- [ ] 5.2 Implement `@Transactional public CodeExecuteResponse executeCode(UUID assessmentId, UUID questionId, UUID requestingUserId)`:
  - Load `Assessment` or throw `NoSuchElementException`
  - Check `assessment.getCandidate().getId().equals(requestingUserId)` → throw `AccessDeniedException` ("Forbidden") if mismatch
  - Check `status == SUBMITTED || status == MARKED` → throw `ConflictException`
  - Load `CodingResponse` via `codingResponseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId)` → throw `NoSuchElementException` if empty
  - Load `CodingQuestion` via `codingQuestionRepository.findById(questionId)` → throw `NoSuchElementException` if empty
  - Call `List<TestCaseResultResponse> results = codeExecutionService.execute(codingQuestion, codingResponse.getCode())`
  - `testCaseResultRepository.deleteAllByCodingResponseId(codingResponse.getId())`
  - For each result, build and save a `TestCaseResult` entity (load `TestCase` via `codingQuestion.getTestCases()` by matching `result.testCaseId()`)
  - `codingResponse.setExecutedAt(Instant.now()); codingResponseRepository.save(codingResponse)`
  - Return `new CodeExecuteResponse(results, codingResponse.getExecutedAt())`

## 6. Add autoExecuteCodingResponses to ResponseService

- [ ] 6.1 Implement `@Transactional public void autoExecuteCodingResponses(UUID assessmentId)`: load all `Response` rows for the assessment via `responseRepository.findByAssessmentId(assessmentId)`; for each `CodingResponse`, load the `CodingQuestion`, call `codeExecutionService.execute()`, delete old `TestCaseResult` rows, persist new ones, update `executedAt`; any exception from `codeExecutionService.execute()` for an individual response should be caught, logged, and skipped (do not abort remaining responses)

## 7. Wire Execute Endpoint in AssessmentController

- [ ] 7.1 Add `@AuthenticationPrincipal AppUser currentUser` parameter to the `executeCode` controller method
- [ ] 7.2 Replace the hardcoded stub body with: `return ResponseEntity.ok(responseService.executeCode(id, questionId, currentUser.getId()));`

## 8. Auto-Execute on Submission in AssessmentService

- [ ] 8.1 In `AssessmentService.submit()`, after `responseService.autoMarkMcqResponses(assessmentId)`, add: `try { responseService.autoExecuteCodingResponses(assessmentId); } catch (Exception ex) { log.error("Failed to auto-execute coding responses for assessment {}: {}", assessmentId, ex.getMessage(), ex); }`

## 9. Unit Tests

- [ ] 9.1 In `ResponseServiceTest` (or a new `CodingResponseServiceTest`), add tests for `saveResponse` with `CodingResponseRequest`: creates new `CodingResponse` when none exists; updates existing `CodingResponse` when one exists; throws `ConflictException` when assessment is SUBMITTED
- [ ] 9.2 Add tests for `ResponseService.executeCode()`: success path (mocked `CodeExecutionService` returns results, results persisted, `CodeExecuteResponse` returned); throws `NoSuchElementException` when no `CodingResponse` saved; throws `ConflictException` when assessment is SUBMITTED; throws `AccessDeniedException` when wrong candidate

## 10. Verification

- [ ] 10.1 Run `mvn verify` — all tests pass, no compilation errors
