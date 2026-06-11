## Requirement: MCQ_PLUS is a recognised question type in the API
The system SHALL accept `"type": "MCQ_PLUS"` in `POST /api/questions` requests and return `"type": "MCQ_PLUS"` in question responses. The type SHALL be included in the `QuestionRequest` sealed interface (backend) and `QuestionType` union (frontend).

#### Scenario: POST with type MCQ_PLUS returns 201
- **WHEN** an authenticated MARKER sends `POST /api/questions` with `"type": "MCQ_PLUS"` and a valid `McqPlusQuestionRequest` body
- **THEN** the system returns HTTP 201 with a persisted `McqPlusQuestionResponse` containing the generated UUID and all submitted fields

#### Scenario: Unknown type still returns 400
- **WHEN** `POST /api/questions` is sent with `"type": "MCQ_PLUS_EXTRA"` (not a valid type)
- **THEN** the system returns HTTP 400

#### Scenario: Frontend QuestionType includes MCQ_PLUS
- **WHEN** the Angular `question.model.ts` is compiled
- **THEN** `QuestionType` resolves to `'MCQ' | 'MCQ_PLUS' | 'TEXT' | 'DOC' | 'GROUP' | 'CODING'` without TypeScript errors

## Requirement: McqPlusQuestionRequest carries all MCQ fields plus a follow-up text question
`McqPlusQuestionRequest` SHALL include `questionBankIds`, `question`, `options`, `correctAnswers`, `followUpQuestion`, optional `followUpKeywords`, and `followUpMarks` (min 1). All MCQ validation rules (correctAnswers non-empty, correctAnswers ⊆ options) apply unchanged.

#### Scenario: McqPlusQuestionRequest with missing followUpQuestion is rejected at validation
- **WHEN** `POST /api/questions` is sent with type `MCQ_PLUS` and `followUpQuestion` absent or blank
- **THEN** the system returns HTTP 400

#### Scenario: McqPlusQuestionRequest with followUpMarks less than 1 is rejected
- **WHEN** `POST /api/questions` is sent with type `MCQ_PLUS` and `followUpMarks: 0`
- **THEN** the system returns HTTP 400

#### Scenario: McqPlusQuestionRequest with correctAnswer not in options is rejected
- **WHEN** `POST /api/questions` is sent with type `MCQ_PLUS` and a `correctAnswers` entry not present in `options`
- **THEN** the system returns HTTP 400

## Requirement: McqPlusQuestion is persisted to the mcq_plus_question table
When a `POST /api/questions` request with type `MCQ_PLUS` is successful, the system SHALL write a row to `mcq_plus_question` containing `question`, `options`, `correct_answers`, `follow_up_question`, `follow_up_keywords`, and `follow_up_marks`.

#### Scenario: Created MCQ_PLUS question is retrievable
- **WHEN** an authenticated MARKER sends `POST /api/questions` with type `MCQ_PLUS` and valid body
- **THEN** `GET /api/questions/{id}` returns a `McqPlusQuestionResponse` with all submitted fields including `followUpQuestion`, `followUpMarks`, and `totalMarks`

#### Scenario: McqPlusQuestionResponse totalMarks equals 1 plus followUpMarks
- **WHEN** `GET /api/questions/{id}` returns a persisted MCQ_PLUS question with `followUpMarks: 4`
- **THEN** `totalMarks` in the response equals 5

#### Scenario: MCQ_PLUS question appears in question bank listing
- **WHEN** `GET /api/questions?questionBankId=<id>` is called and an MCQ_PLUS question belongs to that bank
- **THEN** the question appears in the paginated results with `"type": "MCQ_PLUS"`

## Requirement: McqPlusQuestionResponse carries all MCQ fields plus follow-up and totalMarks
`McqPlusQuestionResponse` SHALL include `id`, `questionBanks`, `question`, `options`, `correctAnswers`, `multiCorrect`, `followUpQuestion`, `followUpKeywords`, `followUpMarks`, and `totalMarks` (always `1 + followUpMarks`).

#### Scenario: McqPlusQuestionResponse totalMarks equals 1 plus followUpMarks
- **WHEN** a `McqPlusQuestionResponse` is returned from any endpoint
- **THEN** `totalMarks` equals `1 + followUpMarks`

#### Scenario: McqPlusQuestionResponse type discriminator is MCQ_PLUS
- **WHEN** a `McqPlusQuestionResponse` is serialised to JSON
- **THEN** the JSON contains `"type": "MCQ_PLUS"`

## Requirement: McqPlusResponse entity exists for future candidate response persistence
The `McqPlusResponse` JPA entity (extending `McqResponse`, JOINED) SHALL be mapped to the `mcq_plus_response` table. No service writes to it in this slice — the entity must exist so `ddl-auto=validate` passes.

#### Scenario: Application starts with mcq_plus_response table present
- **WHEN** the application starts against a Testcontainers database with all Slice A changesets applied
- **THEN** Hibernate validate succeeds and `McqPlusResponse` entity is registered in the persistence context without errors
