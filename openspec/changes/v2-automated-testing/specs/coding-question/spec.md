## ADDED Requirements

### Requirement: Marker creates a coding question in the question bank
A Marker or Admin SHALL be able to create a `coding_question` by providing one or more question banks (`questionBankIds`), a prompt, and a required programming language (Java, Python, or C#). Candidates submit their answer as inline multiline source code — there is no file upload on a coding question. Question creation uses the existing `POST /api/questions` endpoint with `"type": "CODING"` as the discriminator — `CodingQuestionRequest` implements the existing `QuestionRequest` sealed interface. Like every other question type, a coding question has no `category` field; it is scoped exclusively via `Set<QuestionBank>`.

#### Scenario: Create a coding question with a valid language
- **WHEN** a Marker submits a valid `POST /api/questions` request with `"type": "CODING"`, at least one `questionBankIds` entry, and a non-null `language` field (one of `JAVA`, `PYTHON`, `CSHARP`)
- **THEN** the response is HTTP 201 and the returned question has `type = "CODING"`, the specified language, and the resolved `questionBanks`

#### Scenario: Create a coding question without a language is rejected
- **WHEN** a Marker submits a `POST /api/questions` request with `"type": "CODING"` and no `language` field (or `language = null`)
- **THEN** the response is HTTP 400

#### Scenario: Invalid language value is rejected
- **WHEN** a Marker submits a `POST /api/questions` request with `"type": "CODING"` and `language = "RUBY"`
- **THEN** the response is HTTP 400

#### Scenario: Create a coding question without a question bank is rejected
- **WHEN** a Marker submits a `POST /api/questions` request with `"type": "CODING"` and an empty or missing `questionBankIds` list
- **THEN** the response is HTTP 400

#### Scenario: Create a coding question with an unknown question bank is rejected
- **WHEN** a Marker submits a `POST /api/questions` request with `"type": "CODING"` and a `questionBankIds` entry that does not match any existing `QuestionBank`
- **THEN** the response is HTTP 400

#### Scenario: Attempting to create a doc question is blocked
- **WHEN** a Marker submits a `POST /api/questions` request with `"type": "DOC"`
- **THEN** the response is HTTP 410 (Gone — doc question creation is deprecated)

---

### Requirement: Marker manages test cases on a coding question
A Marker or Admin SHALL be able to manage test cases on a coding question. Each test case defines an input string, an expected output string, a timeout (seconds), and a memory limit (MB). Test cases are included in the `testCases` list on `CodingQuestionRequest` and are created or updated together with the question via `POST /api/questions` (create) or `PUT /api/questions/{id}` (update) — there is no separate test-case sub-resource.

#### Scenario: Create a coding question with inline test cases
- **WHEN** a Marker submits a valid `POST /api/questions` request with `"type": "CODING"` and a `testCases` array containing entries with `expectedOutput`, `timeoutSeconds` (≥ 1, ≤ 60), and `memoryMb` (≥ 64, ≤ 1024)
- **THEN** the response is HTTP 201 and the returned question includes the test cases with assigned ordinals

#### Scenario: Update a coding question with revised test cases
- **WHEN** a Marker submits a valid `PUT /api/questions/{id}` request with an updated `testCases` array
- **THEN** the response is HTTP 200 and the question reflects the new test case list

#### Scenario: Timeout out of range is rejected
- **WHEN** a Marker submits a coding question with a test case where `timeoutSeconds = 0` or `timeoutSeconds > 60`
- **THEN** the response is HTTP 400

#### Scenario: Memory limit out of range is rejected
- **WHEN** a Marker submits a coding question with a test case where `memoryMb < 64` or `memoryMb > 1024`
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
