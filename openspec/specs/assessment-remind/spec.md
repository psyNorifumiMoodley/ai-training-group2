### Requirement: Remind endpoint sends a distinct reminder email
The system SHALL expose `POST /api/assessments/{id}/remind` accessible to MARKER and ADMIN roles. It SHALL send a **reminder** email (distinct from the original invitation) to the candidate using the assessment's current invitation token. It SHALL only be allowed when the assessment status is PENDING.

The reminder email SHALL use:
- **Subject:** `Reminder: You have a pending technical assessment`
- **Body** (plain text):
  ```
  Dear {candidateName},

  This is a friendly reminder that you have a technical assessment waiting for you.

  Please complete your assessment at your earliest convenience using the link below:

  {invitationLink}

  This link is unique to you. Please do not share it.

  If you have already started your assessment, you can use the same link to continue.

  The Developer Assessment Platform Team
  ```

`EmailService` SHALL be extended with a new `sendReminder(String toEmail, String candidateName, String invitationLink)` method. The implementation SHALL follow the same `@Async` and error-handling pattern as `sendInvitation`.

#### Scenario: Successful remind
- **WHEN** an Admin or Marker sends POST `/api/assessments/{id}/remind` for a PENDING assessment
- **THEN** the system sends a distinct reminder email (via `EmailService.sendReminder`) to the candidate and returns HTTP 204

#### Scenario: Remind rejected for non-PENDING assessment
- **WHEN** an Admin or Marker sends POST `/api/assessments/{id}/remind` for an assessment that is not PENDING
- **THEN** the system returns HTTP 409 Conflict with an appropriate error message

#### Scenario: Remind returns 404 for unknown assessment
- **WHEN** POST `/api/assessments/{id}/remind` is called with an ID that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Invitation link included in assessment list response
`AssessmentSummaryResponse` SHALL include an `invitationLink` field containing the full candidate-facing URL (`{frontendBaseUrl}/assessment/access/{token}`). The field SHALL always be non-null, as every assessment is assigned an invitation token at creation time.

#### Scenario: Invitation link present in list
- **WHEN** GET `/api/assessments` is called
- **THEN** each item in the response includes a non-null `invitationLink` field
