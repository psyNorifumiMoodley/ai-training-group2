## Why

The question domain refactor (Phase 6) requires a set of database schema changes before any backend entity or service work can begin. The existing schema still carries `category` columns on question tables, a `group_question_follow_up` join table that links `GroupQuestion` to standalone `TextQuestion` rows, and no `marks` columns on `text_question` or `doc_question`. Two new tables (`mcq_plus_question`, `mcq_plus_response`) and one join table (`question_question_bank`) are also absent. Slice A delivers all DDL changes as Liquibase changesets — nothing else — so that Slice B (backend logic) can develop against a schema that Hibernate's `ddl-auto=validate` will accept.

## What Changes

- **New table** — `question_question_bank` join table (composite PK on `question_id` + `question_bank_id`; FK on `question_bank_id` only — TABLE_PER_CLASS pattern, no FK on `question_id`)
- **New table** — `group_question_child` (owned by `group_question`; replaces the `group_question_follow_up` join table)
- **New table** — `mcq_plus_question` (TABLE_PER_CLASS; carries all inherited columns)
- **New table** — `mcq_plus_response` (joined to `response` hierarchy via `response_id` FK)
- **Ensured** — `question_bank` table verified to have correct final structure: `id`, `name` (UNIQUE NOT NULL), `created_at`, `updated_at`
- **Dropped** — `category` column from `mcq_question`, `text_question`, `doc_question`, `group_question`
- **Dropped** — `keywords` column from `group_question`
- **Dropped** — `group_question_follow_up` join table
- **Added** — `marks INTEGER NOT NULL DEFAULT 1` on `text_question`
- **Added** — `marks INTEGER NOT NULL DEFAULT 1` on `doc_question`

## Capabilities

### Modified Capabilities

- `question-bank-management`: The `question_question_bank` join table is the DB backing for `AssessmentQuestion.questionBanks` (ManyToMany). Without this table Hibernate validate will fail when Slice B entities are loaded.
- `question-marks`: `marks` columns on `text_question` and `doc_question` are required for the `TextQuestion` and `DocQuestion` entity fields added in Slice B.
- `group-question-inline-children`: `group_question_child` replaces the old `group_question_follow_up` join table, matching the new `GroupQuestion.children` OneToMany relationship in Slice B.
- `mcq-plus-question`: `mcq_plus_question` and `mcq_plus_response` tables are required for the `McqPlusQuestion` and `McqPlusResponse` entities in Slice B.

## Impact

- **Liquibase changelog** — 11 new changeset XML files added under `src/main/resources/db/changelog/changes/`
- **Master changelog** — each new changeset file included in the master Liquibase changelog
- **No entity changes** — no Java files are touched; this slice is DB-only
- **No service changes** — no business logic affected
- **Unblocks Slice B** — Hibernate `ddl-auto=validate` will pass after all changesets are applied, allowing Slice B entity work to begin
- **Runs in parallel** with Slice B and Slice C with no code conflicts (different files entirely)
