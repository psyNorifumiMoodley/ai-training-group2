## Requirement: Question Bank API contracts expose real database-backed endpoints
The system SHALL expose four endpoints under `/api/question-banks` that read from and write to the database, returning real data rather than hardcoded responses.

#### Scenario: List question banks returns empty list
- **WHEN** an authenticated MARKER or ADMIN sends `GET /api/questions/categories`
- **THEN** the system returns 404 (endpoint removed)

#### Scenario: List question banks returns success
- **WHEN** an authenticated MARKER or ADMIN sends `GET /api/question-banks`
- **THEN** the system returns HTTP 200 with a real paginated list of `QuestionBankResponse` objects from the database (empty array when no banks exist)

#### Scenario: Create question bank returns 201
- **WHEN** an authenticated MARKER or ADMIN sends `POST /api/question-banks` with `{ "name": "Java: Basics" }`
- **THEN** the system returns HTTP 201 with the newly persisted `QuestionBankResponse` body containing the generated UUID and the submitted name

#### Scenario: Rename question bank returns 200
- **WHEN** an authenticated MARKER or ADMIN sends `PUT /api/question-banks/{id}` with a valid UUID path param and a new name
- **THEN** the system returns HTTP 200 with the updated `QuestionBankResponse` body reflecting the new name

#### Scenario: Delete question bank returns 204
- **WHEN** an authenticated MARKER or ADMIN sends `DELETE /api/question-banks/{id}` with a valid UUID path param and the bank has no associated questions
- **THEN** the system returns HTTP 204 with no body and the bank is removed from the database

#### Scenario: Unauthenticated access is rejected
- **WHEN** any request is sent to `/api/question-banks` without a valid JWT
- **THEN** the system returns HTTP 401

#### Scenario: CANDIDATE role is forbidden
- **WHEN** a CANDIDATE JWT is used to call any `/api/question-banks` endpoint
- **THEN** the system returns HTTP 403

## Requirement: QuestionBankResponse shape is defined
The system SHALL return `QuestionBankResponse` objects with a stable shape usable by the frontend.

#### Scenario: QuestionBankResponse contains id and name
- **WHEN** any `/api/question-banks` endpoint returns a `QuestionBankResponse`
- **THEN** the response body contains `id` (UUID string) and `name` (non-null string)

## Requirement: Question bank names must be unique
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

## Requirement: Deleting a question bank with associated questions is rejected
The system SHALL refuse to delete a question bank that still has questions associated with it via the `question_question_bank` join table.

#### Scenario: Delete bank with questions returns 409
- **WHEN** an authenticated MARKER sends `DELETE /api/question-banks/{id}` and the bank has one or more associated questions
- **THEN** the system returns HTTP 409 with an error message indicating the bank is in use

#### Scenario: Delete bank with no questions succeeds
- **WHEN** an authenticated MARKER sends `DELETE /api/question-banks/{id}` and the bank has zero associated questions
- **THEN** the system returns HTTP 204 and the bank row is removed

## Requirement: Question bank not found returns 404
The system SHALL return HTTP 404 when a question bank endpoint is called with an ID that does not exist.

#### Scenario: Rename non-existent bank returns 404
- **WHEN** an authenticated MARKER sends `PUT /api/question-banks/{id}` with a UUID that does not match any bank
- **THEN** the system returns HTTP 404

#### Scenario: Delete non-existent bank returns 404
- **WHEN** an authenticated MARKER sends `DELETE /api/question-banks/{id}` with a UUID that does not match any bank
- **THEN** the system returns HTTP 404

## Requirement: All question DTOs carry questionBankIds in requests and questionBanks in responses
Every question creation request SHALL include `questionBankIds: List<UUID>` (min 1 entry). Every question response SHALL include `questionBanks: List<QuestionBankResponse>`.

#### Scenario: MCQ request without questionBankIds is rejected
- **WHEN** `POST /api/questions` is sent with type `MCQ` and `questionBankIds` absent or empty
- **THEN** the system returns HTTP 400

#### Scenario: TEXT request without questionBankIds is rejected
- **WHEN** `POST /api/questions` is sent with type `TEXT` and `questionBankIds` absent or empty
- **THEN** the system returns HTTP 400

#### Scenario: GROUP request without questionBankIds is rejected
- **WHEN** `POST /api/questions` is sent with type `GROUP` and `questionBankIds` absent or empty
- **THEN** the system returns HTTP 400

#### Scenario: MCQ question response includes questionBanks list
- **WHEN** `GET /api/questions/{id}` returns a question
- **THEN** the response body contains a `questionBanks` array

## Requirement: Question creation and update persist question bank associations
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

## Requirement: GET /api/questions filters by questionBankId using the join table
When `?questionBankId=<uuid>` is provided, the system SHALL query the `question_question_bank` join table to return only questions associated with that bank.

#### Scenario: questionBankId filter returns only questions in that bank
- **WHEN** `GET /api/questions?questionBankId=<valid-uuid>` is called by an authenticated MARKER
- **THEN** the response contains only questions whose `questionBanks` list includes the specified bank

#### Scenario: category filter no longer accepted
- **WHEN** `GET /api/questions?category=Java` is called
- **THEN** the system ignores the `category` param (no 400 — it is simply an unknown query param)
