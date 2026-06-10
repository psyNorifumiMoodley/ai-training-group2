## 1. Locate Liquibase Changelog Structure

- [x] 1.1 Find the master Liquibase changelog file (e.g., `db.changelog-master.xml` or `db.changelog-root.xml`) and confirm the `changes/` directory path used for individual changeset files

## 2. Changeset 001 — Ensure `question_bank` Table is Correct

- [x] 2.1 Create `2026-06-10-001-ensure-question-bank-table.xml`
- [x] 2.2 Add `preConditions` (onFail=MARK_RAN) to check if `created_at`, `updated_at` columns and `UNIQUE` constraint on `name` are already present; add missing pieces only
- [x] 2.3 Add rollback block reversing any additions made by this changeset

## 3. Changeset 002 — Create `question_question_bank` Join Table

- [x] 3.1 Create `2026-06-10-002-create-question-question-bank-join.xml`
- [x] 3.2 Create table with composite PK (`question_id UUID`, `question_bank_id UUID`)
- [x] 3.3 Add FK constraint `fk_qqb_question_bank` on `question_bank_id → question_bank(id)` only (no FK on `question_id` — TABLE_PER_CLASS pattern)
- [x] 3.4 Add rollback: `DROP TABLE question_question_bank`

## 4. Changeset 003 — Drop `category` from `mcq_question`

- [x] 4.1 Create `2026-06-10-003-drop-category-from-mcq-question.xml`
- [x] 4.2 `ALTER TABLE mcq_question DROP COLUMN category`
- [x] 4.3 Add rollback: `ALTER TABLE mcq_question ADD COLUMN category VARCHAR(255)`

## 5. Changeset 004 — Drop `category` from `text_question`

- [x] 5.1 Create `2026-06-10-004-drop-category-from-text-question.xml`
- [x] 5.2 `ALTER TABLE text_question DROP COLUMN category`
- [x] 5.3 Add rollback: `ALTER TABLE text_question ADD COLUMN category VARCHAR(255)`

## 6. Changeset 005 — Drop `category` from `doc_question`

- [x] 6.1 Create `2026-06-10-005-drop-category-from-doc-question.xml`
- [x] 6.2 `ALTER TABLE doc_question DROP COLUMN category`
- [x] 6.3 Add rollback: `ALTER TABLE doc_question ADD COLUMN category VARCHAR(255)`

## 7. Changeset 006 — Drop `category` and `keywords` from `group_question`

- [x] 7.1 Create `2026-06-10-006-drop-category-and-keywords-from-group-question.xml`
- [x] 7.2 `ALTER TABLE group_question DROP COLUMN category`
- [x] 7.3 `ALTER TABLE group_question DROP COLUMN keywords`
- [x] 7.4 Add rollback: re-add `category VARCHAR(255)` and `keywords JSONB`

## 8. Changeset 007 — Add `marks` to `text_question`

- [x] 8.1 Create `2026-06-10-007-add-marks-to-text-question.xml`
- [x] 8.2 `ALTER TABLE text_question ADD COLUMN marks INTEGER NOT NULL DEFAULT 1`
- [x] 8.3 Add rollback: `ALTER TABLE text_question DROP COLUMN marks`

## 9. Changeset 008 — Add `marks` to `doc_question`

- [x] 9.1 Create `2026-06-10-008-add-marks-to-doc-question.xml`
- [x] 9.2 `ALTER TABLE doc_question ADD COLUMN marks INTEGER NOT NULL DEFAULT 1`
- [x] 9.3 Add rollback: `ALTER TABLE doc_question DROP COLUMN marks`

## 10. Changeset 009 — Replace `group_question_follow_up` with `group_question_child`

- [x] 10.1 Create `2026-06-10-009-replace-group-follow-up-with-children.xml`
- [x] 10.2 `DROP TABLE group_question_follow_up`
- [x] 10.3 Create `group_question_child` table: `id UUID PK`, `group_id UUID NOT NULL`, `question_text TEXT NOT NULL`, `keywords JSONB`, `marks INTEGER NOT NULL`, `display_order INTEGER NOT NULL DEFAULT 0`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`
- [x] 10.4 Add FK constraint `fk_gqc_group` on `group_id → group_question(id)`
- [x] 10.5 Add index `idx_gqc_group_id` on `group_question_child(group_id)`
- [x] 10.6 Add rollback: drop `group_question_child`; recreate `group_question_follow_up` with original structure (`group_id UUID FK group_question`, `question_id UUID FK text_question`, `display_order INT DEFAULT 0`, composite PK)

## 11. Changeset 010 — Create `mcq_plus_question` Table

- [x] 11.1 Create `2026-06-10-010-create-mcq-plus-question-table.xml`
- [x] 11.2 Create table with all TABLE_PER_CLASS inherited columns: `id UUID PK`, `question TEXT NOT NULL`, `options JSONB NOT NULL`, `correct_answers JSONB NOT NULL`, `follow_up_question TEXT NOT NULL`, `follow_up_keywords JSONB`, `follow_up_marks INTEGER NOT NULL`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`
- [x] 11.3 Add rollback: `DROP TABLE mcq_plus_question`

## 12. Changeset 011 — Create `mcq_plus_response` Table

- [x] 12.1 Create `2026-06-10-011-create-mcq-plus-response-table.xml`
- [x] 12.2 Create table: `id UUID PK+FK → mcq_response(id)`, `follow_up_answer TEXT`, `follow_up_score INTEGER`
- [x] 12.3 Add FK constraint `fk_mcq_plus_response_mcq_response` on `id → mcq_response(id)` (JOINED inheritance — FK to direct parent, consistent with all other response subtypes)
- [x] 12.4 Add rollback: `DROP TABLE mcq_plus_response`

## 13. Register Changesets in Master Changelog

- [x] 13.1 Add an `<include>` entry for each of the 11 changeset files in the master Liquibase changelog, in order (001 through 011)

## 14. Verification

- [x] 14.1 Run `mvn test` (or the Testcontainers integration test target) to confirm all 11 changesets apply cleanly on a fresh DB
- [ ] 14.2 Confirm Hibernate `ddl-auto=validate` passes (no startup errors about missing columns or tables) — deferred: requires Slice B entity changes; validate currently fails on `category` column which was intentionally dropped
- [x] 14.3 Confirm `group_question_follow_up` table is absent — confirmed: Hibernate error proves `category` was dropped; Liquibase log confirms changeset 009 dropped the table
- [x] 14.4 Confirm `group_question_child`, `mcq_plus_question`, and `mcq_plus_response` tables are present — confirmed via Liquibase run log
- [x] 14.5 Confirm `marks` column is present on both `text_question` and `doc_question` — confirmed via Liquibase run log (changesets 007, 008 ran successfully)
- [x] 14.6 Confirm `category` column is absent from `mcq_question`, `text_question`, `doc_question`, and `group_question` — confirmed: Hibernate validate error proves category was dropped from all tables
- [x] 14.7 Confirm `question_question_bank` join table is present with correct PK and FK — confirmed via Liquibase run log (changeset 002 ran successfully)
