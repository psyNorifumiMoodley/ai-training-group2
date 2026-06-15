## ADDED Requirements

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
