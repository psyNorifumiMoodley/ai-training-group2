## 1. Liquibase Changesets

- [ ] 1.1 Verify `2026-06-11-001-create-coding-question-table.xml`: columns `id`, `question`, `language`, `created_at`, `updated_at`; no FK to `assessment_question` (TABLE_PER_CLASS)
- [ ] 1.2 Verify `2026-06-11-002-create-test-case-table.xml`: columns `id`, `coding_question_id` (FK with ON DELETE CASCADE), `input` (nullable), `expected_output`, `timeout_seconds` (default 10), `memory_mb` (default 256), `ordinal`, `created_at`, `updated_at`
- [ ] 1.3 Verify `db.changelog-master.xml` includes both changesets in order

## 2. JPA Entities

- [ ] 2.1 Verify `CodingQuestion.java`: `@Table(name = "coding_question")`, TABLE_PER_CLASS (inherited), `@Enumerated(EnumType.STRING) CodingQuestionLanguage language`, `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) List<TestCase> testCases`, `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`, no `@Data`
- [ ] 2.2 Verify `TestCase.java`: `@Table(name = "test_case")`, `@ManyToOne(fetch = FetchType.LAZY) CodingQuestion codingQuestion`, `input` (nullable), `expectedOutput`, `timeoutSeconds`, `memoryMb`, `ordinal`, `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`, `@ToString.Exclude` on both associations

## 3. Repositories

- [ ] 3.1 Verify `CodingQuestionRepository.java` extends `JpaRepository<CodingQuestion, UUID>` with no custom methods
- [ ] 3.2 Create `TestCaseRepository.java` extending `JpaRepository<TestCase, UUID>` with `List<TestCase> findByCodingQuestionIdOrderByOrdinalAsc(UUID codingQuestionId)` and `int countByCodingQuestionId(UUID codingQuestionId)`

## 4. Commit

- [ ] 4.1 Stage and commit all Slice A files: `CodingQuestion.java`, `TestCase.java`, `CodingQuestionRepository.java`, `TestCaseRepository.java`, both changesets, `db.changelog-master.xml`
