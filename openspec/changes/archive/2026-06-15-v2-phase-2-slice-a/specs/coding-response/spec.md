## ADDED Requirements

### Requirement: CodingResponse Entity Persists Candidate Code
The system SHALL persist a candidate's submitted code for a CODING question as a `CodingResponse` row in the `coding_response` table, linked to the parent `response` record via JOINED inheritance.

#### Scenario: CodingResponse row is created with non-null code
- **WHEN** a `CodingResponse` is saved with a non-null `code` value and valid `assessment` and `question` associations
- **THEN** a row exists in `response` (with `dtype = 'CodingResponse'`) and a corresponding row exists in `coding_response` with the same `id` and the persisted `code`

#### Scenario: CodingResponse is retrievable by assessment and question
- **WHEN** `CodingResponseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId)` is called with valid IDs
- **THEN** the matching `CodingResponse` is returned (or `Optional.empty()` if none exists)

#### Scenario: CodingResponse tracks last execution timestamp
- **WHEN** a `CodingResponse` is saved with a non-null `executedAt` value
- **THEN** the `executed_at` column in `coding_response` reflects that timestamp

#### Scenario: CodingResponse with null executedAt is valid
- **WHEN** a `CodingResponse` is saved without setting `executedAt`
- **THEN** the `executed_at` column is NULL and the row persists without error

---

### Requirement: TestCaseResult Entity Persists Execution Outcomes
The system SHALL persist the per-test-case result of a code execution run as `TestCaseResult` rows in the `test_case_result` table, each linked to exactly one `CodingResponse` and one `TestCase`.

#### Scenario: TestCaseResult row persists all execution fields
- **WHEN** a `TestCaseResult` is saved with `codingResponse`, `testCase`, `passed`, `ordinal`, and optional nullable fields (`actualOutput`, `executionTimeMs`, `memoryUsedMb`, `errorMessage`)
- **THEN** a row exists in `test_case_result` with all fields correctly stored

#### Scenario: All TestCaseResults for a CodingResponse are retrievable
- **WHEN** `TestCaseResultRepository.findAllByCodingResponseId(codingResponseId)` is called
- **THEN** all `TestCaseResult` rows associated with that `CodingResponse` are returned

#### Scenario: All TestCaseResults for a CodingResponse can be bulk-deleted
- **WHEN** `TestCaseResultRepository.deleteAllByCodingResponseId(codingResponseId)` is called
- **THEN** all `TestCaseResult` rows for that `CodingResponse` are removed and none remain

#### Scenario: TestCaseResult with nullable fields persists without error
- **WHEN** a `TestCaseResult` is saved with `actualOutput`, `executionTimeMs`, `memoryUsedMb`, and `errorMessage` all null
- **THEN** the row persists with those columns as NULL

---

### Requirement: Liquibase Schema for coding_response and test_case_result
The system SHALL create the `coding_response` and `test_case_result` tables via Liquibase changesets, each with a rollback block.

#### Scenario: coding_response table is created by migration
- **WHEN** Liquibase runs the changeset for `coding_response`
- **THEN** a `coding_response` table exists with columns: `id` (UUID PK, FK to `response(id)`), `code` (TEXT NOT NULL), `executed_at` (TIMESTAMPTZ nullable)

#### Scenario: test_case_result table is created by migration
- **WHEN** Liquibase runs the changeset for `test_case_result`
- **THEN** a `test_case_result` table exists with columns: `id` (UUID PK), `coding_response_id` (UUID FK to `coding_response(id)`), `test_case_id` (UUID FK to `test_case(id)`), `passed` (BOOLEAN NOT NULL), `actual_output` (TEXT nullable), `execution_time_ms` (BIGINT nullable), `memory_used_mb` (BIGINT nullable), `error_message` (TEXT nullable), `ordinal` (INTEGER NOT NULL), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ)

#### Scenario: Rollback removes both tables
- **WHEN** the changesets are rolled back
- **THEN** neither `coding_response` nor `test_case_result` tables exist
