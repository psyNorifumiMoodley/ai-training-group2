### Requirement: Candidate Saves Code for a Coding Question
The system SHALL accept a `CodingResponseRequest` body (containing a `code` string) at `PUT /api/assessments/{id}/responses/{questionId}` when the target question is of type `CODING`. The code is persisted as a `CodingResponse` row associated with the assessment and question. Multiple saves overwrite the previous code (upsert semantics, same as other response types).

#### Scenario: Valid coding response is saved
- **WHEN** a candidate sends `PUT /api/assessments/{id}/responses/{questionId}` with body `{ "code": "public class Main { ... }" }` and the question is a CODING type
- **THEN** the server responds with HTTP 204 and the code is persisted

#### Scenario: Blank code string is rejected
- **WHEN** a candidate sends `PUT /api/assessments/{id}/responses/{questionId}` with body `{ "code": "" }`
- **THEN** the server responds with HTTP 400 and a validation error message

#### Scenario: Coding response cannot be saved after submission
- **WHEN** a candidate attempts to save a coding response on an assessment with status `SUBMITTED`
- **THEN** the server responds with HTTP 409

---

### Requirement: CodingResponse Is Auto-Executed on Assessment Submission
When a candidate submits an assessment, the system SHALL re-execute all `CodingResponse` rows against the Judge0 service and store the authoritative `TestCaseResult` set. This is the final record used for marking.

#### Scenario: Submission triggers code execution for all coding responses
- **WHEN** a candidate submits an assessment containing at least one `CodingResponse`
- **THEN** the submission endpoint calls the `CodeExecutionService` for each `CodingResponse` and stores the resulting `TestCaseResult` rows before returning

#### Scenario: Submission completes even if Judge0 execution fails
- **WHEN** a candidate submits and the Judge0 service returns an error for one or more test cases
- **THEN** the assessment is still transitioned to `SUBMITTED` and the failed test case results are recorded with `passed = false` and the error message populated

---

### Requirement: CodingResponse Is Included in the Assessment Review for Markers
When a marker retrieves responses for review via `GET /api/assessments/{id}/responses`, each CODING question SHALL appear with the candidate's submitted code and all `TestCaseResult` rows.

#### Scenario: Marker sees code and test case results
- **WHEN** a marker sends `GET /api/assessments/{id}/responses` for a submitted assessment with a coding question
- **THEN** the response includes an item with `type: "CODING"`, the candidate's `code` string, and a `testCaseResults` array with one entry per test case

---

### Requirement: CodingResponse Entity Persists Candidate Code
The system SHALL persist a candidate's submitted code for a CODING question as a `CodingResponse` row in the `coding_response` table, linked to the parent `response` record via JOINED inheritance.

#### Scenario: CodingResponse row is created with non-null code
- **WHEN** a `CodingResponse` is saved with a non-null `code` value and valid `assessment` and `question` associations
- **THEN** a row exists in `response` (with `dtype = 'CodingResponse'`) and a corresponding row exists in `coding_response` with the same `id` and the persisted `code`

#### Scenario: CodingResponse is retrievable by assessment and question
- **WHEN** `CodingResponseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId)` is called with valid IDs
- **THEN** the matching `CodingResponse` is returned (or `Optional.empty()` if none exists)

#### Scenario: CodingResponse tracks last execution timestamp
- **WHEN** a `CodingResponse` is saved with a non-null `executedAt` value
- **THEN** the `executed_at` column in `coding_response` reflects that timestamp

#### Scenario: CodingResponse with null executedAt is valid
- **WHEN** a `CodingResponse` is saved without setting `executedAt`
- **THEN** the `executed_at` column is NULL and the row persists without error

---

### Requirement: TestCaseResult Entity Persists Execution Outcomes
The system SHALL persist the per-test-case result of a code execution run as `TestCaseResult` rows in the `test_case_result` table, each linked to exactly one `CodingResponse` and one `TestCase`.

#### Scenario: TestCaseResult row persists all execution fields
- **WHEN** a `TestCaseResult` is saved with `codingResponse`, `testCase`, `passed`, `ordinal`, and optional nullable fields (`actualOutput`, `executionTimeMs`, `memoryUsedMb`, `errorMessage`)
- **THEN** a row exists in `test_case_result` with all fields correctly stored

#### Scenario: All TestCaseResults for a CodingResponse are retrievable
- **WHEN** `TestCaseResultRepository.findAllByCodingResponseId(codingResponseId)` is called
- **THEN** all `TestCaseResult` rows associated with that `CodingResponse` are returned

#### Scenario: All TestCaseResults for a CodingResponse can be bulk-deleted
- **WHEN** `TestCaseResultRepository.deleteAllByCodingResponseId(codingResponseId)` is called
- **THEN** all `TestCaseResult` rows for that `CodingResponse` are removed and none remain

#### Scenario: TestCaseResult with nullable fields persists without error
- **WHEN** a `TestCaseResult` is saved with `actualOutput`, `executionTimeMs`, `memoryUsedMb`, and `errorMessage` all null
- **THEN** the row persists with those columns as NULL

---

### Requirement: Liquibase Schema for coding_response and test_case_result
The system SHALL create the `coding_response` and `test_case_result` tables via Liquibase changesets, each with a rollback block.

#### Scenario: coding_response table is created by migration
- **WHEN** Liquibase runs the changeset for `coding_response`
- **THEN** a `coding_response` table exists with columns: `id` (UUID PK, FK to `response(id)`), `code` (TEXT NOT NULL), `executed_at` (TIMESTAMPTZ nullable)

#### Scenario: test_case_result table is created by migration
- **WHEN** Liquibase runs the changeset for `test_case_result`
- **THEN** a `test_case_result` table exists with columns: `id` (UUID PK), `coding_response_id` (UUID FK to `coding_response(id)`), `test_case_id` (UUID FK to `test_case(id)`), `passed` (BOOLEAN NOT NULL), `actual_output` (TEXT nullable), `execution_time_ms` (BIGINT nullable), `memory_used_mb` (BIGINT nullable), `error_message` (TEXT nullable), `ordinal` (INTEGER NOT NULL), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ)

#### Scenario: Rollback removes both tables
- **WHEN** the changesets are rolled back
- **THEN** neither `coding_response` nor `test_case_result` tables exist

---

### Requirement: ResponseService Persists CodingResponse on PUT
`ResponseService.saveResponse()` SHALL handle `CodingResponseRequest` by creating or updating a `CodingResponse` row associated with the assessment and question. Multiple saves to the same question overwrite the `code` field (upsert semantics, matching other response types).

#### Scenario: First save creates a CodingResponse row
- **WHEN** `PUT /api/assessments/{id}/responses/{questionId}` is called with `{ "code": "class Main {}" }` and no prior `CodingResponse` exists for that assessment + question
- **THEN** a new `CodingResponse` is persisted with the given code and the response is HTTP 204

#### Scenario: Second save updates the existing CodingResponse
- **WHEN** `PUT /api/assessments/{id}/responses/{questionId}` is called twice with different code values
- **THEN** only one `CodingResponse` row exists after both saves, and its `code` equals the second value

#### Scenario: Saving code on a SUBMITTED assessment returns 409
- **WHEN** a candidate calls the PUT endpoint on an assessment with status `SUBMITTED`
- **THEN** the server responds with HTTP 409

---

### Requirement: On-Demand Execute Wires CodingResponse to CodeExecutionService
`ResponseService.executeCode()` SHALL load the saved `CodingResponse` for the given assessment + question, invoke `CodeExecutionService.execute()`, persist the resulting `TestCaseResult` rows (replacing any prior results), update `executed_at` on the `CodingResponse`, and return a `CodeExecuteResponse`.

#### Scenario: Successful on-demand execution returns results
- **WHEN** `POST /api/assessments/{id}/responses/{questionId}/execute` is called for an in-progress assessment with a saved `CodingResponse`
- **THEN** HTTP 200 is returned with a `CodeExecuteResponse` whose `results` list has one entry per test case and `executedAt` is non-null

#### Scenario: Existing TestCaseResult rows are replaced on re-execution
- **WHEN** the execute endpoint is called twice for the same question
- **THEN** after the second call, only the `TestCaseResult` rows from the second execution remain

#### Scenario: 404 when no code has been saved yet
- **WHEN** `POST /api/assessments/{id}/responses/{questionId}/execute` is called but no `CodingResponse` exists for that question
- **THEN** the server responds with HTTP 404

#### Scenario: 409 when assessment is already submitted
- **WHEN** `POST /api/assessments/{id}/responses/{questionId}/execute` is called on an assessment with status `SUBMITTED`
- **THEN** the server responds with HTTP 409

#### Scenario: 403 when requesting user is not the assigned candidate
- **WHEN** a candidate sends the execute request for an assessment assigned to a different candidate
- **THEN** the server responds with HTTP 403

---

### Requirement: CodingResponse Rows Are Auto-Executed on Assessment Submission
When `POST /api/assessments/{id}/submit` is called, the system SHALL execute all `CodingResponse` rows in the assessment through `CodeExecutionService` and persist the resulting `TestCaseResult` rows. If code execution fails for any response (Judge0 unreachable), the assessment is still transitioned to `SUBMITTED` and the failure is logged.

#### Scenario: Submission triggers execution for each CodingResponse
- **WHEN** a candidate submits an assessment containing two CODING questions with saved code
- **THEN** `CodeExecutionService.execute()` is called once per CODING question and `TestCaseResult` rows are persisted for each

#### Scenario: Judge0 failure during submission does not block the transition
- **WHEN** `CodeExecutionService.execute()` throws an exception during auto-execution on submission
- **THEN** the assessment is still transitioned to `SUBMITTED`, the error is logged, and the submit endpoint returns HTTP 200
