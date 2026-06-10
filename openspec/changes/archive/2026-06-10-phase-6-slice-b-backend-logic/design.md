## Context

Phase 6 Slice A applied 11 Liquibase changesets that transformed the schema:
- `category` columns dropped from all four concrete question tables
- `group_question_follow_up` join table replaced with `group_question_child` (owned children)
- `marks` added to `text_question` and `doc_question`
- `question_question_bank` join table added for question-to-bank many-to-many
- `mcq_plus_question` and `mcq_plus_response` tables created

The JPA entity layer has not been touched. `AssessmentQuestion` still maps `category`, `GroupQuestion` still points to `group_question_follow_up`, and `TextQuestion`/`DocQuestion` carry no `marks` field. The backend will fail to start with `ddl-auto=validate` until Slice B updates the entities to match the new schema.

`QuestionBankController` is a stub returning hardcoded values. `QuestionService` resolves no `questionBankIds` and stores no question-bank associations. `GroupQuestionChild`, `QuestionBank`, `McqPlusQuestion`, and `McqPlusResponse` do not exist as JPA entities.

## Goals / Non-Goals

**Goals:**
- Update all four existing question entities to match the new schema (remove `category`, add `marks`, swap to new associations)
- Introduce `QuestionBank`, `GroupQuestionChild`, `McqPlusQuestion`, and `McqPlusResponse` entities
- Implement `QuestionBankService` (real CRUD) and wire `QuestionBankController` to it
- Update `QuestionService` to resolve `questionBankIds`, persist `GroupQuestionChild` rows, and persist `McqPlusQuestion` via its own repository
- Filter `GET /api/questions` by `questionBankId` using the join table
- Pass `ddl-auto=validate` with Testcontainers and all existing tests green

**Non-Goals:**
- Angular UI (Slice C)
- `McqPlusResponse` submission and auto-marking — response persistence for MCQ_PLUS is handled in a later sub-task of Phase 4 work; the entity is created here but no service methods write to it
- Coding question changes — `CodingQuestion` entity and its repository are out of scope for this slice
- Data migration — no existing `category` data needs to be preserved (dev/test only)

## Decisions

### 1. `AssessmentQuestion` inherits `@ManyToMany` to `QuestionBank` via `question_question_bank`

**Decision:** Declare `questionBanks: Set<QuestionBank>` with `@ManyToMany`, `@JoinTable(name = "question_question_bank", joinColumns = @JoinColumn(name = "question_id"), inverseJoinColumns = @JoinColumn(name = "question_bank_id"))` on `AssessmentQuestion`. No FK on `question_id` in the join table (TABLE_PER_CLASS pattern — no single source table to reference). The mapping is `FetchType.LAZY` with `@ToString.Exclude`.

**Rationale:** Placing the owning side on `AssessmentQuestion` means all concrete question types automatically inherit the association without repeating the `@JoinTable` in each subclass. This matches how the existing `assessment_question_link` join table is handled.

**Alternative considered:** Declare the owning side on `QuestionBank.questions` instead. Rejected: would require loading a `QuestionBank` to add a question to it, creating a bidirectional ownership conflict and complicating the `QuestionService` API.

---

### 2. `QuestionBank.questions` is the inverse (mappedBy) side

**Decision:** `QuestionBank` declares `@ManyToMany(mappedBy = "questionBanks", fetch = FetchType.LAZY)` on a `questions` field annotated `@ToString.Exclude`. It is never directly written — `QuestionService` always writes through the question side.

**Rationale:** `QuestionBank` CRUD (create, rename, delete) never needs to touch question associations. Loading banks by ID never needs to load all their questions. The inverse mapping satisfies Hibernate's bidirectionality requirement without forcing an eager join.

---

### 3. `GroupQuestion.children` is a `@OneToMany(cascade = ALL, orphanRemoval = true)` to `GroupQuestionChild`

**Decision:** `GroupQuestion.children` is declared as `@OneToMany(mappedBy = "groupQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)` ordered by `@OrderColumn(name = "display_order")`. `GroupQuestionChild` has `@ManyToOne(fetch = FetchType.LAZY)` back to `GroupQuestion`.

**Rationale:** Children are aggregate members of their parent group — they have no independent lifecycle and never appear in standalone queries. `CascadeType.ALL` is appropriate here (unlike the blanket prohibition in CLAUDE.md which targets `REMOVE` alone) because children are true aggregate members. Orphan removal ensures deleting children from the list physically removes the rows.

---

### 4. `McqPlusQuestion` TABLE_PER_CLASS extending `McqQuestion`

**Decision:** `McqPlusQuestion` extends `McqQuestion` and uses `@Table(name = "mcq_plus_question")`. It carries all inherited columns (`id`, `question`, `options`, `correct_answers`) directly in its own table (TABLE_PER_CLASS — no FK to `mcq_question`). A dedicated `McqPlusQuestionRepository` manages persistence.

**Rationale:** The entire question hierarchy uses TABLE_PER_CLASS. Introducing a JOINED or SINGLE_TABLE subtype for `McqPlusQuestion` alone would break Hibernate's polymorphic query across `AssessmentQuestion`. The `mcq_plus_question` schema changeset already models this pattern.

---

### 5. `McqPlusResponse` JOINED hierarchy extending `McqResponse`

**Decision:** `McqPlusResponse` extends `McqResponse` and uses `@Table(name = "mcq_plus_response")`. Its `id` column is a PK and also a FK to `mcq_response(id)`, consistent with how the response hierarchy is already structured (JOINED, `@DiscriminatorColumn` on `response`).

**Rationale:** The response hierarchy is JOINED (all responses share a single `response` table for discriminator-based querying). `McqPlusResponse` must join this hierarchy, not use TABLE_PER_CLASS, because `ResponseService` queries `response` polymorphically. Fields `followUpAnswer` and `followUpScore` are the only additions.

---

### 6. `QuestionBankService` deletes a bank only if it has no questions

**Decision:** `QuestionBankService.delete()` checks whether any question in `question_question_bank` references the bank's ID. If questions exist, it throws `ValidationException` with HTTP 409. If the bank has no questions, it deletes the `question_bank` row.

**Rationale:** Silently orphaning join-table rows or cascade-deleting question-bank links would cause questions to lose their bank association. A guard-and-reject pattern keeps data consistent without requiring a forced disassociation flow that isn't in scope.

**Alternative considered:** Cascade delete all associations first, then delete the bank. Rejected: destroying a marker's question bank inadvertently would be destructive; explicit error is safer.

---

### 7. `@EntityGraph` on `AssessmentQuestionRepository` for bank-scoped listing

**Decision:** Add a `findAllByQuestionBanks_Id(UUID bankId, Pageable pageable)` method to `AssessmentQuestionRepository` annotated with `@EntityGraph(attributePaths = {"questionBanks"})` so the `questionBanks` collection is fetched in one join rather than N+1.

**Rationale:** The `list()` endpoint maps each question to a response that includes `questionBanks`. Without an entity graph, each question would trigger a separate select to load its banks. The entity graph collapses this to one query.

## Risks / Trade-offs

- **`ddl-auto=validate` column name exactness** — Every `@Column(name = "...")` on new and updated entities must match exactly what the Slice A changesets created. Any mismatch (e.g., `followUpQuestion` vs `follow_up_question`) will cause startup failure. Mitigation: verify each column name against the changeset XML before finalising entity annotations.

- **Existing tests that assume `category` field** — `QuestionControllerTest` and `QuestionService` tests currently supply `category` in requests. After this slice, `category` is removed and `questionBankIds` is required (min 1). Test fixtures must be updated to create a `QuestionBank` row first and pass its ID. Mitigation: all tests use Testcontainers; a test helper that creates a throwaway `QuestionBank` before each test simplifies setup.

- **TABLE_PER_CLASS polymorphic query performance** — `assessmentQuestionRepository.findAll()` issues a UNION ALL across all question tables. Adding `mcq_plus_question` to the hierarchy adds one more branch to every polymorphic query. Acceptable at current scale; noted for future optimisation if query count grows.

- **`QuestionBank` delete constraint** — If the guard raises HTTP 409 when questions exist, the Angular UI must handle it gracefully. This is a Slice C concern; the API contract is established here.

## Open Questions

None — schema is finalised by Slice A, DTOs are finalised by Slice 0. All column names and entity relationships are derivable from the existing changesets.
