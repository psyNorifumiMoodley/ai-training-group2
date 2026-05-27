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
The system SHALL prevent any question from appearing in an assessment if the same candidate has already seen that question in a previously submitted assessment within the same calendar year. Questions from prior calendar years are eligible again.

#### Scenario: Previously seen question (same year) is excluded
- **WHEN** an assessment is generated for a candidate who has submitted a prior assessment in the same calendar year containing question X
- **THEN** the generated assessment MUST NOT include question X, even if it was explicitly requested

#### Scenario: Previously seen question (prior year) is eligible
- **WHEN** an assessment is generated for a candidate whose only prior submission containing question X was in a previous calendar year
- **THEN** question X MAY be included in the new assessment

#### Scenario: Assessment generation fails when all requested questions have been seen this year
- **WHEN** all questions in the generation request have been seen by the candidate in submitted assessments within the current calendar year
- **THEN** the response is HTTP 422 with a message indicating no eligible questions remain

---

### Requirement: Doc Question Limit Per Assessment
Each assessment SHALL contain no more doc-type questions than the configured limit. The limit is defined by the server-side property `assessment.doc-question-limit` and MUST NOT be hardcoded.

#### Scenario: Assessment exceeding the configured doc question limit is rejected
- **WHEN** a Marker attempts to generate an assessment with more `DOC`-type questions than the configured limit
- **THEN** the response is HTTP 400 with a message indicating the configured doc question limit has been exceeded

#### Scenario: Assessment at or below the configured limit is accepted
- **WHEN** a Marker generates an assessment with a number of `DOC`-type questions equal to or less than the configured limit
- **THEN** the assessment is created successfully

---

### Requirement: Invitation Token Generation
The system SHALL generate a signed JWT invitation token for each assessment and store it on the assessment record. The token's expiry is tied to the assessment session — it becomes invalid once the assessment time limit has elapsed from first access (`start_time + time_limit_minutes`). No fixed TTL is set at creation time.

#### Scenario: Invitation token is created with the assessment
- **WHEN** an assessment is successfully generated
- **THEN** the assessment record contains a non-null `invitation_token` (a signed JWT) that encodes the assessment UUID; no `exp` claim is set at generation time

#### Scenario: Token is invalidated once the session has expired
- **WHEN** a candidate attempts to access an assessment using a valid invitation token but the assessment's `start_time + time_limit_minutes` is in the past
- **THEN** the response is HTTP 401 with a message indicating the assessment session has expired

#### Scenario: Token is invalidated once the assessment is submitted
- **WHEN** a candidate attempts to access an assessment using a valid invitation token but the assessment status is `SUBMITTED` or `MARKED`
- **THEN** the response is HTTP 409 with a message indicating the assessment has already been submitted

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