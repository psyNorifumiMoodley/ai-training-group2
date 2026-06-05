## ADDED Requirements

### Requirement: MCQ questions are exempt from the finalise feedback gate
The system SHALL allow a marker to finalise an assessment even when MCQ questions have no manual feedback draft. Only TEXT and DOC question types SHALL be checked for non-empty feedback before finalisation is permitted.

#### Scenario: Finalise succeeds with blank MCQ feedback
- **WHEN** a marker calls `POST /api/assessments/{id}/finalise`
- **AND** all TEXT and DOC questions on the assessment have non-empty feedback
- **AND** one or more MCQ questions have a null or empty feedback draft
- **THEN** the system SHALL transition the assessment to `MARKED` status and return 200

#### Scenario: Finalise blocked when TEXT question has empty feedback
- **WHEN** a marker calls `POST /api/assessments/{id}/finalise`
- **AND** at least one TEXT question has a null or empty feedback draft
- **THEN** the system SHALL return 422 and NOT transition the assessment to `MARKED`

#### Scenario: Finalise blocked when DOC question has empty feedback
- **WHEN** a marker calls `POST /api/assessments/{id}/finalise`
- **AND** at least one DOC question has a null or empty feedback draft
- **THEN** the system SHALL return 422 and NOT transition the assessment to `MARKED`

### Requirement: Finalise endpoint accepts overall feedback text
The system SHALL accept an optional JSON request body on `POST /api/assessments/{id}/finalise` containing an `overallFeedback` string field. When omitted or null, the system SHALL treat it as an empty string.

#### Scenario: Finalise with overall feedback provided
- **WHEN** a marker calls `POST /api/assessments/{id}/finalise` with body `{ "overallFeedback": "Strong performance overall." }`
- **AND** all TEXT and DOC questions have non-empty feedback
- **THEN** the system SHALL use the provided text as the content of the candidate feedback email

#### Scenario: Finalise with no request body
- **WHEN** a marker calls `POST /api/assessments/{id}/finalise` with no request body
- **AND** all TEXT and DOC questions have non-empty feedback
- **THEN** the system SHALL succeed and send a feedback email with an empty overall feedback section

### Requirement: Candidate feedback email contains only the overall feedback text
The feedback email sent to the candidate upon finalisation SHALL contain only the marker's overall feedback text. The email SHALL NOT contain per-question UUIDs or individual question feedback drafts.

#### Scenario: Email body uses overall feedback text
- **WHEN** the system sends a feedback email after finalisation
- **AND** the marker provided `overallFeedback = "Great work on the algorithms section."`
- **THEN** the email body SHALL include the text "Great work on the algorithms section."
- **AND** the email body SHALL NOT contain any question UUID strings

#### Scenario: Email body with empty overall feedback
- **WHEN** the system sends a feedback email after finalisation
- **AND** `overallFeedback` was empty or not provided
- **THEN** the email SHALL still be sent with a well-formed body (no UUID output, no crash)
