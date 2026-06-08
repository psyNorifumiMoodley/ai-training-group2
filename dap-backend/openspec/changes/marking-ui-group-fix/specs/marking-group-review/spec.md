## ADDED Requirements

### Requirement: Group responses are not duplicated as flat items
The `GET /api/assessments/{id}/responses` endpoint SHALL return only top-level responses. Child responses that belong to a `QuestionGroupResponse` SHALL NOT appear as independent items in the response list.

#### Scenario: Assessment with a grouped question returns correct item count
- **WHEN** a marker requests `GET /api/assessments/{id}/responses`
- **AND** the assessment has 1 grouped question with 2 follow-up questions and 8 other questions
- **THEN** the response body SHALL contain exactly 9 items (not 11)

#### Scenario: Standalone text questions are still returned
- **WHEN** a marker requests `GET /api/assessments/{id}/responses`
- **AND** the assessment has both standalone text questions and grouped questions
- **THEN** standalone text questions SHALL appear as independent items
- **AND** follow-up questions inside groups SHALL NOT appear as independent items

### Requirement: GROUP review item contains nested child items
A `ResponseReviewItem` with `questionType = "GROUP"` SHALL include a `childItems` list containing one entry per follow-up question in the group. Each child item SHALL carry the follow-up question body and the candidate's text answer.

#### Scenario: GROUP item has childItems populated
- **WHEN** the response list for an assessment is returned
- **AND** one item has `questionType = "GROUP"`
- **THEN** that item's `childItems` SHALL be a non-empty list
- **AND** each entry in `childItems` SHALL have `questionType = "TEXT"` and a non-null `questionBody`

#### Scenario: Non-GROUP items have no childItems
- **WHEN** the response list for an assessment is returned
- **AND** an item has `questionType` of `"MCQ"`, `"TEXT"`, or `"DOC"`
- **THEN** that item's `childItems` SHALL be null or absent

### Requirement: Marker review screen renders GROUP card with nested child rows
The marker review screen SHALL display a GROUP response as a single card. Inside the card, each follow-up question SHALL be rendered as a read-only row showing the question body and the candidate's answer.

#### Scenario: GROUP card shows follow-up answers
- **WHEN** a marker views the response review screen for an assessment containing a group question
- **THEN** the group appears as one card (not multiple separate cards)
- **AND** each follow-up question and its answer is visible inside the group card

#### Scenario: GROUP card has a single feedback field
- **WHEN** a marker views a GROUP card on the review screen
- **THEN** there is exactly one feedback textarea on the card (at the group level)
- **AND** there are no individual feedback fields for each follow-up question
