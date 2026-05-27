## ADDED Requirements

### Requirement: Token-Gated Assessment Access
A candidate SHALL be able to access their assessment using only the invitation token, without requiring a login session.

#### Scenario: Valid token grants access to assessment
- **WHEN** a GET request is sent to `/api/assessments/access/{token}` with a valid, unexpired invitation token
- **THEN** the response is HTTP 200 with the assessment questions and the remaining time; the assessment status transitions from `PENDING` to `IN_PROGRESS`

#### Scenario: Invalid token is rejected
- **WHEN** a GET request is sent to `/api/assessments/access/{token}` with a malformed or invalid token
- **THEN** the response is HTTP 401

#### Scenario: Accessing an already-submitted assessment is rejected
- **WHEN** a GET request is sent to `/api/assessments/access/{token}` for an assessment with status `SUBMITTED` or `MARKED`
- **THEN** the response is HTTP 409 with a message indicating the assessment has already been submitted

---

### Requirement: Candidate Saves Responses
A candidate SHALL be able to save and update their responses at any time while the assessment is `IN_PROGRESS`. Supported response types are MCQ, text, doc, and question group (which aggregates child responses).

#### Scenario: Save MCQ response
- **WHEN** a candidate sends PUT `/api/assessments/{id}/responses/{questionId}` with selected answer(s) while the assessment is `IN_PROGRESS`
- **THEN** the response is persisted and the API returns HTTP 200

#### Scenario: Save text response
- **WHEN** a candidate sends PUT `/api/assessments/{id}/responses/{questionId}` with a text body
- **THEN** the text response is persisted and the API returns HTTP 200

#### Scenario: Save question group response
- **WHEN** a candidate sends PUT `/api/assessments/{id}/responses/{questionId}` for a group question containing child question responses
- **THEN** the `QuestionGroupResponse` and all its child responses are persisted and the API returns HTTP 200

#### Scenario: Saving responses after submission is rejected
- **WHEN** a candidate attempts to save a response on an assessment with status `SUBMITTED`
- **THEN** the response is HTTP 409

---

### Requirement: Server-Side Timer Enforcement
The server SHALL enforce the assessment time limit and auto-submit the assessment if the candidate does not submit before the deadline.

#### Scenario: Submission within time limit is accepted
- **WHEN** a candidate submits before `start_time + time_limit_minutes`
- **THEN** the submission is accepted and status transitions to `SUBMITTED`

#### Scenario: Server auto-submits on time expiry
- **WHEN** the submission endpoint is called and `now() > start_time + time_limit_minutes`
- **THEN** the server saves all current responses, sets `auto_submitted = true`, transitions status to `SUBMITTED`, and returns HTTP 200

#### Scenario: Frontend countdown is informational only
- **WHEN** the frontend countdown reaches zero but the candidate has not submitted
- **THEN** the server remains the authority on submission time; no automatic client-side submission is required

---

### Requirement: One Submission Per Assessment
A candidate SHALL only be able to submit an assessment once.

#### Scenario: Second submission attempt is rejected
- **WHEN** a candidate attempts to submit an assessment that already has status `SUBMITTED`
- **THEN** the response is HTTP 409 with a message indicating the assessment has already been submitted

---

### Requirement: MCQ Auto-Marking on Submission
The system SHALL automatically mark all MCQ responses immediately upon submission.

#### Scenario: Single-answer MCQ marked correctly
- **WHEN** an assessment is submitted and a candidate's MCQ response matches the single correct answer
- **THEN** the MCQ response is marked as correct

#### Scenario: Multi-answer MCQ marked correctly
- **WHEN** an assessment is submitted and the candidate's selected answers exactly match all correct answers
- **THEN** the MCQ response is marked as correct

#### Scenario: Partial multi-answer MCQ marked incorrect
- **WHEN** an assessment is submitted and the candidate's selected answers do not exactly match all correct answers
- **THEN** the MCQ response is marked as incorrect

---

### Requirement: Angular Assessment Taking UI
The Angular application SHALL provide a candidate-facing UI for taking an assessment accessed via invitation token.

#### Scenario: Candidate views questions after token access
- **WHEN** a candidate opens the invitation link
- **THEN** the assessment questions are displayed along with a countdown timer showing remaining time

#### Scenario: Countdown timer reflects server time
- **WHEN** the candidate loads the assessment
- **THEN** the countdown is initialised from the `remainingSeconds` returned by the server, not from the client clock

#### Scenario: Candidate submits assessment
- **WHEN** a candidate clicks Submit and confirms
- **THEN** the submission is sent to the API and the candidate is shown a confirmation screen