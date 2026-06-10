## Requirement: TextQuestion and DocQuestion requests require a marks field
`TextQuestionRequest` and `DocQuestionRequest` SHALL each include a required integer `marks` field with a minimum value of 1. Requests missing this field or with `marks < 1` SHALL be rejected with HTTP 400.

#### Scenario: TextQuestionRequest without marks is rejected
- **WHEN** `POST /api/questions` is sent with type `TEXT` and `marks` absent
- **THEN** the system returns HTTP 400

#### Scenario: TextQuestionRequest with marks zero is rejected
- **WHEN** `POST /api/questions` is sent with type `TEXT` and `marks: 0`
- **THEN** the system returns HTTP 400

#### Scenario: DocQuestionRequest without marks is rejected
- **WHEN** `POST /api/questions` is sent with type `DOC` and `marks` absent
- **THEN** the system returns HTTP 400

#### Scenario: TextQuestionRequest with marks 1 or greater is accepted
- **WHEN** `POST /api/questions` is sent with type `TEXT` and `marks: 5`
- **THEN** the system returns HTTP 201

## Requirement: TextQuestionResponse and DocQuestionResponse include marks
`TextQuestionResponse` and `DocQuestionResponse` SHALL each include `marks: int` reflecting the configured mark value for that question.

#### Scenario: TextQuestionResponse includes marks field
- **WHEN** `GET /api/questions/{id}` returns a TEXT question
- **THEN** the response body contains a `marks` field with a positive integer value

#### Scenario: DocQuestionResponse includes marks field
- **WHEN** `GET /api/questions/{id}` returns a DOC question
- **THEN** the response body contains a `marks` field with a positive integer value

## Requirement: MCQ questions are always worth exactly 1 mark
MCQ questions SHALL NOT carry a configurable marks field. The mark value for any MCQ is the constant 1, known at the type level and not stored in the DB.

#### Scenario: McqQuestionResponse does not include a marks field
- **WHEN** `GET /api/questions/{id}` returns an MCQ question
- **THEN** the response body does NOT contain a top-level `marks` field

## Requirement: GroupQuestion totalMarks is computed as the sum of child marks
`GroupQuestionResponse` SHALL include `totalMarks: int` equal to the sum of `children[].marks`. This value is computed by the service and is not stored.

#### Scenario: GroupQuestionResponse includes totalMarks
- **WHEN** `GET /api/questions/{id}` returns a GROUP question
- **THEN** the response body contains `totalMarks` equal to the sum of all `children[i].marks` values

## Requirement: McqPlusQuestion total marks is 1 plus followUpMarks
`McqPlusQuestionResponse` SHALL include `totalMarks: int` equal to `1 + followUpMarks`.

#### Scenario: McqPlusQuestionResponse totalMarks is 1 plus followUpMarks
- **WHEN** `GET /api/questions/{id}` returns an MCQ_PLUS question with `followUpMarks: 3`
- **THEN** `totalMarks` in the response equals 4
