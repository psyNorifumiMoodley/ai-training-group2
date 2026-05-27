## ADDED Requirements

### Requirement: Marker Generates an Assessment for a Candidate
A Marker SHALL be able to generate a tailored assessment and assign it to a specific candidate.

#### Scenario: Successful assessment generation
- **WHEN** a Marker sends POST `/api/assessments` with a candidate UUID, a list of question UUIDs, and a time limit in minutes
- **THEN** the response is HTTP 201 with the new assessment UUID, status `PENDING`, and the assigned candidate's details

#### Scenario: Non-marker cannot generate assessments
- **WHEN** a user with role `CANDIDATE` sends POST `/api/assessments`
- **THEN** the response is HTTP 403

---

### Requirement: No-Repeat Question Rule
The system SHALL prevent any question from appearing in an assessment if the same candidate has already seen that question in a previously submitted assessment.

#### Scenario: Previously seen question is excluded
- **WHEN** an assessment is generated for a candidate who has submitted a prior assessment containing question X
- **THEN** the generated assessment MUST NOT include question X, even if it was explicitly requested

#### Scenario: Assessment generation fails when all requested questions have been seen
- **WHEN** all questions in the generation request have been seen by the candidate in prior submitted assessments
- **THEN** the response is HTTP 422 with a message indicating no eligible questions remain

---

### Requirement: Doc Question Limit Per Assessment
Each assessment SHALL contain at most one doc-type question.

#### Scenario: Assessment with two doc questions is rejected
- **WHEN** a Marker attempts to generate an assessment with two or more `DOC`-type questions in the question list
- **THEN** the response is HTTP 400 with a message indicating the doc question limit of 1 has been exceeded

#### Scenario: Assessment with exactly one doc question is accepted
- **WHEN** a Marker generates an assessment with exactly one `DOC`-type question
- **THEN** the assessment is created successfully

---

### Requirement: Invitation Token Generation
The system SHALL generate a short-lived signed JWT invitation token for each assessment and store it on the assessment record.

#### Scenario: Invitation token is created with the assessment
- **WHEN** an assessment is successfully generated
- **THEN** the assessment record contains a non-null `invitation_token` (a signed JWT) that encodes the assessment UUID and an expiry

#### Scenario: Expired invitation token is rejected
- **WHEN** a candidate attempts to access an assessment using an invitation token whose `exp` claim is in the past
- **THEN** the response is HTTP 401 with a message indicating the token has expired

---

### Requirement: Assessment Invitation Email
The system SHALL send an invitation email to the candidate when an assessment is generated.

#### Scenario: Invitation email sent on assessment creation
- **WHEN** an assessment is successfully generated and persisted
- **THEN** an invitation email is sent to the candidate's registered email address containing a link with the `invitation_token`

#### Scenario: Email delivery failure does not roll back assessment creation
- **WHEN** the email service is unavailable at the time of assessment creation
- **THEN** the assessment is still persisted and the API returns HTTP 201; the email failure is logged

---

### Requirement: Angular Assessment Generation UI
The Angular application SHALL provide a UI for Markers to generate and assign assessments.

#### Scenario: Marker generates assessment via form
- **WHEN** a Marker selects a candidate, selects questions from one or more banks, sets a time limit, and submits
- **THEN** the assessment is created via the API and the Marker is shown the new assessment details including the invitation link

#### Scenario: No-repeat warning shown in UI
- **WHEN** a Marker selects a question that the chosen candidate has already seen
- **THEN** the UI displays a warning indicator on that question before submission