### Requirement: Candidate Sees a Code Editor for CODING Questions
During an assessment, each CODING question SHALL render a `CodingAnswerComponent` (standalone, `OnPush`) that displays: the question text, the programming language label (e.g. "Java"), a `<textarea>` code editor pre-populated with any previously saved code, a "Run" button, and a test case results panel below the editor. No external editor library (Monaco, CodeMirror) is required — a styled `<textarea>` is sufficient.

#### Scenario: CODING question renders coding answer panel
- **WHEN** a candidate accesses an assessment containing a CODING question
- **THEN** the question renders with a textarea, a language label matching the question's language, and a "Run" button

#### Scenario: Previously saved code is pre-populated in the editor
- **WHEN** the candidate has previously saved code for the question (e.g. after page reload via `alreadyStarted` path)
- **THEN** the textarea is pre-populated with the saved code from the assessment access response

---

### Requirement: Candidate Can Run Code and See Test Case Results
Clicking "Run" SHALL call `CandidateAssessmentService.executeCode()` and display the returned `TestCaseResult` array in the results panel below the editor. Each row in the results panel shows: test case ordinal, pass/fail badge, actual output (truncated if > 500 chars), and error message (if present). The "Run" button is disabled and shows a loading state while execution is in progress.

#### Scenario: Run button triggers execution and shows results
- **WHEN** the candidate clicks "Run" on a CODING question with code in the editor
- **THEN** the button enters a loading state, `executeCode()` is called, and on response the results panel populates with one row per test case

#### Scenario: All test cases passing shows a success summary
- **WHEN** all `TestCaseResult` entries have `passed = true`
- **THEN** a green "All tests passed" summary badge is shown above the results table

#### Scenario: One or more test cases failing shows a failure summary
- **WHEN** at least one `TestCaseResult` has `passed = false`
- **THEN** an amber or red summary badge shows "X / Y tests passed" above the results table

#### Scenario: Run button is disabled during in-flight execution
- **WHEN** an execute request is in flight
- **THEN** the "Run" button is disabled and shows a spinner; clicking it again has no effect

---

### Requirement: Code Is Auto-Saved on Change
Code typed in the editor SHALL be auto-saved via `CandidateAssessmentService.saveResponse()` using a debounce of 1.5 seconds after the last keystroke, matching the debounce used by all other response types in `AssessmentTakingComponent`. The auto-save uses `CodingResponseRequest { code }`.

#### Scenario: Code is auto-saved after typing stops
- **WHEN** the candidate stops typing in the code editor for 1.5 seconds
- **THEN** `saveResponse()` is called with the current code string

#### Scenario: Auto-save does not re-trigger execution
- **WHEN** auto-save fires
- **THEN** no call to `executeCode()` is made; only the `PUT /responses/{questionId}` save endpoint is called

---

### Requirement: CandidateAssessmentService.executeCode() Calls the Execute Endpoint
`CandidateAssessmentService.executeCode(assessmentId, questionId, code)` SHALL make a `POST /api/assessments/{assessmentId}/responses/{questionId}/execute` HTTP request and return an `Observable<CodeExecuteResponse>`. The stub implementation (`return EMPTY`) is not acceptable in production.

#### Scenario: executeCode() sends POST to execute endpoint
- **WHEN** `executeCode(assessmentId, questionId, code)` is called with valid IDs
- **THEN** a `POST` request is sent to `/api/assessments/{assessmentId}/responses/{questionId}/execute` and the returned `Observable` emits the `CodeExecuteResponse`

#### Scenario: executeCode() error propagates to caller
- **WHEN** the execute endpoint returns a non-2xx response
- **THEN** the `Observable` errors and the caller (`CodingAnswerComponent`) is responsible for handling it (resetting loading state)

---

### Requirement: Assessment-Taking Submission Flow Handles Coding Questions
The existing submit confirmation dialog and post-submit confirmation screen SHALL continue to work unchanged when the assessment contains CODING questions. No additional UI prompt is required to inform the candidate that their code will be executed on submission — submission behaviour is already documented in the invitation flow.

#### Scenario: Submit flow proceeds normally with coding questions present
- **WHEN** a candidate clicks "Submit" on an assessment containing CODING questions
- **THEN** the existing confirmation dialog appears; on confirm, `submitAssessment()` is called; no coding-specific UI step is injected
