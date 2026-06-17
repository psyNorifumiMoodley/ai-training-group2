## ADDED Requirements

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
