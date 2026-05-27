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

### Requirement: Marker Finalises Marking
A Marker SHALL be able to finalise the marking of an assessment, transitioning its status to `MARKED`.

#### Scenario: Finalise marking transitions status
- **WHEN** a Marker sends POST `/api/assessments/{id}/finalise`
- **THEN** the assessment status transitions from `SUBMITTED` to `MARKED` and the response is HTTP 200

#### Scenario: Finalising an already-marked assessment is rejected
- **WHEN** a Marker attempts to finalise an assessment with status `MARKED`
- **THEN** the response is HTTP 409 with a message indicating the assessment is already marked

#### Scenario: Finalising a non-submitted assessment is rejected
- **WHEN** a Marker attempts to finalise an assessment with status `PENDING` or `IN_PROGRESS`
- **THEN** the response is HTTP 409 with a message indicating the assessment has not yet been submitted

---

### Requirement: Result Email Sent on Finalisation
The system SHALL send a result email to the candidate when a Marker finalises marking. The email MUST NOT contain scores or marks.

#### Scenario: Result email sent after finalisation
- **WHEN** a Marker successfully finalises marking
- **THEN** an email is sent to the candidate's registered email address containing the Marker's feedback and comments, with no numerical scores

#### Scenario: Result email is never sent automatically on submission
- **WHEN** a candidate submits an assessment
- **THEN** no result email is sent; the email is only triggered by the Marker explicitly finalising

#### Scenario: Email delivery failure does not roll back finalisation
- **WHEN** the email service is unavailable at the time of finalisation
- **THEN** the assessment status is still updated to `MARKED` and the API returns HTTP 200; the email failure is logged

---

### Requirement: Angular Marking UI
The Angular application SHALL provide a Marker-facing UI for reviewing responses and finalising marking.

#### Scenario: Marker navigates to assessment marking view
- **WHEN** a Marker selects a submitted assessment from the queue
- **THEN** all candidate responses are displayed grouped by question, with MCQ results shown inline and text/doc responses shown with feedback input fields

#### Scenario: Marker finalises and confirms
- **WHEN** a Marker clicks Finalise and confirms the action
- **THEN** the finalisation is sent to the API, the assessment is removed from the pending queue, and a success message is shown

---

### Requirement: Candidate Views Own Results
A Candidate SHALL be able to view the feedback on their own marked assessment after a Marker has finalised.

#### Scenario: Candidate views results after marking
- **WHEN** an authenticated Candidate sends GET `/api/assessments/{id}/results`
- **THEN** the response includes the Marker's feedback per question; no numerical scores are included

#### Scenario: Candidate cannot view results before marking is finalised
- **WHEN** an authenticated Candidate sends GET `/api/assessments/{id}/results` and the assessment status is not `MARKED`
- **THEN** the response is HTTP 404 or HTTP 403