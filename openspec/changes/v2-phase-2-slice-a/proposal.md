## Why

Slice 0 of V2 Phase 2 established the DTOs and stub endpoint for coding responses, but no persistence layer exists — `CodingResponse` and `TestCaseResult` entities are absent, meaning the `PUT /responses/{questionId}` and `POST /responses/{questionId}/execute` stubs cannot save or retrieve anything. Slice A delivers the schema and JPA entities that Slice B (Judge0 service logic) and Slice C (Angular UI) both depend on.

## What Changes

- New JPA entity `CodingResponse` — extends `Response` (JOINED inheritance), adds `code` (TEXT not null) and `executed_at` (TIMESTAMPTZ nullable) columns to a new `coding_response` table
- New JPA entity `TestCaseResult` — persists per-test-case execution results linked to a `CodingResponse` and a `TestCase`; adds a new `test_case_result` table
- New Liquibase changesets — `coding_response` and `test_case_result` DDL (with rollbacks)
- New `CodingResponseRepository` — Spring Data JPA interface for `CodingResponse` lookup by assessment + question
- New `TestCaseResultRepository` — Spring Data JPA interface for bulk delete-by-response and find-by-response

## Capabilities

### New Capabilities

- `coding-response`: Schema and JPA persistence layer for storing a candidate's code answer and its per-test-case execution results

### Modified Capabilities

## Impact

- **New entities**: `CodingResponse.java`, `TestCaseResult.java` in `domain/`
- **New repositories**: `CodingResponseRepository.java`, `TestCaseResultRepository.java` in `repository/`
- **New Liquibase changesets**: two new XML files in `db/changelog/changesets/`
- **No controller or service changes** — those belong in Slice B
- **No Angular changes** — those belong in Slice C
- Downstream: Slice B's service layer can now save/load `CodingResponse` rows and write `TestCaseResult` rows after Judge0 returns results
