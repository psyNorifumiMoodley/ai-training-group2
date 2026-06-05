### Requirement: Manual selection confirmation shown as modal overlay
After the user completes manual question selection and the backend confirms assessment creation, the system SHALL display the success confirmation (candidate name, question count, time limit, invitation link) as a visually distinct modal/overlay card consistent with the auto-generate success style. The question count SHALL always display 10, reflecting the system-fixed total.

#### Scenario: Success state shown after manual selection submit
- **WHEN** the user submits their manually selected questions and the backend returns success
- **THEN** the question-selection page transitions to a full-screen modal overlay showing the AssessmentConfirmationComponent with candidateName, questionCount=10, timeLimitMinutes, and invitationLink

#### Scenario: User can copy invitation link from confirmation
- **WHEN** the confirmation modal is displayed after manual selection
- **THEN** the invitation link is shown with a copy-to-clipboard button

#### Scenario: User can close confirmation and return to assessments list
- **WHEN** the user clicks close/done on the confirmation modal
- **THEN** they are navigated to `/assessments` and the table reloads
