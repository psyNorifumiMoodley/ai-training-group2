## Why

`CodingQuestion` and `TestCase` have no DB tables, JPA entities, or repositories yet. Slice B (`QuestionService` coding question support) is already implemented and committed, but it cannot be deployed or tested end-to-end until the schema and data layer exist.

## What Changes

- New Liquibase changeset: `coding_question` table (TABLE_PER_CLASS, stores `language` + inherited `question`/timestamps)
- New Liquibase changeset: `test_case` table (FK to `coding_question`, stores `input`, `expected_output`, `timeout_seconds`, `memory_mb`, `ordinal`)
- New JPA entity: `CodingQuestion extends AssessmentQuestion` with `@OneToMany(cascade = CascadeType.ALL)` to `TestCase`
- New JPA entity: `TestCase extends BaseEntity` with `@ManyToOne` back to `CodingQuestion`
- New repository: `CodingQuestionRepository extends JpaRepository<CodingQuestion, UUID>`
- New repository: `TestCaseRepository extends JpaRepository<TestCase, UUID>` with ordinal-ordered finder
- `db.changelog-master.xml` updated to include both new changesets

## Capabilities

### New Capabilities
- `coding-question-schema`: DB schema and JPA layer for `CodingQuestion` and `TestCase` entities — tables, entities, and repositories

### Modified Capabilities

## Impact

- Adds two new Liquibase changesets; all environments must run the migration before the app starts
- `ddl-auto=validate` will fail at startup until both tables exist
- Slice B (`QuestionService`) depends on `CodingQuestionRepository` being present; Slice B is already committed on this branch
