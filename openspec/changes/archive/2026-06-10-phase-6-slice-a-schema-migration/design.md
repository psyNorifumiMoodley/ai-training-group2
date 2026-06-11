## Context

Phase 2 implemented the question domain with a `String category` field on every concrete question table. The question hierarchy uses TABLE_PER_CLASS inheritance — each concrete type (`mcq_question`, `text_question`, `doc_question`, `group_question`) owns all its columns including `category`. `GroupQuestion` also had a `group_question_follow_up` join table linking it to standalone `text_question` rows via a ManyToMany, and a `keywords` column that was never populated.

Phase 6 replaces `category` with a proper `QuestionBank` entity backed by a `question_question_bank` many-to-many join table. It also:
- Embeds `GroupQuestion` children as owned `GroupQuestionChild` rows (OneToMany, not ManyToMany to standalone TextQuestions)
- Adds `marks` to `TextQuestion` and `DocQuestion`
- Introduces `McqPlusQuestion` (TABLE_PER_CLASS, inherits from McqQuestion) and `McqPlusResponse` (JOINED hierarchy, inherits from McqResponse)

Slice A's sole responsibility is the DDL: 11 Liquibase changesets that bring the schema to the state that Slice B's Hibernate entities expect. `ddl-auto=validate` is the acceptance criterion — if validate passes, the schema is correct.

## Goals / Non-Goals

**Goals:**
- Deliver all 11 DDL changesets as separate XML files, one logical change each
- Include rollback blocks on every changeset
- Use the project's ID format: `YYYY-MM-DD-NNN-short-description`
- Author field: developer's name on every changeset
- Verify that `ddl-auto=validate` passes on a Testcontainers DB after all changesets are applied
- Include the new changeset files in the Liquibase master changelog

**Non-Goals:**
- No JPA entity changes (Slice B)
- No service or repository changes (Slice B)
- No Angular changes (Slice C)
- No data migration — no existing rows need to be transformed; `category` columns are dropped without preserving data (dev/test environment only)
- No FK on `question_id` in `question_question_bank` — intentional, matching the TABLE_PER_CLASS pattern used by `assessment_question_link`

## Decisions

### 1. One changeset file per logical change

**Decision:** Each of the 11 DDL operations lives in its own XML file rather than being batched.

**Rationale:** Matches the Liquibase coding standard in CLAUDE.md ("one changeset per logical change"). Independent files make rollback granular — if `group_question_child` creation fails, we don't also roll back the `question_question_bank` table. Each file also maps cleanly to a single code review diff.

**Alternative considered:** One file with multiple changesets. Rejected: harder to review, harder to roll back selectively.

---

### 2. No FK on `question_id` in `question_question_bank`

**Decision:** `question_question_bank.question_id` has no foreign key constraint.

**Rationale:** The question hierarchy is TABLE_PER_CLASS — there is no single `assessment_question` table to reference. The existing `assessment_question_link` table in the codebase uses the same pattern. Adding individual FKs to each concrete table (`mcq_question`, `text_question`, etc.) would require a check constraint or multiple FK declarations, which adds complexity for no referential benefit at this stage.

**Alternative considered:** Add FK references to each concrete table. Rejected: overly complex; inconsistent with existing join table pattern.

---

### 3. `marks` columns use `NOT NULL DEFAULT 1`

**Decision:** `ALTER TABLE text_question ADD COLUMN marks INTEGER NOT NULL DEFAULT 1` and similarly for `doc_question`.

**Rationale:** Existing rows in dev/test environments need a valid value to satisfy NOT NULL. `DEFAULT 1` is the safest non-zero default — it represents "1 mark" which is the minimum valid value. The default is only applied to existing rows at migration time; Slice B's entity enforces `@Min(1)` going forward.

**Alternative considered:** `NOT NULL` without a default (would fail on tables with existing rows). Rejected: changeset must be re-runnable on non-empty dev databases.

---

### 4. Rollback for `group_question_follow_up` recreates it from scratch

**Decision:** The rollback block for changeset 009 recreates `group_question_follow_up` with the same structure it had before migration.

**Rationale:** The Liquibase standard in CLAUDE.md requires rollback blocks on all destructive changesets. Recreating the table structure (without data) is sufficient for dev/test rollback. No data preservation is needed.

---

### 5. `mcq_plus_response` joins to the `response` table, not a concrete MCQ table

**Decision:** `mcq_plus_response` has a `response_id` PK that is also a FK to `response(id)`, consistent with the JOINED inheritance strategy used by the response hierarchy.

**Rationale:** The spec explicitly states `McqPlusResponse` uses the existing JOINED `response` hierarchy. The `response` table's `dtype` discriminator handles type identification. This is consistent with how `mcq_response`, `text_response`, etc. are modelled.

## Risks / Trade-offs

- **`question_bank` table pre-existence** — The `question_bank` table was created in an earlier baseline changeset. Changeset 001 must use a Liquibase `preConditions` block to check for missing columns/constraints and add them only if absent. If `question_bank` already has the correct structure, changeset 001 is a no-op.

- **`ddl-auto=validate` requires exact column names** — Hibernate's `validate` mode checks that every mapped column exists with the correct type. Any discrepancy between Slice B entity mappings and the schema delivered here will cause startup failure. The changeset column names must exactly match the field names and `@Column` annotations defined in the Slice B spec.

- **Parallel development** — Slice B and Slice C developers will be working simultaneously. As long as Slice A only adds/modifies `resources/db/changelog` files, there are no code-level merge conflicts.

## Open Questions

None — the spec fully defines all 11 changesets with exact SQL, column types, and rollback blocks.
