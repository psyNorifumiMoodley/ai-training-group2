## 1. Liquibase Changesets

- [x] 1.1 Create `2026-06-15-001-create-coding-response-table.xml` — `coding_response` table with `id` (UUID PK, FK to `response(id)`), `code` (TEXT NOT NULL), `executed_at` (TIMESTAMPTZ nullable); include rollback block
- [x] 1.2 Create `2026-06-15-002-create-test-case-result-table.xml` — `test_case_result` table with `id` (UUID PK), `coding_response_id` (FK to `coding_response(id)`), `test_case_id` (FK to `test_case(id)`), `passed` (BOOLEAN NOT NULL), `actual_output` (TEXT nullable), `execution_time_ms` (BIGINT nullable), `memory_used_mb` (BIGINT nullable), `error_message` (TEXT nullable), `ordinal` (INTEGER NOT NULL), `created_at`/`updated_at` (TIMESTAMPTZ); include rollback block
- [x] 1.3 Register both changesets in `db.changelog-master.xml` in order

## 2. JPA Entities

- [x] 2.1 Create `CodingResponse.java` — extends `Response`; `@Entity`, `@Table(name = "coding_response")`, `@DiscriminatorValue("CodingResponse")`; fields: `code` (`@Column(nullable = false, columnDefinition = "TEXT")`), `executedAt` (`@Column(name = "executed_at")`); Lombok: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- [x] 2.2 Create `TestCaseResult.java` — extends `BaseEntity`; `@Entity`, `@Table(name = "test_case_result")`; `@ManyToOne(fetch = FetchType.LAZY)` to `CodingResponse` (`coding_response_id`); `@ManyToOne(fetch = FetchType.LAZY)` to `TestCase` (`test_case_id`); fields: `passed` (boolean, not null), `actualOutput` (TEXT nullable), `executionTimeMs` (BIGINT nullable), `memoryUsedMb` (BIGINT nullable), `errorMessage` (TEXT nullable), `ordinal` (int, not null); Lombok: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

## 3. Repositories

- [x] 3.1 Create `CodingResponseRepository.java` — extends `JpaRepository<CodingResponse, UUID>`; method: `Optional<CodingResponse> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId)`
- [x] 3.2 Create `TestCaseResultRepository.java` — extends `JpaRepository<TestCaseResult, UUID>`; methods: `List<TestCaseResult> findAllByCodingResponseId(UUID codingResponseId)`, `void deleteAllByCodingResponseId(UUID codingResponseId)`

## 4. Verification

- [x] 4.1 Run `mvn verify` (Testcontainers) and confirm Liquibase applies both changesets without errors and schema validation passes (`ddl-auto = validate`)
- [x] 4.2 Confirm `CodingResponse` and `TestCaseResult` entities load via basic repository `save` + `findById` in a test or integration check
