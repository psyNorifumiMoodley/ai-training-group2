## ADDED Requirements

### Requirement: Marker Views Submitted Assessments
A Marker SHALL be able to retrieve a list of submitted assessments awaiting marking.

#### Scenario: List submitted assessments
- **WHEN** an authenticated Marker sends GET `/api/assessments?status=SUBMITTED`
- **THEN** the response is HTTP 200 with a paginated list of submitted assessments including candidate name and submission time

#### Scenario: Non-marker cannot access marking queue
- **WHEN** a user with role `CANDIDATE` sends GET `/api/assessments?status=SUBMITTED`
- **THEN** the response is HTTP 403

---

### Requirement: Marker Reviews Individual Responses
A Marker SHALL be able to view and annotate each text and doc response on a submitted assessment.

#### Scenario: Marker views assessment responses
- **WHEN** a Marker sends GET `/api/assessments/{id}/responses`
- **THEN** the response includes all candidate responses (MCQ, text, doc) with MCQ responses already marked and text/doc responses awaiting review

#### Scenario: Marker adds feedback to a text response
- **WHEN** a Marker sends PATCH `/api/assessments/{id}/responses/{responseId}` with a `feedback` field
- **THEN** the feedback is persisted and the response is HTTP 200

---

### Requirement: Auto-Generated Editable Feedback
When a Marker opens a submitted assessment for review, the system SHALL auto-generate an initial feedback draft for every question. The Marker MUST be able to edit any draft before finalising.

#### Scenario: MCQ feedback is auto-generated on review
- **WHEN** a Marker opens a submitted assessment
- **THEN** each MCQ response has a system-generated feedback draft (e.g. "Correct" or "Incorrect — please review this topic") pre-populated and editable

#### Scenario: Text and doc feedback drafts are empty
- **WHEN** a Marker opens a submitted assessment
- **THEN** each text and doc response has an empty editable feedback field that the Marker MUST fill before finalising

#### Scenario: Marker edits a feedback draft
- **WHEN** a Marker sends PATCH `/api/assessments/{id}/feedback/{questionId}` with updated feedback text
- **THEN** the feedback draft is updated and the API returns HTTP 200

---

### Requirement: Marker Finalises Marking
A Marker SHALL be able to finalise the marking of an assessment once all feedback entries are complete, transitioning its status to `MARKED`.

#### Scenario: Finalise marking transitions status
- **WHEN** a Marker sends POST `/api/assessments/{id}/finalise` and all text/doc feedback entries are non-empty
- **THEN** the assessment status transitions from `SUBMITTED` to `MARKED` and the response is HTTP 200

#### Scenario: Finalisation blocked when text/doc feedback is missing
- **WHEN** a Marker attempts to finalise and one or more text or doc feedback entries are still empty
- **THEN** the response is HTTP 400 with a message listing the questions with missing feedback

#### Scenario: Finalising an already-marked assessment is rejected
- **WHEN** a Marker attempts to finalise an assessment with status `MARKED`
- **THEN** the response is HTTP 409 with a message indicating the assessment is already marked

#### Scenario: Finalising a non-submitted assessment is rejected
- **WHEN** a Marker attempts to finalise an assessment with status `PENDING` or `IN_PROGRESS`
- **THEN** the response is HTTP 409 with a message indicating the assessment has not yet been submitted

---

### Requirement: Feedback Email Sent on Finalisation
The system SHALL send a feedback email to the candidate when a Marker finalises marking. The email MUST contain only the curated per-question feedback text and MUST NOT contain any scores, marks, or correctness indicators beyond the Marker's written feedback.

#### Scenario: Feedback email sent after finalisation
- **WHEN** a Marker successfully finalises marking
- **THEN** an email is sent to the candidate's registered email address containing the per-question feedback text as edited by the Marker — no numerical scores or raw correct/incorrect flags

#### Scenario: Feedback email is never sent automatically on submission
- **WHEN** a candidate submits an assessment
- **THEN** no feedback email is sent; the email is only triggered by the Marker explicitly finalising

#### Scenario: Email delivery failure does not roll back finalisation
- **WHEN** the email service is unavailable at the time of finalisation
- **THEN** the assessment status is still updated to `MARKED` and the API returns HTTP 200; the email failure is logged

---

### Requirement: Angular Marking UI
The Angular application SHALL provide a Marker-facing UI for reviewing responses, editing feedback, and finalising marking.

#### Scenario: Marker navigates to assessment marking view
- **WHEN** a Marker selects a submitted assessment from the queue
- **THEN** all candidate responses are displayed grouped by question; MCQ rows show the auto-generated feedback draft (editable); text and doc rows show the candidate's answer alongside an empty editable feedback field

#### Scenario: Finalise button blocked until all feedback is complete
- **WHEN** any text or doc feedback field is still empty
- **THEN** the Finalise button is disabled and a summary of incomplete questions is shown

#### Scenario: Marker finalises and confirms
- **WHEN** all feedback is complete and the Marker confirms the finalise action
- **THEN** the finalisation is sent to the API, the assessment is removed from the pending queue, and a success message is shown

---

### Requirement: Candidate Views Own Feedback
A Candidate SHALL be able to view the per-question feedback on their own marked assessment after a Marker has finalised. No scores or marks are visible.

#### Scenario: Candidate views feedback after marking
- **WHEN** an authenticated Candidate sends GET `/api/assessments/{id}/feedback`
- **THEN** the response includes the Marker's per-question feedback text; no numerical scores or raw correct/incorrect data are included

#### Scenario: Candidate cannot view feedback before marking is finalised
- **WHEN** an authenticated Candidate sends GET `/api/assessments/{id}/feedback` and the assessment status is not `MARKED`
- **THEN** the response is HTTP 403