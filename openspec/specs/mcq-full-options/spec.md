## ADDED Requirements

### Requirement: MCQ cards display all answer options
The MCQ `ResponseReviewItem` card SHALL render every option that was presented to the candidate during the assessment, not only the option(s) they selected.

#### Scenario: All options are listed
- **WHEN** an MCQ card is rendered
- **THEN** every entry in `answer.allOptions` is shown as a separate row, in the order they appear in the list

#### Scenario: Correct answer is highlighted in green
- **WHEN** an option matches an entry in `answer.correctAnswers`
- **THEN** that option row is styled with a green background and a checkmark icon (regardless of whether the candidate selected it)

#### Scenario: Candidate's selected answer is marked
- **WHEN** an option matches an entry in `answer.selectedAnswers`
- **THEN** that option row displays a filled radio/circle indicator to show it was the candidate's choice

#### Scenario: Correct selection is shown with both markers
- **WHEN** the candidate selected an option that is also a correct answer
- **THEN** the row shows both the green correct styling AND the candidate selection indicator

#### Scenario: Incorrect selection is shown distinctly
- **WHEN** the candidate selected an option that is NOT a correct answer
- **THEN** the row shows the candidate selection indicator but uses a red/neutral background to indicate the wrong pick

### Requirement: Backend populates MCQ option lists
The `ResponseReviewItem` payload for an MCQ response SHALL include `allOptions` (all options presented) and `correctAnswers` (the correct option(s)) sourced from the `McqQuestion` entity.

#### Scenario: Review endpoint returns full option context
- **WHEN** `GET /api/assessments/{id}/responses/review` is called
- **THEN** each MCQ item in the response includes non-empty `allOptions` and `correctAnswers` arrays
