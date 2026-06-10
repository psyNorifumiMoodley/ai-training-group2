## Requirement: MCQ_PLUS is a recognised question type in the API
The system SHALL accept `"type": "MCQ_PLUS"` in `POST /api/questions` requests and return `"type": "MCQ_PLUS"` in question responses. The type SHALL be included in the `QuestionRequest` sealed interface (backend) and `QuestionType` union (frontend).

#### Scenario: POST with type MCQ_PLUS returns 201
- **WHEN** an authenticated MARKER sends `POST /api/questions` with `"type": "MCQ_PLUS"` and a valid `McqPlusQuestionRequest` body
- **THEN** the system returns HTTP 201 (stub response in Slice 0)

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

## Requirement: McqPlusQuestionResponse carries all MCQ fields plus follow-up and totalMarks
`McqPlusQuestionResponse` SHALL include `id`, `questionBanks`, `question`, `options`, `correctAnswers`, `multiCorrect`, `followUpQuestion`, `followUpKeywords`, `followUpMarks`, and `totalMarks` (always `1 + followUpMarks`).

#### Scenario: McqPlusQuestionResponse totalMarks equals 1 plus followUpMarks
- **WHEN** a `McqPlusQuestionResponse` is returned from any endpoint
- **THEN** `totalMarks` equals `1 + followUpMarks`

#### Scenario: McqPlusQuestionResponse type discriminator is MCQ_PLUS
- **WHEN** a `McqPlusQuestionResponse` is serialised to JSON
- **THEN** the JSON contains `"type": "MCQ_PLUS"`
