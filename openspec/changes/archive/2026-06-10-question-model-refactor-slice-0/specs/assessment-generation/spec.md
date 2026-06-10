## ADDED Requirements

### Requirement: AssessmentRequest scopes question pool by QuestionBank IDs
`AssessmentRequest` SHALL include `questionBankIds: List<UUID>` (min 1 entry) instead of a category filter. Assessment auto-generation SHALL draw questions from the pool of questions belonging to any of the specified QBs. Manual question selection SHALL show only questions from the specified QBs.

#### Scenario: AssessmentRequest without questionBankIds is rejected
- **WHEN** `POST /api/assessments` is sent without `questionBankIds` or with an empty list
- **THEN** the system returns HTTP 400

#### Scenario: AssessmentRequest with one or more QB IDs is accepted
- **WHEN** `POST /api/assessments` is sent with `questionBankIds` containing at least one valid QB UUID
- **THEN** the system proceeds with assessment generation scoped to that QB set

### Requirement: McqPlusQuestion counts as one MCQ slot in assessment composition
When generating an assessment, `McqPlusQuestion` instances SHALL be eligible to fill MCQ composition slots. An assessment with N MCQ slots may be filled by any combination of `McqQuestion` and `McqPlusQuestion` instances.

#### Scenario: McqPlusQuestion fills an MCQ composition slot
- **WHEN** assessment auto-generation is running and an `McqPlusQuestion` is selected
- **THEN** it counts as one MCQ slot consumed (same as a plain `McqQuestion`)

#### Scenario: MCQ composition quota can be entirely filled by McqPlusQuestion instances
- **WHEN** the question pool within the selected QBs contains only `McqPlusQuestion` instances for the MCQ category
- **THEN** the MCQ quota can be fulfilled entirely by `McqPlusQuestion` instances
