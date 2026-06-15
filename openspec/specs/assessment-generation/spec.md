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

---

### Requirement: CODING Questions Are Eligible for Manual Selection in Assessment Generation
The Angular question picker in `AssessmentGenerateComponent` SHALL display `CODING` question type questions alongside MCQ, TEXT, DOC, and GROUP questions when the marker is manually selecting questions for an assessment. The CODING type SHALL be included in the question type filter dropdown.

#### Scenario: Question picker shows CODING questions
- **WHEN** a marker opens the question picker within the assessment generation flow for a question bank that contains CODING questions
- **THEN** CODING questions appear in the list with their language badge and test case count visible

#### Scenario: Marker can filter by CODING type
- **WHEN** a marker selects "Coding" in the question type filter dropdown
- **THEN** only CODING questions are shown in the picker

---

### Requirement: CODING Questions Count Against the Combined Doc+Coding Slot Limit
The existing `assessment.doc-question-limit` property governs the maximum number of DOC **and** CODING questions that may appear in a single assessment. The Angular generation UI SHALL enforce and display this combined limit when the marker is selecting questions manually.

#### Scenario: Adding a CODING question beyond the combined limit is rejected in the UI
- **WHEN** the marker has already selected the maximum allowed number of DOC and CODING questions combined
- **THEN** the "Add" action for any additional DOC or CODING question is disabled, with a tooltip explaining the combined limit

#### Scenario: Backend rejects an assessment request that exceeds the combined limit
- **WHEN** `POST /api/assessments` is sent with more than `assessment.doc-question-limit` combined DOC and CODING questions
- **THEN** the server responds with HTTP 400 and an error message indicating the limit
