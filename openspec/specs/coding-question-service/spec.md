### Requirement: Marker can create a coding question
`QuestionService` SHALL accept a `CodingQuestionRequest` via `POST /api/questions`, persist a `CodingQuestion` with the specified language, question banks, and inline test cases, and return a `CodingQuestionResponse` with HTTP 201.

#### Scenario: Valid create request returns 201
- **WHEN** a MARKER sends `POST /api/questions` with `type: "CODING"`, a valid `language`, at least one `questionBankId`, and a (possibly empty) `testCases` array
- **THEN** the server returns HTTP 201 with a `CodingQuestionResponse` containing `id`, `questionBanks`, `language`, and `testCases`

#### Scenario: Missing language returns 400
- **WHEN** a MARKER sends `POST /api/questions` with `type: "CODING"` and no `language` field
- **THEN** the server returns HTTP 400 (bean validation failure)

#### Scenario: Empty questionBankIds returns 400
- **WHEN** a MARKER sends `POST /api/questions` with `type: "CODING"` and an empty `questionBankIds` list
- **THEN** the server returns HTTP 400

#### Scenario: Unknown questionBankId returns 400
- **WHEN** a MARKER sends `POST /api/questions` with a `questionBankIds` entry that does not exist in the database
- **THEN** the server returns HTTP 400 (ValidationException)

### Requirement: Test cases receive sequential ordinals on creation
`QuestionService` SHALL assign ordinals 1, 2, 3… to test cases in the order they appear in `CodingQuestionRequest.testCases`.

#### Scenario: Ordinals reflect request order
- **WHEN** a coding question is created with three test cases
- **THEN** the persisted test cases have `ordinal` values 1, 2, and 3 respectively

#### Scenario: No test cases results in empty testCases list
- **WHEN** a coding question is created with a null or empty `testCases` field
- **THEN** the returned `CodingQuestionResponse.testCases` is an empty list

### Requirement: Marker can retrieve a coding question
`QuestionService` SHALL map a `CodingQuestion` entity to a `CodingQuestionResponse` when `GET /api/questions/{id}` is called.

#### Scenario: Response includes language, questionBanks, and testCases
- **WHEN** `GET /api/questions/{id}` is called for a coding question
- **THEN** the response body contains `type: "CODING"`, `language`, `questionBanks`, and `testCases` (empty list if none exist)

### Requirement: Marker can update a coding question
`QuestionService` SHALL accept a `CodingQuestionRequest` via `PUT /api/questions/{id}` for an existing `CodingQuestion`, replacing all fields including test cases.

#### Scenario: Update replaces test cases fully
- **WHEN** a `PUT /api/questions/{id}` is sent for a coding question that has two test cases, with a new request containing one test case
- **THEN** the response contains exactly one test case (the old ones are removed)

#### Scenario: Type mismatch returns 400
- **WHEN** a `PUT /api/questions/{id}` sends a `CodingQuestionRequest` for a question ID that is not a `CodingQuestion`
- **THEN** the server returns HTTP 400 (ValidationException)

### Requirement: Assessment doc/coding question limit applies to both types
`AssessmentService` SHALL count both `DocQuestion` and `CodingQuestion` rows against `assessment.doc-question-limit` when validating an assessment's composition.

#### Scenario: One DocQuestion plus one CodingQuestion exceeds limit of 1
- **WHEN** an assessment is generated or validated that would include one `DocQuestion` and one `CodingQuestion`
- **THEN** the service throws a `ValidationException` and the assessment is not created (HTTP 409)

#### Scenario: Two CodingQuestions exceeds limit of 1
- **WHEN** an assessment is generated or validated that would include two `CodingQuestion` rows
- **THEN** the service throws a `ValidationException` and the assessment is not created (HTTP 409)

#### Scenario: One CodingQuestion within limit succeeds
- **WHEN** an assessment is generated with exactly one `CodingQuestion` and no `DocQuestion`
- **THEN** the assessment is created successfully
