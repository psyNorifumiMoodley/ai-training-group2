## ADDED Requirements

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
