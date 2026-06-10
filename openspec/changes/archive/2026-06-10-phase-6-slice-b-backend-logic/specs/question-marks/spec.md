## MODIFIED Requirements

### Requirement: TextQuestionResponse and DocQuestionResponse include marks
`TextQuestionResponse` and `DocQuestionResponse` SHALL each include `marks: int` reflecting the persisted mark value for that question, read from the `text_question.marks` and `doc_question.marks` database columns respectively.

#### Scenario: TextQuestionResponse includes marks field
- **WHEN** `GET /api/questions/{id}` returns a TEXT question
- **THEN** the response body contains a `marks` field equal to the value that was supplied at creation time

#### Scenario: DocQuestionResponse includes marks field
- **WHEN** `GET /api/questions/{id}` returns a DOC question
- **THEN** the response body contains a `marks` field equal to the value that was supplied at creation time

## ADDED Requirements

### Requirement: TextQuestion marks are persisted to the database
The system SHALL store the `marks` value from `TextQuestionRequest` in the `text_question.marks` column when creating or updating a text question.

#### Scenario: Created text question marks are readable
- **WHEN** an authenticated MARKER sends `POST /api/questions` with type `TEXT` and `marks: 3`
- **THEN** `GET /api/questions/{id}` subsequently returns `marks: 3` in the response

#### Scenario: Updated text question marks replace the stored value
- **WHEN** an authenticated MARKER sends `PUT /api/questions/{id}` with type `TEXT` and `marks: 5`
- **THEN** `GET /api/questions/{id}` subsequently returns `marks: 5`

### Requirement: DocQuestion marks are persisted to the database
The system SHALL store the `marks` value from `DocQuestionRequest` in the `doc_question.marks` column when creating or updating a doc question.

#### Scenario: Created doc question marks are readable
- **WHEN** an authenticated MARKER sends `POST /api/questions` with type `DOC` and `marks: 2`
- **THEN** `GET /api/questions/{id}` subsequently returns `marks: 2` in the response

### Requirement: GroupQuestion totalMarks reflects sum of persisted child marks
`GroupQuestionResponse.totalMarks` SHALL be computed at response-mapping time as the sum of all `GroupQuestionChild.marks` values loaded from the `group_question_child` table.

#### Scenario: GroupQuestionResponse totalMarks equals sum of children marks
- **WHEN** `GET /api/questions/{id}` returns a GROUP question with three children having marks 1, 2, 3
- **THEN** `totalMarks` in the response equals 6

#### Scenario: GroupQuestion with no children has totalMarks zero
- **WHEN** `GET /api/questions/{id}` returns a GROUP question with an empty `children` array
- **THEN** `totalMarks` equals 0
