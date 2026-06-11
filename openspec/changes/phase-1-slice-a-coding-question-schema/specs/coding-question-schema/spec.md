## ADDED Requirements

### Requirement: coding_question table exists with correct schema
The database SHALL contain a `coding_question` table with columns `id` (UUID PK), `question` (TEXT NOT NULL), `language` (VARCHAR(20) NOT NULL), `created_at` (TIMESTAMPTZ NOT NULL), and `updated_at` (TIMESTAMPTZ NOT NULL). No FK to `assessment_question` (TABLE_PER_CLASS pattern).

#### Scenario: Schema validation passes on startup
- **WHEN** the Spring Boot application starts with `ddl-auto=validate` and the `coding_question` migration has been applied
- **THEN** Hibernate schema validation succeeds with no missing-column or missing-table errors

#### Scenario: Language column rejects null
- **WHEN** an INSERT into `coding_question` is attempted with a NULL `language` value
- **THEN** the database rejects the insert with a NOT NULL constraint violation

### Requirement: test_case table exists with correct schema
The database SHALL contain a `test_case` table with columns `id` (UUID PK), `coding_question_id` (UUID NOT NULL, FK to `coding_question.id` with ON DELETE CASCADE), `input` (TEXT nullable), `expected_output` (TEXT NOT NULL), `timeout_seconds` (INT NOT NULL, default 10), `memory_mb` (INT NOT NULL, default 256), `ordinal` (INT NOT NULL), `created_at` (TIMESTAMPTZ NOT NULL), `updated_at` (TIMESTAMPTZ NOT NULL).

#### Scenario: Schema validation passes on startup
- **WHEN** the Spring Boot application starts with `ddl-auto=validate` and the `test_case` migration has been applied
- **THEN** Hibernate schema validation succeeds with no missing-column or missing-table errors

#### Scenario: Deleting a coding_question cascades to its test_cases
- **WHEN** a `coding_question` row is deleted
- **THEN** all `test_case` rows with the matching `coding_question_id` are deleted automatically by the database (ON DELETE CASCADE)

### Requirement: CodingQuestion and TestCase JPA entities load cleanly
The `CodingQuestion` and `TestCase` entities SHALL pass Hibernate's startup schema validation and be injectable via the Spring context.

#### Scenario: Spring context loads with both entities present
- **WHEN** the Spring Boot application context starts
- **THEN** `CodingQuestion` and `TestCase` are loaded by Hibernate without mapping errors

### Requirement: CodingQuestionRepository and TestCaseRepository are injectable
`CodingQuestionRepository` (extending `JpaRepository<CodingQuestion, UUID>`) and `TestCaseRepository` (extending `JpaRepository<TestCase, UUID>`) SHALL be available as Spring beans.

#### Scenario: CodingQuestionRepository can be autowired
- **WHEN** a Spring component declares `CodingQuestionRepository` as a dependency
- **THEN** Spring injects the repository without errors

#### Scenario: TestCaseRepository ordered finder returns test cases sorted by ordinal
- **WHEN** `TestCaseRepository.findByCodingQuestionIdOrderByOrdinalAsc(id)` is called for a coding question that has test cases
- **THEN** the returned list is ordered ascending by the `ordinal` column
