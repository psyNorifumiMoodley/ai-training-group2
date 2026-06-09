## ADDED Requirements

### Requirement: Marker creates a coding question in the question bank
A Marker or Admin SHALL be able to create a `coding_question` in a question bank by providing a prompt and a required programming language (Java, Python, or C#). Candidates submit their answer as inline source code — there is no file upload on a coding question.

#### Scenario: Create a coding question with a valid language
- **WHEN** a Marker submits a valid `POST /api/question-banks/{bankId}/coding-questions` request with a non-null `language` field (one of `JAVA`, `PYTHON`, `CSHARP`)
- **THEN** the response is HTTP 201 and the returned question has `type = CODING_QUESTION` and the specified language

#### Scenario: Create a coding question without a language is rejected
- **WHEN** a Marker submits a `POST /api/question-banks/{bankId}/coding-questions` request with no `language` field (or `language = null`)
- **THEN** the response is HTTP 400

#### Scenario: Invalid language value is rejected
- **WHEN** a Marker submits a `POST /api/question-banks/{bankId}/coding-questions` request with `language = "RUBY"`
- **THEN** the response is HTTP 400

#### Scenario: Attempting to create a doc question is blocked
- **WHEN** a Marker submits a `POST /api/question-banks/{bankId}/questions` request with `type = DOC_QUESTION`
- **THEN** the response is HTTP 410 (Gone — doc question creation is deprecated)

---

### Requirement: Marker manages test cases on a coding question
A Marker or Admin SHALL be able to add, edit, and delete test cases on a coding question. Each test case defines an input string, an expected output string, a timeout (seconds), and a memory limit (MB).

#### Scenario: Add a test case to a coding question
- **WHEN** a Marker submits a valid `POST /api/coding-questions/{questionId}/test-cases` request with `input`, `expectedOutput`, `timeoutSeconds` (≥ 1, ≤ 60), and `memoryMb` (≥ 64, ≤ 1024)
- **THEN** the response is HTTP 201 and the test case is associated with the question

#### Scenario: Edit an existing test case
- **WHEN** a Marker submits a valid `PUT /api/coding-questions/{questionId}/test-cases/{testCaseId}` request with updated fields
- **THEN** the response is HTTP 200 and the test case reflects the new values

#### Scenario: Delete a test case
- **WHEN** a Marker submits `DELETE /api/coding-questions/{questionId}/test-cases/{testCaseId}`
- **THEN** the response is HTTP 204 and the test case is removed

#### Scenario: Timeout out of range is rejected
- **WHEN** a Marker submits a test case with `timeoutSeconds = 0` or `timeoutSeconds > 60`
- **THEN** the response is HTTP 400

#### Scenario: Memory limit out of range is rejected
- **WHEN** a Marker submits a test case with `memoryMb < 64` or `memoryMb > 1024`
- **THEN** the response is HTTP 400

---

### Requirement: Coding question language and test case count are shown in question bank UI
The question bank UI SHALL display a language badge and test case count on every coding question card.

#### Scenario: Language badge visible on coding question card
- **WHEN** a Marker views the question bank and a coding question has `language = JAVA`
- **THEN** the question card displays a "Java" badge and the number of configured test cases

#### Scenario: "Doc Question" creation option is absent from question bank UI
- **WHEN** a Marker opens the question bank and navigates to the question creation menu
- **THEN** the "Doc Question" option is not present; only "Coding Question" (and other existing types) are available

---

### Requirement: Coding question counts toward the assessment doc/coding question limit
The existing assessment question limit (configurable, default 1 per assessment) SHALL apply to `coding_question` rows as well as legacy `doc_question` rows. A coding question and a legacy doc question each count as one toward the limit.

#### Scenario: Adding a coding question to an assessment that already has a legacy doc question is rejected
- **WHEN** an assessment already contains one `doc_question` (legacy) and a Marker attempts to add a `coding_question`
- **THEN** the response is HTTP 409 (limit exceeded)

#### Scenario: Adding a second coding question to an assessment that already has one is rejected
- **WHEN** an assessment already contains one `coding_question` and a Marker attempts to add another
- **THEN** the response is HTTP 409 (limit exceeded)
