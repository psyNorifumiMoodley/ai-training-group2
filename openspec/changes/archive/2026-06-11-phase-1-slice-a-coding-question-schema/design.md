## Context

All existing question subtypes (`McqQuestion`, `DocQuestion`, `TextQuestion`, `GroupQuestion`, `McqPlusQuestion`) use Hibernate `TABLE_PER_CLASS` inheritance: each concrete type gets its own fully independent table that duplicates the `AssessmentQuestion` base columns (`id`, `question`, `created_at`, `updated_at`). `CodingQuestion` follows this same pattern.

Test cases are an aggregate owned by `CodingQuestion`. They are never queried independently of their parent question (the `QuestionService` reads them via the `@OneToMany` collection). `CodingQuestionRepository` persists them through `CascadeType.ALL`.

Some Slice A files were produced as a prerequisite when Slice B was implemented: `CodingQuestion.java`, `TestCase.java`, `CodingQuestionRepository.java`, both Liquibase changesets, and the `db.changelog-master.xml` include. `TestCaseRepository` was not created (Slice B only needs `CodingQuestionRepository`). This slice commits that existing work and adds `TestCaseRepository`.

## Goals / Non-Goals

**Goals:**
- `coding_question` and `test_case` tables exist and pass `ddl-auto=validate`
- `CodingQuestion` and `TestCase` JPA entities compile and load
- `CodingQuestionRepository` and `TestCaseRepository` are injectable

**Non-Goals:**
- `QuestionService` changes (Slice B)
- Test case sub-resource endpoints
- Frontend changes

## Decisions

**TABLE_PER_CLASS, no FK to `assessment_question`:** `TABLE_PER_CLASS` creates a fully independent physical table — there is no `assessment_question` row for a `CodingQuestion` row, so an FK from `coding_question.id` → `assessment_question.id` would always be violated. The spec draft suggested this FK but it is incorrect for TABLE_PER_CLASS. The existing pattern (checked against `mcq_question`, `doc_question` etc. in the baseline changeset) uses no such FK.

**`question_question_bank` join table:** `coding_question_id` values appear in the shared `question_question_bank` join table with no FK constraint — same as every other TABLE_PER_CLASS subtype. No additional join-table changeset is needed; the `question_question_bank` table was created by the Question Model Refactor.

**`CascadeType.ALL` + `orphanRemoval = true` on `CodingQuestion.testCases`:** Test cases are a true aggregate member — they have no identity outside their parent question. This lets `QuestionService.createCodingQuestion()` and `updateCodingQuestion()` manage the list directly without calling `TestCaseRepository` explicitly.

**`TestCaseRepository` included anyway:** The spec requires it for future use (e.g., Slice D read-only display, or direct ordinal queries). Two targeted finder methods are sufficient: `findByCodingQuestionIdOrderByOrdinalAsc` and `countByCodingQuestionId`.

**Changeset IDs use `2026-06-11` date:** The files were authored on 2026-06-11, so their IDs reflect that date. The spec draft used `2026-06-05` as a placeholder; Liquibase IDs are immutable once applied, so the actual date is used.

## Risks / Trade-offs

- **[Risk] `ddl-auto=validate` fails on startup if migration hasn't run** → Mitigation: run `./mvnw liquibase:update` or let Spring Boot auto-run Liquibase before the first deploy.
- **[Risk] TABLE_PER_CLASS `id` in `question_question_bank` has no FK** → Mitigation: consistent with all other question subtypes; referential integrity is enforced at the service layer by `resolveQuestionBanks()`.
