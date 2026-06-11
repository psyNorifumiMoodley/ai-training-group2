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

## Requirement: AssessmentRequest scopes question pool by QuestionBank IDs
`AssessmentRequest` SHALL include `questionBankIds: List<UUID>` (min 1 entry) instead of a category filter. Assessment auto-generation SHALL draw questions from the pool of questions belonging to any of the specified QBs. Manual question selection SHALL show only questions from the specified QBs.

#### Scenario: AssessmentRequest without questionBankIds is rejected
- **WHEN** `POST /api/assessments` is sent without `questionBankIds` or with an empty list
- **THEN** the system returns HTTP 400

#### Scenario: AssessmentRequest with one or more QB IDs is accepted
- **WHEN** `POST /api/assessments` is sent with `questionBankIds` containing at least one valid QB UUID
- **THEN** the system proceeds with assessment generation scoped to that QB set

## Requirement: McqPlusQuestion counts as one MCQ slot in assessment composition
When generating an assessment, `McqPlusQuestion` instances SHALL be eligible to fill MCQ composition slots. An assessment with N MCQ slots may be filled by any combination of `McqQuestion` and `McqPlusQuestion` instances.

#### Scenario: McqPlusQuestion fills an MCQ composition slot
- **WHEN** assessment auto-generation is running and an `McqPlusQuestion` is selected
- **THEN** it counts as one MCQ slot consumed (same as a plain `McqQuestion`)

#### Scenario: MCQ composition quota can be entirely filled by McqPlusQuestion instances
- **WHEN** the question pool within the selected QBs contains only `McqPlusQuestion` instances for the MCQ category
- **THEN** the MCQ quota can be fulfilled entirely by `McqPlusQuestion` instances
