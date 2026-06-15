## MODIFIED Requirements

### Requirement: Code Is Auto-Saved on Change
Code typed in the editor SHALL be auto-saved via `CandidateAssessmentService.saveResponse()` using a debounce of **1.5 seconds** after the last keystroke, matching the debounce used by all other response types in `AssessmentTakingComponent`. The auto-save uses `CodingResponseRequest { code }`.

#### Scenario: Code is auto-saved after typing stops
- **WHEN** the candidate stops typing in the code editor for 1.5 seconds
- **THEN** `saveResponse()` is called with the current code string

#### Scenario: Auto-save does not re-trigger execution
- **WHEN** auto-save fires
- **THEN** no call to `executeCode()` is made; only the `PUT /responses/{questionId}` save endpoint is called

---

## ADDED Requirements

### Requirement: CandidateAssessmentService.executeCode() Calls the Execute Endpoint
`CandidateAssessmentService.executeCode(assessmentId, questionId, code)` SHALL make a `POST /api/assessments/{assessmentId}/responses/{questionId}/execute` HTTP request and return an `Observable<CodeExecuteResponse>`. The stub implementation (`return EMPTY`) is not acceptable in production.

#### Scenario: executeCode() sends POST to execute endpoint
- **WHEN** `executeCode(assessmentId, questionId, code)` is called with valid IDs
- **THEN** a `POST` request is sent to `/api/assessments/{assessmentId}/responses/{questionId}/execute` and the returned `Observable` emits the `CodeExecuteResponse`

#### Scenario: executeCode() error propagates to caller
- **WHEN** the execute endpoint returns a non-2xx response
- **THEN** the `Observable` errors and the caller (`CodingAnswerComponent`) is responsible for handling it (resetting loading state)
