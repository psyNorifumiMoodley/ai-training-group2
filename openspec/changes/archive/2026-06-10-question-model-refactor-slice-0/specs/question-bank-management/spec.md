## ADDED Requirements

### Requirement: Question Bank API contracts exist as stubs
The system SHALL expose four stub endpoints under `/api/question-banks` that return hardcoded responses, enabling frontend development to proceed against real HTTP paths before backend logic is implemented.

#### Scenario: List question banks returns empty list
- **WHEN** an authenticated MARKER or ADMIN sends `GET /api/questions/categories`
- **THEN** the system returns 404 (endpoint removed)

#### Scenario: List question banks returns success
- **WHEN** an authenticated MARKER or ADMIN sends `GET /api/question-banks`
- **THEN** the system returns HTTP 200 with an empty JSON array `[]`

#### Scenario: Create question bank returns stub 201
- **WHEN** an authenticated MARKER or ADMIN sends `POST /api/question-banks` with `{ "name": "Java: Basics" }`
- **THEN** the system returns HTTP 201 with a hardcoded `QuestionBankResponse` body

#### Scenario: Rename question bank returns stub 200
- **WHEN** an authenticated MARKER or ADMIN sends `PUT /api/question-banks/{id}` with a valid UUID path param
- **THEN** the system returns HTTP 200 with a hardcoded `QuestionBankResponse` body

#### Scenario: Delete question bank returns stub 204
- **WHEN** an authenticated MARKER or ADMIN sends `DELETE /api/question-banks/{id}` with a valid UUID path param
- **THEN** the system returns HTTP 204 with no body

#### Scenario: Unauthenticated access is rejected
- **WHEN** any request is sent to `/api/question-banks` without a valid JWT
- **THEN** the system returns HTTP 401

#### Scenario: CANDIDATE role is forbidden
- **WHEN** a CANDIDATE JWT is used to call any `/api/question-banks` endpoint
- **THEN** the system returns HTTP 403

### Requirement: QuestionBankResponse shape is defined
The system SHALL return `QuestionBankResponse` objects with a stable shape usable by the frontend.

#### Scenario: QuestionBankResponse contains id and name
- **WHEN** any `/api/question-banks` endpoint returns a `QuestionBankResponse`
- **THEN** the response body contains `id` (UUID string) and `name` (non-null string)

### Requirement: All question DTOs carry questionBankIds in requests and questionBanks in responses
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
- **THEN** the response body contains a `questionBanks` array (may be empty in stub state)

### Requirement: GET /api/questions category filter replaced with questionBankId
The system SHALL accept an optional `?questionBankId=<uuid>` query parameter on `GET /api/questions` instead of `?category=`.

#### Scenario: questionBankId filter is accepted
- **WHEN** `GET /api/questions?questionBankId=<valid-uuid>` is called by an authenticated MARKER
- **THEN** the system returns HTTP 200 (stub may return empty page)

#### Scenario: category filter no longer accepted
- **WHEN** `GET /api/questions?category=Java` is called
- **THEN** the system ignores the `category` param (no 400 — it is simply an unknown query param)
