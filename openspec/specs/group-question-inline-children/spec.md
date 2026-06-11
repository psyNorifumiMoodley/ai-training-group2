## Requirement: GroupQuestionRequest uses inline child definitions, not ID references
`GroupQuestionRequest` SHALL accept `children: List<GroupChildRequest>` (min 1 entry) instead of `followUpQuestionIds: List<UUID>`. Each `GroupChildRequest` SHALL contain `questionText` (required, non-blank), optional `keywords`, and `marks` (required, min 1). No lookup of existing `TextQuestion` entities occurs during group creation.

#### Scenario: GroupQuestionRequest with children list is accepted
- **WHEN** `POST /api/questions` is sent with type `GROUP` and a valid `children` array containing at least one entry
- **THEN** the system returns HTTP 201

#### Scenario: GroupQuestionRequest with empty children list is rejected
- **WHEN** `POST /api/questions` is sent with type `GROUP` and `children: []`
- **THEN** the system returns HTTP 400

#### Scenario: GroupQuestionRequest with missing children field is rejected
- **WHEN** `POST /api/questions` is sent with type `GROUP` and no `children` field
- **THEN** the system returns HTTP 400

#### Scenario: GroupChildRequest with blank questionText is rejected
- **WHEN** `POST /api/questions` is sent with type `GROUP` and a child entry where `questionText` is blank
- **THEN** the system returns HTTP 400

#### Scenario: GroupChildRequest with marks less than 1 is rejected
- **WHEN** `POST /api/questions` is sent with type `GROUP` and a child entry where `marks: 0`
- **THEN** the system returns HTTP 400

#### Scenario: followUpQuestionIds field is not recognised
- **WHEN** `POST /api/questions` is sent with type `GROUP` and a `followUpQuestionIds` field (old contract)
- **THEN** the field is ignored by Jackson deserialization; `children` must still be provided

## Requirement: GroupQuestion children are persisted as owned GroupQuestionChild rows
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

## Requirement: GroupQuestionResponse returns inline child definitions read from the database
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

## Requirement: Group children do not appear in standalone question listings
Group question children are embedded in their parent and SHALL NOT appear in any `GET /api/questions` listing regardless of type filters. They are not independently retrievable via `GET /api/questions/{id}`.

#### Scenario: GET /api/questions does not return embedded group children
- **WHEN** `GET /api/questions` is called
- **THEN** the paginated results contain only top-level questions (MCQ, MCQ_PLUS, TEXT, DOC, GROUP, CODING); no `GroupQuestionChild` entries appear as independent rows
