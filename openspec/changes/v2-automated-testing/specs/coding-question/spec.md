## ADDED Requirements

### Requirement: Marker defines a coding question in the question bank
A Marker or Admin SHALL be able to mark a `doc_question` as a coding question by selecting a programming language (Java, Python, or C#) when creating or editing the question. A doc question without a language remains a plain file-upload question.

#### Scenario: Create a coding question with a language
- **WHEN** a Marker submits a valid `POST /api/question-banks/{bankId}/questions` request with `type = DOC_QUESTION` and a non-null `language` field (one of `JAVA`, `PYTHON`, `CSHARP`)
- **THEN** the response is HTTP 201 and the returned question includes the specified language

#### Scenario: Create a plain doc question without a language
- **WHEN** a Marker submits a valid `POST /api/question-banks/{bankId}/questions` request with `type = DOC_QUESTION` and no `language` field (or `language = null`)
- **THEN** the response is HTTP 201 and the question is created as a plain file-upload question with no test cases

#### Scenario: Invalid language value is rejected
- **WHEN** a Marker submits a `POST /api/question-banks/{bankId}/questions` request with `language = "RUBY"`
- **THEN** the response is HTTP 400

---

### Requirement: Marker manages test cases on a coding question
A Marker or Admin SHALL be able to add, edit, and delete test cases on a coding question. Each test case defines an input string, an expected output string, a timeout (seconds), and a memory limit (MB).

#### Scenario: Add a test case to a coding question
- **WHEN** a Marker submits a valid `POST /api/questions/{questionId}/test-cases` request with `input`, `expectedOutput`, `timeoutSeconds` (≥ 1, ≤ 60), and `memoryMb` (≥ 64, ≤ 1024)
- **THEN** the response is HTTP 201 and the test case is associated with the question

#### Scenario: Add a test case to a plain doc question is rejected
- **WHEN** a Marker submits a `POST /api/questions/{questionId}/test-cases` request for a doc question that has no language set
- **THEN** the response is HTTP 409

#### Scenario: Edit an existing test case
- **WHEN** a Marker submits a valid `PUT /api/questions/{questionId}/test-cases/{testCaseId}` request with updated fields
- **THEN** the response is HTTP 200 and the test case reflects the new values

#### Scenario: Delete a test case
- **WHEN** a Marker submits `DELETE /api/questions/{questionId}/test-cases/{testCaseId}`
- **THEN** the response is HTTP 204 and the test case is removed

#### Scenario: Timeout out of range is rejected
- **WHEN** a Marker submits a test case with `timeoutSeconds = 0` or `timeoutSeconds > 60`
- **THEN** the response is HTTP 400

#### Scenario: Memory limit out of range is rejected
- **WHEN** a Marker submits a test case with `memoryMb < 64` or `memoryMb > 1024`
- **THEN** the response is HTTP 400

---

### Requirement: Coding question language is shown in question bank UI
The question bank UI SHALL display a language badge on any doc question that has a non-null language, and show a test case count alongside it.

#### Scenario: Language badge visible on coding question card
- **WHEN** a Marker views the question bank and a doc question has `language = JAVA`
- **THEN** the question card displays a "Java" badge and the number of configured test cases

#### Scenario: No badge on plain doc questions
- **WHEN** a Marker views the question bank and a doc question has no language set
- **THEN** no language badge or test case count is shown

---

### Requirement: Coding question doc limit rule applies to coding questions
The existing assessment doc question limit (configurable, default 1 per assessment) SHALL continue to apply to coding questions. A coding question counts toward the limit identically to a plain doc question.

#### Scenario: Adding a second coding question to an assessment that already has one doc question is rejected
- **WHEN** an assessment already contains one `doc_question` (of any subtype) and a Marker attempts to add another `doc_question`
- **THEN** the response is HTTP 409 (doc question limit exceeded)
