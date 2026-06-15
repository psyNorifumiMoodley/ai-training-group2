### Requirement: Candidate Can Trigger On-Demand Code Execution
The system SHALL provide a `POST /api/assessments/{id}/responses/{questionId}/execute` endpoint that runs the candidate's current code for a CODING question against all its test cases via the Judge0 service and returns the results synchronously. The results are also persisted as `TestCaseResult` rows (overwriting any previous execution results for that response).

#### Scenario: Successful on-demand execution returns per-test-case results
- **WHEN** a candidate sends `POST /api/assessments/{id}/responses/{questionId}/execute` on an in-progress assessment with a CODING question
- **THEN** the server responds with HTTP 200 and a `CodeExecuteResponse` containing a `results` array with one `TestCaseResult` per test case and an `executedAt` timestamp

#### Scenario: Execute endpoint requires an in-progress assessment
- **WHEN** a candidate sends `POST /api/assessments/{id}/responses/{questionId}/execute` on an assessment with status `SUBMITTED`
- **THEN** the server responds with HTTP 409

#### Scenario: Execute endpoint requires code to have been saved first
- **WHEN** a candidate sends `POST /api/assessments/{id}/responses/{questionId}/execute` and no `CodingResponse` exists yet for that question
- **THEN** the server responds with HTTP 404

#### Scenario: Execute endpoint is accessible only to the assessment's candidate
- **WHEN** a user who is not the candidate for the given assessment sends the execute request
- **THEN** the server responds with HTTP 403

---

### Requirement: CodeExecuteResponse Contains Per-Test-Case Pass/Fail Details
The `CodeExecuteResponse` returned by the execute endpoint SHALL contain a `results` array where each element corresponds to one test case and includes: `testCaseId` (UUID), `passed` (boolean), `actualOutput` (String, nullable), `executionTimeMs` (Long, nullable), `memoryUsedMb` (Long, nullable), `errorMessage` (String, nullable on success), and `ordinal` (int).

#### Scenario: Passing test case has passed = true and no error message
- **WHEN** the candidate's code produces the expected output for a test case
- **THEN** the corresponding `TestCaseResult` has `passed = true`, `actualOutput` equal to the expected output, and `errorMessage = null`

#### Scenario: Failing test case has passed = false with actual output
- **WHEN** the candidate's code produces output that does not match the expected output
- **THEN** the corresponding `TestCaseResult` has `passed = false`, `actualOutput` populated with the actual program output, and `errorMessage = null`

#### Scenario: Runtime error test case has passed = false with error message
- **WHEN** the candidate's code throws a runtime exception or compilation error
- **THEN** the corresponding `TestCaseResult` has `passed = false`, `actualOutput = null`, and `errorMessage` containing the error description from Judge0

---

### Requirement: Code Execution Uses Judge0 as the Execution Backend
The system SHALL integrate with Judge0 (self-hosted or cloud) to execute candidate code. The `CodeExecutionService` SHALL map the CODING question's `language` enum (`JAVA`, `PYTHON`, `CSHARP`) to the corresponding Judge0 language ID and submit one batch request per `POST /execute` call.

#### Scenario: Java code is submitted to Judge0 with language ID 91
- **WHEN** the CODING question has `language = JAVA` and execution is triggered
- **THEN** `CodeExecutionService` sends submissions to Judge0 with language_id 91

#### Scenario: Python code is submitted to Judge0 with language ID 71
- **WHEN** the CODING question has `language = PYTHON` and execution is triggered
- **THEN** `CodeExecutionService` sends submissions to Judge0 with language_id 71

#### Scenario: C# code is submitted to Judge0 with language ID 51
- **WHEN** the CODING question has `language = CSHARP` and execution is triggered
- **THEN** `CodeExecutionService` sends submissions to Judge0 with language_id 51

#### Scenario: Judge0 base URL and API key are read from application properties
- **WHEN** the application starts
- **THEN** `CodeExecutionService` reads `code-execution.judge0.base-url` and `code-execution.judge0.api-key` (optional) from `application.properties` via `@Value`; hard-coding either value is forbidden
