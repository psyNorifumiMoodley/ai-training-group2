## MODIFIED Requirements

### Requirement: Question Bank API contracts exist as stubs
The system SHALL expose four stub endpoints under `/api/question-banks` that return hardcoded responses, enabling frontend development to proceed against real HTTP paths before backend logic is implemented.

#### Scenario: List question banks returns empty list
- **WHEN** an authenticated MARKER or ADMIN sends `GET /api/questions/categories`
- **THEN** the system returns 404 (endpoint removed)

#### Scenario: List question banks returns success
- **WHEN** an authenticated MARKER or ADMIN sends `GET /api/question-banks`
- **THEN** the system returns HTTP 200 with a real paginated list of `QuestionBankResponse` objects from the database (empty array when no banks exist)

#### Scenario: Create question bank returns stub 201
- **WHEN** an authenticated MARKER or ADMIN sends `POST /api/question-banks` with `{ "name": "Java: Basics" }`
- **THEN** the system returns HTTP 201 with the newly persisted `QuestionBankResponse` body containing the generated UUID and the submitted name

#### Scenario: Rename question bank returns stub 200
- **WHEN** an authenticated MARKER or ADMIN sends `PUT /api/question-banks/{id}` with a valid UUID path param and a new name
- **THEN** the system returns HTTP 200 with the updated `QuestionBankResponse` body reflecting the new name

#### Scenario: Delete question bank returns stub 204
- **WHEN** an authenticated MARKER or ADMIN sends `DELETE /api/question-banks/{id}` with a valid UUID path param and the bank has no associated questions
- **THEN** the system returns HTTP 204 with no body and the bank is removed from the database

#### Scenario: Unauthenticated access is rejected
- **WHEN** any request is sent to `/api/question-banks` without a valid JWT
- **THEN** the system returns HTTP 401

#### Scenario: CANDIDATE role is forbidden
- **WHEN** a CANDIDATE JWT is used to call any `/api/question-banks` endpoint
- **THEN** the system returns HTTP 403

## ADDED Requirements

### Requirement: Question bank names must be unique
The system SHALL reject creation or renaming of a question bank if the requested name already exists in the database (case-sensitive comparison).

#### Scenario: Duplicate name on create is rejected
- **WHEN** an authenticated MARKER sends `POST /api/question-banks` with a `name` that already exists
- **THEN** the system returns HTTP 409

#### Scenario: Duplicate name on rename is rejected
- **WHEN** an authenticated MARKER sends `PUT /api/question-banks/{id}` with a `name` matching a different existing bank
- **THEN** the system returns HTTP 409

#### Scenario: Renaming to the same name is idempotent
- **WHEN** an authenticated MARKER sends `PUT /api/question-banks/{id}` with the bank's current name unchanged
- **THEN** the system returns HTTP 200 with no changes

### Requirement: Deleting a question bank with associated questions is rejected
The system SHALL refuse to delete a question bank that still has questions associated with it via the `question_question_bank` join table.

#### Scenario: Delete bank with questions returns 409
- **WHEN** an authenticated MARKER sends `DELETE /api/question-banks/{id}` and the bank has one or more associated questions
- **THEN** the system returns HTTP 409 with an error message indicating the bank is in use

#### Scenario: Delete bank with no questions succeeds
- **WHEN** an authenticated MARKER sends `DELETE /api/question-banks/{id}` and the bank has zero associated questions
- **THEN** the system returns HTTP 204 and the bank row is removed

### Requirement: Question bank not found returns 404
The system SHALL return HTTP 404 when a question bank endpoint is called with an ID that does not exist.

#### Scenario: Rename non-existent bank returns 404
- **WHEN** an authenticated MARKER sends `PUT /api/question-banks/{id}` with a UUID that does not match any bank
- **THEN** the system returns HTTP 404

#### Scenario: Delete non-existent bank returns 404
- **WHEN** an authenticated MARKER sends `DELETE /api/question-banks/{id}` with a UUID that does not match any bank
- **THEN** the system returns HTTP 404

### Requirement: Question creation and update persist question bank associations
The system SHALL associate questions with the provided question banks at create and update time by inserting rows into the `question_question_bank` join table.

#### Scenario: Created question is associated with all specified banks
- **WHEN** an authenticated MARKER sends `POST /api/questions` with `questionBankIds: [<bankA-id>, <bankB-id>]`
- **THEN** the response `questionBanks` array contains both banks and two rows exist in `question_question_bank`

#### Scenario: Updated question replaces bank associations
- **WHEN** an authenticated MARKER sends `PUT /api/questions/{id}` with a different `questionBankIds` list
- **THEN** the old join-table rows are removed and new ones reflecting the new bank IDs are inserted

#### Scenario: Question creation with non-existent bankId returns 404
- **WHEN** an authenticated MARKER sends `POST /api/questions` with a `questionBankId` that does not exist
- **THEN** the system returns HTTP 404

### Requirement: GET /api/questions filters by questionBankId using the join table
When `?questionBankId=<uuid>` is provided, the system SHALL query the `question_question_bank` join table to return only questions associated with that bank.

#### Scenario: questionBankId filter returns only questions in that bank
- **WHEN** `GET /api/questions?questionBankId=<valid-uuid>` is called by an authenticated MARKER
- **THEN** the response contains only questions whose `questionBanks` list includes the specified bank
