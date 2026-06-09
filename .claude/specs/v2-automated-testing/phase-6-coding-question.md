# Phase 6 — Coding Question

> **Epic:** Phase 6 — Coding Question
> **Delivery:** Slice 0 merges first; Slices A–D run in parallel (one dev each).
> **Dependency:** Phases 0–5 fully merged.
> **Jira Epic:** ATG-55

### Key Design Decisions
- `coding_question` is a new `TABLE_PER_CLASS` subtype of `assessment_question` — it gets its own DB table
- `language` is **required** (NOT NULL) on `coding_question`; candidates submit source code as inline text (no file upload)
- `doc_question` is **soft-deprecated**: existing rows remain valid; new creation blocked at the API layer (HTTP 410)
- The assessment doc/coding question limit (`assessment.doc-question-limit`, default 1) counts both `doc_question` (legacy) and `coding_question` rows
- `test_case` rows are owned by `coding_question` (FK `coding_question_id`), not by `doc_question`

---

## Slice 0: API Contracts *(merge first)*

### Agent Brief
Stub all Phase 6 backend endpoints and Angular service methods so Slices A–D can develop simultaneously without merge conflicts. Also block `doc_question` creation (return 410) and define the TypeScript models for the new `CodingQuestion` type. No business logic — hardcoded responses only.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    Language.java                      ← enum JAVA, PYTHON, CSHARP
  dto/
    CodingQuestionRequest.java         ← record { String category, String question, @NotNull Language language, List<@Valid TestCaseRequest> testCases }
    CodingQuestionResponse.java        ← record { UUID id, String category, String question, Language language, List<TestCaseResponse> testCases }
    TestCaseRequest.java               ← record { String input, @NotBlank String expectedOutput, @Min(1) @Max(60) int timeoutSeconds, @Min(64) @Max(1024) int memoryMb }
    TestCaseResponse.java              ← record { UUID id, String input, String expectedOutput, int timeoutSeconds, int memoryMb, int ordinal }
  controller/
    CodingQuestionController.java      ← stub
```

**Frontend**
```
src/app/
  core/
    services/
      coding-question.service.ts       ← stub
    models/
      coding-question.model.ts
```

### Entities
None in this slice.

### Liquibase Changesets
None in this slice.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`CodingQuestionController`** — stubs
```
POST   /api/question-banks/{bankId}/coding-questions                     → 201 (hardcoded)
GET    /api/question-banks/{bankId}/coding-questions/{id}                → 200 (hardcoded)
POST   /api/coding-questions/{questionId}/test-cases                     → 201 (hardcoded)
GET    /api/coding-questions/{questionId}/test-cases                     → 200 [] (hardcoded)
PUT    /api/coding-questions/{questionId}/test-cases/{testCaseId}        → 200 (hardcoded)
DELETE /api/coding-questions/{questionId}/test-cases/{testCaseId}        → 204 (hardcoded)
```
All endpoints secured with `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")`.

**`QuestionController`** — add deprecation block
```
POST /api/question-banks/{bankId}/questions  (type = DOC_QUESTION)  → 410 Gone
  Message body: { "message": "Doc question creation is deprecated. Use POST /api/question-banks/{bankId}/coding-questions instead." }
```

### Frontend Type Definitions
```typescript
// core/models/coding-question.model.ts
export type Language = 'JAVA' | 'PYTHON' | 'CSHARP';

export interface TestCaseRequest {
  input: string;
  expectedOutput: string;
  timeoutSeconds: number;
  memoryMb: number;
}

export interface TestCase {
  id: string;
  input: string;
  expectedOutput: string;
  timeoutSeconds: number;
  memoryMb: number;
  ordinal: number;
}

export interface CodingQuestionRequest {
  category: string;
  question: string;
  language: Language;
  testCases?: TestCaseRequest[];
}

export interface CodingQuestionResponse {
  id: string;
  category: string;
  question: string;
  language: Language;
  testCases: TestCase[];
}
```

### Frontend Services

**`CodingQuestionService`** — stubs returning `EMPTY`
```typescript
createCodingQuestion(bankId: string, request: CodingQuestionRequest): Observable<CodingQuestionResponse>
getCodingQuestion(bankId: string, id: string): Observable<CodingQuestionResponse>
addTestCase(questionId: string, request: TestCaseRequest): Observable<TestCase>
getTestCases(questionId: string): Observable<TestCase[]>
updateTestCase(questionId: string, testCaseId: string, request: TestCaseRequest): Observable<TestCase>
deleteTestCase(questionId: string, testCaseId: string): Observable<void>
```

### Frontend Components
None in this slice.

### Route Additions
None in this slice.

### Testing
- All stubs return correct HTTP status codes with MARKER/ADMIN role JWT
- Non-MARKER/ADMIN callers receive 403
- `POST /api/question-banks/{bankId}/questions` with `type = DOC_QUESTION` returns 410
- Angular `CodingQuestionService` compiles with correct type signatures

### Done When
- All backend stubs compile and return hardcoded responses
- `Language` enum exists in domain package
- Angular `CodingQuestionService` and `coding-question.model.ts` compile with no TypeScript errors
- Merge Slice 0 to `main` before any other slice begins

---

## Slice A: Schema Migration & Test Case CRUD

### Agent Brief
Create the `coding_question` and `test_case` DB tables via Liquibase. Implement the `CodingQuestion` and `TestCase` JPA entities, `TestCaseRepository`, and `TestCaseService`. Wire the test case CRUD endpoints in `CodingQuestionController`. No coding question creation logic yet — that is Slice B.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    CodingQuestion.java         ← entity, TABLE_PER_CLASS subtype
    TestCase.java               ← entity
  repository/
    CodingQuestionRepository.java
    TestCaseRepository.java
  service/
    TestCaseService.java
src/main/resources/db/changelog/changesets/
  2026-06-05-001-create-coding-question-table.xml
  2026-06-05-002-create-test-case-table.xml
src/test/java/com/psybergate/dap/service/
  TestCaseServiceTest.java
```

### Entities

**`CodingQuestion`** — new TABLE_PER_CLASS subtype
- Annotations: `@Entity`
- Extends: `AssessmentQuestion`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- Fields:
  - `@Enumerated(EnumType.STRING) @Column(nullable = false) Language language`
  - `@OneToMany(mappedBy = "codingQuestion", cascade = CascadeType.ALL, orphanRemoval = true) @ToString.Exclude List<TestCase> testCases = new ArrayList<>()`

**`TestCase`**
- Table: `test_case`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coding_question_id", nullable = false) @ToString.Exclude CodingQuestion codingQuestion`
  - `@Column(columnDefinition = "TEXT") String input`
  - `@Column(nullable = false, columnDefinition = "TEXT") String expectedOutput`
  - `@Column(nullable = false) int timeoutSeconds`
  - `@Column(nullable = false) int memoryMb`
  - `@Column(nullable = false) int ordinal`

### Liquibase Changesets

**`2026-06-05-001-create-coding-question-table.xml`**
```xml
<changeSet id="2026-06-05-001-create-coding-question-table" author="developer">
  <createTable tableName="coding_question">
    <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
    <column name="language" type="VARCHAR(20)"><constraints nullable="false"/></column>
  </createTable>
  <addForeignKeyConstraint
    baseTableName="coding_question" baseColumnNames="id"
    referencedTableName="assessment_question" referencedColumnNames="id"
    constraintName="fk_coding_question_assessment_question"/>
  <rollback><dropTable tableName="coding_question"/></rollback>
</changeSet>
```

**`2026-06-05-002-create-test-case-table.xml`**
```xml
<changeSet id="2026-06-05-002-create-test-case-table" author="developer">
  <createTable tableName="test_case">
    <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
    <column name="coding_question_id" type="UUID"><constraints nullable="false"/></column>
    <column name="input" type="TEXT"/>
    <column name="expected_output" type="TEXT"><constraints nullable="false"/></column>
    <column name="timeout_seconds" type="INT" defaultValueNumeric="10"><constraints nullable="false"/></column>
    <column name="memory_mb" type="INT" defaultValueNumeric="256"><constraints nullable="false"/></column>
    <column name="ordinal" type="INT"><constraints nullable="false"/></column>
  </createTable>
  <addForeignKeyConstraint
    baseTableName="test_case" baseColumnNames="coding_question_id"
    referencedTableName="coding_question" referencedColumnNames="id"
    constraintName="fk_test_case_coding_question" onDelete="CASCADE"/>
  <rollback><dropTable tableName="test_case"/></rollback>
</changeSet>
```

### Repositories

**`CodingQuestionRepository extends JpaRepository<CodingQuestion, UUID>`**
```java
// No custom methods needed in this slice
```

**`TestCaseRepository extends JpaRepository<TestCase, UUID>`**
```java
List<TestCase> findByCodingQuestionIdOrderByOrdinalAsc(UUID codingQuestionId);
```

### Services

**`TestCaseService`**
```java
TestCaseResponse addTestCase(UUID questionId, TestCaseRequest request);
// Load CodingQuestion (throw ResourceNotFoundException → 404 if not found)
// Set ordinal = current max ordinal + 1
// Persist and return TestCaseResponse

List<TestCaseResponse> getTestCases(UUID questionId);

TestCaseResponse updateTestCase(UUID questionId, UUID testCaseId, TestCaseRequest request);
// Load test case; verify it belongs to questionId (throw ResourceNotFoundException if not)
// Update fields; persist; return updated response

void deleteTestCase(UUID questionId, UUID testCaseId);
// Load test case; verify ownership; delete
```

All methods `@Transactional`. Validation constraints on `TestCaseRequest` are enforced by `@Valid` in the controller.

### Controllers
Wire `TestCaseService` into `CodingQuestionController` stub methods (replace hardcoded 201/200/204 with real service calls).

### Frontend Type Definitions
No changes in this slice.

### Frontend Services
No changes in this slice.

### Frontend Components
None.

### Route Additions
None.

### Testing

**`TestCaseServiceTest`** (`@SpringBootTest`, Testcontainers)
- Add test case to coding question → 201 and row persisted with correct ordinal
- `timeoutSeconds = 0` → 400
- `timeoutSeconds > 60` → 400
- `memoryMb < 64` → 400
- `memoryMb > 1024` → 400
- Delete test case → 204 and row removed from DB
- Update test case → 200 and fields reflect new values
- Add test case to non-existent question → 404

### Done When
- `coding_question` and `test_case` tables exist in the Testcontainers DB and pass `ddl-auto=validate`
- Test case CRUD endpoints return real data backed by the DB
- All `TestCaseServiceTest` cases pass

---

## Slice B: Coding Question Service & Language Validation

### Agent Brief
Implement `CodingQuestionService` — creation and retrieval of coding questions. Fix the assessment generation doc limit check so it counts both legacy `doc_question` and new `coding_question` rows. Wire the service into `CodingQuestionController`.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    CodingQuestionService.java
src/test/java/com/psybergate/dap/service/
  CodingQuestionServiceTest.java
```

### Entities
No new entities.

### Liquibase Changesets
None.

### Repositories
No new repositories. `AssessmentService` may need a query update — see Services section.

### Services

**`CodingQuestionService`**
```java
CodingQuestionResponse createCodingQuestion(UUID bankId, CodingQuestionRequest request);
// Validate language is non-null and a known Language enum value (400 if not)
// Persist CodingQuestion entity with category = bankId-resolved-category, question, language
// Return CodingQuestionResponse (testCases will be empty on creation)

CodingQuestionResponse getCodingQuestion(UUID id);
// Use @EntityGraph to eagerly load testCases in one query
// Return CodingQuestionResponse
```

**`AssessmentService`** — update doc/coding limit check
```java
// The doc question limit check must count both DocQuestion and CodingQuestion rows
// for the assessment being generated. Update the count query to include both subtypes.
// Add a comment on the query explaining the multi-subtype count.
```

### Controllers
Wire `CodingQuestionService.createCodingQuestion()` and `getCodingQuestion()` into `CodingQuestionController` create/get stubs.

### Frontend Type Definitions
No changes in this slice.

### Frontend Services
No changes in this slice.

### Frontend Components
None.

### Route Additions
None.

### Testing

**`CodingQuestionServiceTest`** (`@SpringBootTest`, Testcontainers)
- Create with `language = JAVA` → 201 and language persisted
- Create with `language = null` → 400
- Create with an invalid language string (e.g. `"RUBY"`) → 400
- Get coding question → response includes correct language and empty test case list
- Doc limit: assessment already has one legacy `doc_question` + attempt to add one `coding_question` → 409
- Doc limit: assessment already has one `coding_question` + attempt to add another → 409

### Done When
- Coding question creation persists to DB with correct language
- Invalid/null language rejected with 400
- Doc limit check blocks the second doc-or-coding question in an assessment

---

## Slice C: Angular Question Editor — Coding Question Form

### Agent Brief
Add the coding question creation and edit form. Replace the "Doc Question" option in the question bank creation menu with a "Coding Question" option. The form requires a language selection before allowing test case rows to be added. Wire the form to the real `CodingQuestionService` API calls.

### Package Tree Additions

**Frontend**
```
src/app/features/
  question-bank/
    components/
      coding-question-form/
        coding-question-form.component.ts
        coding-question-form.component.html
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
No new services.

### Controllers
None.

### Frontend Type Definitions
No changes in this slice.

### Frontend Services
Wire stub methods to real API calls in `CodingQuestionService`:
- `createCodingQuestion()` → `POST /api/question-banks/{bankId}/coding-questions`
- `addTestCase()` → `POST /api/coding-questions/{questionId}/test-cases`
- `updateTestCase()` → `PUT /api/coding-questions/{questionId}/test-cases/{testCaseId}`
- `deleteTestCase()` → `DELETE /api/coding-questions/{questionId}/test-cases/{testCaseId}`

### Frontend Components

**`CodingQuestionFormComponent`**
- Standalone, `OnPush` change detection
- Inputs via `input()` signal API: `bankId: string`, `existingQuestion?: CodingQuestionResponse`
- Language dropdown: required; options Java, Python, C# (mapped from `Language` enum)
- Test case editor panel: visible only when a language is selected
  - Each row: input textarea, expected output textarea (required), timeout number input (1–60), memory MB number input (64–1024)
  - "Add row" button appends a blank row
  - "Remove" button on each row deletes that row
- On submit: call `createCodingQuestion()` with `language` and `question`; then persist each test case row via `addTestCase()` sequentially
- Reactive forms only — no template-driven forms

**Question bank creation menu**
- Remove "Doc Question" option
- Add "Coding Question" option that opens `CodingQuestionFormComponent`

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Language dropdown renders exactly three options: Java, Python, C#
- Language is required — form is invalid when language is not selected
- Test case panel is hidden when no language is selected; visible once a language is chosen
- "Add row" appends a new blank test case row
- "Remove" on a row deletes only that row
- Submit with valid data calls `CodingQuestionService.createCodingQuestion()` with correct payload
- Submit with missing language does not call the service

### Done When
- Marker can open the "Coding Question" form, select a language, add test cases, and submit
- The "Doc Question" option is absent from the creation menu
- All unit tests pass with no TypeScript errors

---

## Slice D: Angular Question Bank — Language Badge & Test Case Count

### Agent Brief
Update the question bank question list and card components to display a language badge and test case count on every coding question. Update the question detail view to show test cases in read-only mode. No backend changes.

### Package Tree Additions

**Frontend**
```
src/app/features/
  question-bank/
    components/
      question-card/
        question-card.component.ts      ← updated
        question-card.component.html    ← updated
      question-detail/
        question-detail.component.ts    ← updated
        question-detail.component.html  ← updated
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
Wire `getTestCases()` in `CodingQuestionService` to real API call:
- `getTestCases()` → `GET /api/coding-questions/{questionId}/test-cases`

### Controllers
None.

### Frontend Type Definitions
No changes.

### Frontend Services
No changes beyond the `getTestCases()` wire-up above.

### Frontend Components

**`QuestionCardComponent`** — updates
- For questions with `type === 'CODING_QUESTION'`: display a language badge (`"Java"` / `"Python"` / `"C#"`)
- Display test case count alongside the badge: e.g., `"Java · 3 test cases"` or `"Java · 0 test cases"`
- Badge and count are absent for all other question types

**`QuestionDetailComponent`** — updates
- For coding questions: show a read-only test case list (input, expected output, timeout, memory)
- Each row is read-only; no edit controls in this view

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Badge renders `"Java"` for a coding question with `language = JAVA`
- Badge is absent for MCQ, text, and group questions
- Count displays the correct number (e.g., `"3 test cases"`)
- Zero test cases shows `"0 test cases"`
- Detail view renders one row per test case from mocked data

### Done When
- Language badge and test case count visible on all coding question cards
- Question detail view shows read-only test case list for coding questions
- All unit tests pass
