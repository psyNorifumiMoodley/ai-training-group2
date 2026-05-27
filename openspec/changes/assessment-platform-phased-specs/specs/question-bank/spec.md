## ADDED Requirements

### Requirement: Marker Creates a Question Bank
A Marker SHALL be able to create a named question bank to organise questions by category or topic.

#### Scenario: Successful question bank creation
- **WHEN** an authenticated Marker sends POST `/api/question-banks` with a valid name
- **THEN** the response is HTTP 201 with the created bank's UUID and name

#### Scenario: Non-marker cannot create question banks
- **WHEN** a user with role `CANDIDATE` sends POST `/api/question-banks`
- **THEN** the response is HTTP 403

---

### Requirement: Marker Adds MCQ Questions
A Marker SHALL be able to add multiple-choice questions (single or multiple correct answers) to a question bank.

#### Scenario: Add single-answer MCQ
- **WHEN** a Marker sends POST `/api/question-banks/{bankId}/questions` with type `MCQ`, one or more options, and exactly one `correct_answer`
- **THEN** the response is HTTP 201 with the created question UUID and all option data persisted

#### Scenario: Add multi-answer MCQ
- **WHEN** a Marker sends POST `/api/question-banks/{bankId}/questions` with type `MCQ` and two or more `correct_answers`
- **THEN** the response is HTTP 201 with all correct answers stored

#### Scenario: MCQ with no correct answer is rejected
- **WHEN** a Marker submits an MCQ question with zero `correct_answers`
- **THEN** the response is HTTP 400 with a validation error message

---

### Requirement: Marker Adds Text Questions
A Marker SHALL be able to add open-ended text questions to a question bank.

#### Scenario: Add text question
- **WHEN** a Marker sends POST `/api/question-banks/{bankId}/questions` with type `TEXT` and a non-empty question body
- **THEN** the response is HTTP 201 with the created question UUID

---

### Requirement: Marker Adds Document Questions
A Marker SHALL be able to add document-upload questions to a question bank.

#### Scenario: Add doc question
- **WHEN** a Marker sends POST `/api/question-banks/{bankId}/questions` with type `DOC` and a non-empty question body
- **THEN** the response is HTTP 201 with the created question UUID

---

### Requirement: Marker Creates Question Groups
A Marker SHALL be able to group related questions under a parent question group within a bank.

#### Scenario: Add question group with child questions
- **WHEN** a Marker sends POST `/api/question-banks/{bankId}/questions` with type `GROUP` containing child question definitions
- **THEN** the response is HTTP 201 with the group UUID and all child question UUIDs

---

### Requirement: Marker Lists Questions in a Bank
A Marker SHALL be able to retrieve all questions in a question bank, paginated.

#### Scenario: List questions
- **WHEN** a Marker sends GET `/api/question-banks/{bankId}/questions`
- **THEN** the response is HTTP 200 with a paginated list of questions including their type and content

#### Scenario: Non-existent bank returns 404
- **WHEN** a Marker requests questions for a `bankId` that does not exist
- **THEN** the response is HTTP 404

---

### Requirement: Angular Question Bank UI
The Angular application SHALL provide a UI for Markers to manage question banks and their questions.

#### Scenario: Marker creates a question bank
- **WHEN** a Marker submits the create-bank form with a valid name
- **THEN** the bank is created via the API and appears in the bank list

#### Scenario: Marker adds an MCQ question
- **WHEN** a Marker selects MCQ type, enters the question text, enters options, marks correct answers, and submits
- **THEN** the question is persisted and appears in the bank's question list