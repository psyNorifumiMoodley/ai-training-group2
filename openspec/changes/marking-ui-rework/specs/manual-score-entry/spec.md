## ADDED Requirements

### Requirement: Score input on TEXT, DOC, and GROUP cards is interactive
The score input rendered on TEXT, DOC, and GROUP question cards SHALL accept a numeric value from the marker and persist it to the backend when the marker commits the value (on blur or Enter).

#### Scenario: Marker enters a score
- **WHEN** the marker types a value into the score input and moves focus away
- **THEN** the system sends `PATCH /api/assessments/{assessmentId}/responses/{responseId}/score` with `{ "score": <value> }`

#### Scenario: Score is clamped to valid range
- **WHEN** the marker enters a value less than 0 or greater than the question's `marks` value
- **THEN** the input rejects or clamps the value to the valid range [0, marks]; the invalid value is not submitted

#### Scenario: Saved score is reflected immediately
- **WHEN** the backend confirms the score update
- **THEN** the `score` field on the corresponding `ResponseReviewItem` in the component is updated without a full page reload

### Requirement: Score summary panel aggregates manual scores
The score summary panel SHALL display a running total of manually entered scores across all TEXT, DOC, and GROUP responses.

#### Scenario: Manual score total shown once any score is entered
- **WHEN** at least one TEXT/DOC/GROUP response has a saved score
- **THEN** the manual score row displays `<sum>/<maxSum> pts` instead of "— pts"

#### Scenario: Manual score row remains blank before any entry
- **WHEN** no TEXT/DOC/GROUP response has a score yet
- **THEN** the manual score row displays "— pts"

### Requirement: Backend exposes score update endpoint
The system SHALL expose `PATCH /api/assessments/{assessmentId}/responses/{responseId}/score` that accepts `{ "score": int }`, validates the score is within [0, question.marks], and persists it to the response record.

#### Scenario: Valid score is accepted
- **WHEN** a valid score within [0, marks] is submitted
- **THEN** the response's score field is updated and HTTP 200 is returned

#### Scenario: Score out of range is rejected
- **WHEN** a score less than 0 or greater than the question's marks is submitted
- **THEN** the endpoint returns HTTP 400 with a validation error message

#### Scenario: Score update on MCQ response is rejected
- **WHEN** the endpoint is called targeting an MCQ response
- **THEN** the endpoint returns HTTP 400 — MCQ scores are set automatically at submission
