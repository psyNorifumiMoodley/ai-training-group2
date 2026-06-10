## MODIFIED Requirements

### Requirement: GroupQuestionResponse returns inline child definitions
`GroupQuestionResponse` SHALL include `children: List<GroupChildResponse>` read from the `group_question_child` table (owned by the parent `GroupQuestion`). Each `GroupChildResponse` SHALL contain `id` (UUID), `questionText`, `keywords`, and `marks`. `totalMarks` SHALL equal the sum of `children[].marks`.

#### Scenario: GroupQuestionResponse contains children array
- **WHEN** `GET /api/questions/{id}` returns a GROUP question
- **THEN** the response body contains a `children` array (not `followUpQuestions`)

#### Scenario: GroupQuestionResponse does not contain followUpQuestions field
- **WHEN** `GET /api/questions/{id}` returns a GROUP question
- **THEN** the response body does NOT contain a `followUpQuestions` field

#### Scenario: Each child in GroupQuestionResponse has id, questionText, keywords, marks
- **WHEN** `GET /api/questions/{id}` returns a GROUP question with children
- **THEN** each object in the `children` array contains `id` (a non-null UUID), `questionText`, `keywords`, and `marks` read from the persisted `group_question_child` rows

## ADDED Requirements

### Requirement: GroupQuestion children are persisted as owned GroupQuestionChild rows
When a GROUP question is created or updated, the system SHALL persist each entry in `children` as a `group_question_child` row owned by the parent `GroupQuestion`.

#### Scenario: Created group question children are readable
- **WHEN** an authenticated MARKER sends `POST /api/questions` with type `GROUP` and a `children` array containing two entries
- **THEN** `GET /api/questions/{id}` returns both children in the `children` array with their `id`, `questionText`, `keywords`, and `marks`

#### Scenario: Updated group question replaces all children
- **WHEN** an authenticated MARKER sends `PUT /api/questions/{id}` with type `GROUP` and a new `children` list
- **THEN** all previous `group_question_child` rows for that group are deleted and replaced by the new set

#### Scenario: Group question child display order is preserved
- **WHEN** a GROUP question is created with children in a specific order
- **THEN** `GET /api/questions/{id}` returns the `children` array in the same insertion order

### Requirement: Group children do not appear in standalone question listings
Group question children are embedded in their parent and SHALL NOT appear in any `GET /api/questions` listing regardless of type filters. They are not independently retrievable via `GET /api/questions/{id}`.

#### Scenario: GET /api/questions does not return embedded group children
- **WHEN** `GET /api/questions` is called
- **THEN** the paginated results contain only top-level questions (MCQ, MCQ_PLUS, TEXT, DOC, GROUP, CODING); no `GroupQuestionChild` entries appear as independent rows
