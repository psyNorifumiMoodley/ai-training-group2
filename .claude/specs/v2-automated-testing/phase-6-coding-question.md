# Phase 6 — Coding Question

> **Epic:** Phase 6 — Coding Question
> **Delivery:** Slice 0 merges first; Slices A–D run in parallel (one dev each).
> **Dependency:** Phases 0–5 fully merged.
> **Jira Epic:** ATG-58 | **Slice 0:** ATG-59 | **Slice A:** ATG-60 | **Slice B:** ATG-61 | **Slice C:** ATG-62 | **Slice D:** ATG-63

### Key Design Decisions
- `CodingQuestion` extends `AssessmentQuestion` as a new `TABLE_PER_CLASS` subtype — mirrors `McqQuestion`, `DocQuestion`, etc.
- `CodingQuestionRequest` implements the existing `QuestionRequest` sealed interface with `"type": "CODING"` — creation goes through the existing `POST /api/questions` endpoint, not a new one
- `CodingQuestionResponse` implements the existing `QuestionResponse` sealed interface with `"type": "CODING"`
- `QuestionService.create()` gets a new `instanceof CodingQuestionRequest` branch — same dispatch pattern as existing types
- `language` is **required** (NOT NULL); candidates submit source code as inline text (no file upload)
- `doc_question` is **soft-deprecated**: `QuestionController.createQuestion()` returns HTTP 410 when the request is a `DocQuestionRequest`
- `CodingQuestionController` is a **new controller for test-case sub-resources only** (`/api/coding-questions/{id}/test-cases`) — it does not own question creation or retrieval
- Test cases are created after the question is saved, via the test-case sub-resource endpoints
- The doc/coding question limit (`assessment.doc-question-limit`, default 1) counts both `DocQuestion` (legacy) and `CodingQuestion` rows

---

## Slice 0: API Contracts *(merge first)*

### Agent Brief
Define all Phase 6 contracts so Slices A–D can develop simultaneously without merge conflicts:
- Add `CodingQuestionRequest` and `CodingQuestionResponse` to the existing `QuestionRequest` / `QuestionResponse` sealed interfaces
- Add the `Language` enum, `TestCaseRequest`, and `TestCaseResponse` DTOs
- Stub `CodingQuestionController` for test-case sub-resources only
- Block `DocQuestionRequest` creation in `QuestionController` (return 410)
- Add TypeScript types and a stub `CodingQuestionService` (test-case operations only) to the frontend

No business logic — hardcoded responses only.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    Language.java                      ← new enum: JAVA, PYTHON, CSHARP
  dto/
    CodingQuestionRequest.java         ← new record; implements QuestionRequest; @JsonSubTypes name = "CODING"
    CodingQuestionResponse.java        ← new record; implements QuestionResponse; @JsonSubTypes name = "CODING"
    TestCaseRequest.java               ← new record
    TestCaseResponse.java              ← new record
    QuestionRequest.java               ← update: add CodingQuestionRequest to permits + @JsonSubTypes
    QuestionResponse.java              ← update: add CodingQuestionResponse to permits + @JsonSubTypes
  controller/
    CodingQuestionController.java      ← new stub (test-case sub-resources only)
```

**Frontend**
```
src/app/
  core/
    models/
      question.model.ts                ← update: add 'CODING' to QuestionType, add CodingQuestionRequest, CodingQuestionResponse
      coding-question.model.ts         ← new: TestCase, TestCaseRequest interfaces
    services/
      coding-question.service.ts       ← new stub: test-case operations only
      question.service.ts              ← update: add CodingQuestionRequest to createQuestion / updateQuestion union types
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

**`QuestionController`** — add deprecation guard (no other changes)
```java
@PostMapping
public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody QuestionRequest request) {
    if (request instanceof DocQuestionRequest) {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(/* ErrorResponse */ null);  // stub: return 410 with message
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
}
```
Return body for 410:
```json
{ "status": 410, "error": "Gone", "message": "Doc question creation is deprecated. Use POST /api/questions with type CODING instead.", "timestamp": "<now>" }
```

**`CodingQuestionController`** — new, stubs for test-case sub-resources only
```
POST   /api/coding-questions/{questionId}/test-cases                → 201 (hardcoded)
GET    /api/coding-questions/{questionId}/test-cases                → 200 [] (hardcoded)
PUT    /api/coding-questions/{questionId}/test-cases/{testCaseId}   → 200 (hardcoded)
DELETE /api/coding-questions/{questionId}/test-cases/{testCaseId}   → 204 (hardcoded)
```
All endpoints secured with `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")`.

### DTOs

**`Language`** — enum in `domain/`
```java
public enum Language { JAVA, PYTHON, CSHARP }
```

**`CodingQuestionRequest`** — record, implements `QuestionRequest`
```java
public record CodingQuestionRequest(
    @NotBlank String category,
    @NotBlank String question,
    @NotNull Language language,
    List<@Valid TestCaseRequest> testCases   // nullable — test cases added via sub-resource after creation
) implements QuestionRequest {}
```

**`CodingQuestionResponse`** — record, implements `QuestionResponse`
```java
public record CodingQuestionResponse(
    UUID id,
    String category,
    String question,
    Language language,
    List<TestCaseResponse> testCases
) implements QuestionResponse {}
```

**`TestCaseRequest`** — record
```java
public record TestCaseRequest(
    String input,
    @NotBlank String expectedOutput,
    @Min(1) @Max(60) int timeoutSeconds,
    @Min(64) @Max(1024) int memoryMb
) {}
```

**`TestCaseResponse`** — record
```java
public record TestCaseResponse(
    UUID id,
    String input,
    String expectedOutput,
    int timeoutSeconds,
    int memoryMb,
    int ordinal
) {}
```

**`QuestionRequest`** sealed interface — updated
```java
@JsonSubTypes({
    @JsonSubTypes.Type(value = McqQuestionRequest.class,    name = "MCQ"),
    @JsonSubTypes.Type(value = DocQuestionRequest.class,    name = "DOC"),
    @JsonSubTypes.Type(value = TextQuestionRequest.class,   name = "TEXT"),
    @JsonSubTypes.Type(value = GroupQuestionRequest.class,  name = "GROUP"),
    @JsonSubTypes.Type(value = CodingQuestionRequest.class, name = "CODING")   // ← new
})
public sealed interface QuestionRequest
    permits McqQuestionRequest, DocQuestionRequest, TextQuestionRequest, GroupQuestionRequest, CodingQuestionRequest {}
```

**`QuestionResponse`** sealed interface — updated
```java
@JsonSubTypes({
    @JsonSubTypes.Type(value = McqQuestionResponse.class,    name = "MCQ"),
    @JsonSubTypes.Type(value = TextQuestionResponse.class,   name = "TEXT"),
    @JsonSubTypes.Type(value = DocQuestionResponse.class,    name = "DOC"),
    @JsonSubTypes.Type(value = GroupQuestionResponse.class,  name = "GROUP"),
    @JsonSubTypes.Type(value = CodingQuestionResponse.class, name = "CODING")  // ← new
})
public sealed interface QuestionResponse
    permits McqQuestionResponse, TextQuestionResponse, DocQuestionResponse, GroupQuestionResponse, CodingQuestionResponse {}
```

### Frontend Type Definitions

**`question.model.ts`** — updated (additions only)
```typescript
// Add CODING to the union type
export type QuestionType = 'MCQ' | 'TEXT' | 'DOC' | 'GROUP' | 'CODING';

// Add request interface
export interface CodingQuestionRequest {
  type: 'CODING';
  category: string;
  question: string;
  language: Language;
  testCases?: TestCaseRequest[];
}

// Add response interface
export interface CodingQuestionResponse extends BaseQuestionResponse {
  type: 'CODING';
  language: Language;
  testCases: TestCase[];
}

// Update the QuestionResponse union
export type QuestionResponse =
  | McqQuestionResponse
  | TextQuestionResponse
  | DocQuestionResponse
  | GroupQuestionResponse
  | CodingQuestionResponse;    // ← new
```

**`coding-question.model.ts`** — new file
```typescript
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
```

### Frontend Services

**`question.service.ts`** — update union types only
```typescript
// Update createQuestion and updateQuestion signatures to include CodingQuestionRequest:
createQuestion(request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest | CodingQuestionRequest): Observable<QuestionResponse>
updateQuestion(id: string, request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest | CodingQuestionRequest): Observable<QuestionResponse>
```

**`CodingQuestionService`** — new stub; test-case operations only; returning `EMPTY`
```typescript
addTestCase(questionId: string, request: TestCaseRequest): Observable<TestCase>
getTestCases(questionId: string): Observable<TestCase[]>
updateTestCase(questionId: string, testCaseId: string, request: TestCaseRequest): Observable<TestCase>
deleteTestCase(questionId: string, testCaseId: string): Observable<void>
```
Base URL: `/api/coding-questions`

### Frontend Components
None in this slice.

### Route Additions
None in this slice.

### Testing
- `POST /api/questions` with `"type": "CODING"` and valid body → 201 (hardcoded)
- `POST /api/questions` with `"type": "DOC"` → 410 with deprecation message
- Non-MARKER/ADMIN caller → 403 on all coding-question endpoints
- All test-case stub endpoints return correct HTTP status codes
- `QuestionRequest` and `QuestionResponse` sealed interfaces compile with `CodingQuestionRequest`/`CodingQuestionResponse` in permits
- Angular `question.model.ts` and `coding-question.model.ts` compile with no TypeScript errors

### Done When
- All backend stubs compile and return hardcoded responses
- `POST /api/questions` with `"type": "DOC"` returns 410
- `QuestionType` in Angular includes `'CODING'`
- `CodingQuestionService` and model files compile with no TypeScript errors
- Merged to `main` before any other Phase 6 slice begins

---

## Slice A: Schema Migration & Test Case CRUD

### Agent Brief
Create the `coding_question` and `test_case` DB tables via Liquibase. Implement the `CodingQuestion` and `TestCase` JPA entities, `CodingQuestionRepository`, `TestCaseRepository`, and `TestCaseService`. Wire the test-case CRUD endpoints in `CodingQuestionController`. No `QuestionService` changes yet — that is Slice B.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    CodingQuestion.java                ← new entity, TABLE_PER_CLASS subtype of AssessmentQuestion
    TestCase.java                      ← new entity
  repository/
    CodingQuestionRepository.java      ← new
    TestCaseRepository.java            ← new
  service/
    TestCaseService.java               ← new
src/main/resources/db/changelog/changesets/
  2026-06-05-001-create-coding-question-table.xml
  2026-06-05-002-create-test-case-table.xml
src/test/java/com/psybergate/dap/service/
  TestCaseServiceTest.java
```

### Entities

**`CodingQuestion`** — new TABLE_PER_CLASS subtype of `AssessmentQuestion`
```java
@Entity
@Table(name = "coding_question")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingQuestion extends AssessmentQuestion {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @OneToMany(mappedBy = "codingQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<TestCase> testCases = new ArrayList<>();
}
```

**`TestCase`**
```java
@Entity
@Table(name = "test_case")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coding_question_id", nullable = false)
    @ToString.Exclude
    private CodingQuestion codingQuestion;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private int timeoutSeconds;

    @Column(nullable = false)
    private int memoryMb;

    @Column(nullable = false)
    private int ordinal;
}
```

### Liquibase Changesets

**`2026-06-05-001-create-coding-question-table.xml`**
```xml
<changeSet id="2026-06-05-001-create-coding-question-table" author="developer">
  <createTable tableName="coding_question">
    <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
    <column name="category" type="VARCHAR(255)"><constraints nullable="false"/></column>
    <column name="question" type="TEXT"><constraints nullable="false"/></column>
    <column name="language" type="VARCHAR(20)"><constraints nullable="false"/></column>
    <column name="created_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
    <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
  </createTable>
  <addForeignKeyConstraint
    baseTableName="coding_question" baseColumnNames="id"
    referencedTableName="assessment_question" referencedColumnNames="id"
    constraintName="fk_coding_question_assessment_question"/>
  <rollback><dropTable tableName="coding_question"/></rollback>
</changeSet>
```

> **Note on columns:** `category`, `question`, `created_at`, `updated_at` are inherited from `BaseEntity`/`AssessmentQuestion` but must be declared here because `TABLE_PER_CLASS` creates a fully independent table. Match the column definitions used in existing subtypes (`mcq_question`, `doc_question`, etc.) in the baseline changeset.

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
    <column name="created_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
    <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
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
// No custom query methods needed in this slice
```

**`TestCaseRepository extends JpaRepository<TestCase, UUID>`**
```java
List<TestCase> findByCodingQuestionIdOrderByOrdinalAsc(UUID codingQuestionId);
int countByCodingQuestionId(UUID codingQuestionId);
```

### Services

**`TestCaseService`**
```java
@Service
public class TestCaseService {

    // Constructor-inject CodingQuestionRepository, TestCaseRepository

    @Transactional
    public TestCaseResponse addTestCase(UUID questionId, TestCaseRequest request) {
        // Load CodingQuestion — throw NoSuchElementException (→ 404) if not found
        // Set ordinal = countByCodingQuestionId + 1
        // Build and save TestCase; return TestCaseResponse
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestCases(UUID questionId) { ... }

    @Transactional
    public TestCaseResponse updateTestCase(UUID questionId, UUID testCaseId, TestCaseRequest request) {
        // Load TestCase; verify codingQuestion.id == questionId (throw NoSuchElementException if not)
        // Update fields; save; return TestCaseResponse
    }

    @Transactional
    public void deleteTestCase(UUID questionId, UUID testCaseId) { ... }
}
```

Validation constraints on `TestCaseRequest` (`@Min`, `@Max`, `@NotBlank`) are enforced by `@Valid` in the controller.

### Controllers
Wire `TestCaseService` into `CodingQuestionController` stub methods (replace hardcoded responses with real service calls).

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
- Add test case to existing coding question → 201, row persisted with correct ordinal
- `timeoutSeconds = 0` → 400
- `timeoutSeconds > 60` → 400
- `memoryMb < 64` → 400
- `memoryMb > 1024` → 400
- Delete test case → 204, row removed from DB
- Update test case → 200, fields reflect new values
- Add test case to non-existent question → 404

### Done When
- `coding_question` and `test_case` tables exist in Testcontainers DB and pass `ddl-auto=validate`
- Test-case CRUD endpoints return real DB-backed data
- All `TestCaseServiceTest` cases pass

---

## Slice B: Coding Question in QuestionService & Language Validation

### Agent Brief
Extend `QuestionService` to handle `CodingQuestion` — following the exact same pattern used for `McqQuestion`, `DocQuestion`, etc. Add a `createCodingQuestion()` private method and a `toCodingQuestionResponse()` private method. Constructor-inject `CodingQuestionRepository`. Fix the assessment doc limit check to count both `DocQuestion` and `CodingQuestion` rows.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    QuestionService.java               ← updated (new branches; CodingQuestionRepository injected)
src/test/java/com/psybergate/dap/service/
  CodingQuestionServiceTest.java
```

### Entities
No new entities.

### Liquibase Changesets
None.

### Repositories
No new repositories. `CodingQuestionRepository` (from Slice A) is constructor-injected into `QuestionService`.

### Services

**`QuestionService`** — additions
```java
// Constructor: add CodingQuestionRepository codingQuestionRepository

// In create():
if (request instanceof CodingQuestionRequest r) {
    return toCodingQuestionResponse(createCodingQuestion(r));
}

// New private method:
private CodingQuestion createCodingQuestion(CodingQuestionRequest request) {
    // language is @NotNull — validated by @Valid at controller level; no extra check needed
    CodingQuestion q = new CodingQuestion();
    q.setCategory(request.category());
    q.setQuestion(request.question());
    q.setLanguage(request.language());
    return codingQuestionRepository.save(q);
}

// In toResponse():
if (q instanceof CodingQuestion cq) {
    return toCodingQuestionResponse(cq);
}

// New private method:
private CodingQuestionResponse toCodingQuestionResponse(CodingQuestion q) {
    List<TestCaseResponse> testCases = q.getTestCases().stream()
        .map(tc -> new TestCaseResponse(tc.getId(), tc.getInput(), tc.getExpectedOutput(),
                                        tc.getTimeoutSeconds(), tc.getMemoryMb(), tc.getOrdinal()))
        .toList();
    return new CodingQuestionResponse(q.getId(), q.getCategory(), q.getQuestion(), q.getLanguage(), testCases);
}
```

**`AssessmentService`** — update doc/coding question limit check
```java
// The doc question limit check must count both DocQuestion and CodingQuestion rows.
// Update the count query to query both subtypes from assessmentQuestionRepository.
// Add a comment explaining the multi-subtype count.
```

### Controllers
No new controller changes — `QuestionController` already routes `POST /api/questions` to `QuestionService.create()`.

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
- `POST /api/questions` with `"type": "CODING"` and `language: "JAVA"` → 201, language persisted
- `POST /api/questions` with `"type": "CODING"` and missing `language` → 400 (bean validation)
- `GET /api/questions/{id}` for a coding question → response includes `language` and `testCases: []`
- Doc limit: assessment already has one legacy `DocQuestion` + attempt to add one `CodingQuestion` → 409
- Doc limit: assessment has one `CodingQuestion` + attempt to add another → 409

### Done When
- `POST /api/questions` with `"type": "CODING"` creates a `CodingQuestion` and returns `CodingQuestionResponse`
- Missing `language` field returns 400
- Doc limit check blocks second doc-or-coding question in an assessment (409)
- All `CodingQuestionServiceTest` cases pass

---

## Slice C: Angular Question Editor — Coding Question Form

### Agent Brief
Add the coding question creation and edit form. Replace the "Doc Question" option in the question bank creation menu with a "Coding Question" option. The form requires a language selection before test case rows can be added. On submit, call `QuestionService.createQuestion()` with a `CodingQuestionRequest` (type `'CODING'`), then persist test case rows via `CodingQuestionService`.

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
Wire stub methods to real API calls:

**`question.service.ts`** — no new methods; existing `createQuestion()` accepts `CodingQuestionRequest` (union already updated in Slice 0)

**`coding-question.service.ts`** — wire stubs to real calls
```typescript
addTestCase(questionId: string, request: TestCaseRequest): Observable<TestCase>
  → POST /api/coding-questions/{questionId}/test-cases
updateTestCase(questionId: string, testCaseId: string, request: TestCaseRequest): Observable<TestCase>
  → PUT /api/coding-questions/{questionId}/test-cases/{testCaseId}
deleteTestCase(questionId: string, testCaseId: string): Observable<void>
  → DELETE /api/coding-questions/{questionId}/test-cases/{testCaseId}
```

### Controllers
None.

### Frontend Components

**`CodingQuestionFormComponent`**
- Standalone, `OnPush` change detection
- Inputs via `input()` signal API: `existingQuestion?: CodingQuestionResponse`
- Language dropdown: required; options Java, Python, C# (mapped from `Language` type)
- Test case editor panel: visible only when a language is selected
  - Each row: input textarea, expected output textarea (required), timeout number input (1–60), memory MB number input (64–1024)
  - "Add row" button appends a blank row; "Remove" button on each row deletes that row
- On submit:
  1. Call `questionService.createQuestion({ type: 'CODING', category, question, language })` → get question ID
  2. For each test case row, call `codingQuestionService.addTestCase(id, row)` sequentially
- Reactive forms only

**Question bank creation menu**
- Remove the "Doc Question" option
- Add "Coding Question" option that opens `CodingQuestionFormComponent`

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Language dropdown renders exactly Java, Python, C#
- Language is required — form invalid when not selected
- Test case panel hidden when no language; visible once language selected
- "Add row" appends a new blank test case row
- "Remove" on a row deletes only that row
- Submit with valid data calls `QuestionService.createQuestion()` with `type: 'CODING'` and correct payload
- Submit with missing language does not call the service

### Done When
- Marker can open "Coding Question" form, select a language, add test cases, and submit
- "Doc Question" option is absent from the creation menu
- All unit tests pass with no TypeScript errors

---

## Slice D: Angular Question Bank — Language Badge & Test Case Count

### Agent Brief
Update the question bank question list and card components to display a language badge and test case count on every coding question (`type === 'CODING'`). Update the question detail view to show test cases in read-only mode. Wire `CodingQuestionService.getTestCases()` to the real API.

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

**`coding-question.service.ts`** — wire remaining stub
```typescript
getTestCases(questionId: string): Observable<TestCase[]>
  → GET /api/coding-questions/{questionId}/test-cases
```

### Controllers
None.

### Frontend Components

**`QuestionCardComponent`** — updates
- For questions with `type === 'CODING'`: display a language badge (`"Java"` / `"Python"` / `"C#"`) and test case count (`"Java · 3 test cases"` / `"Java · 0 test cases"`)
- Badge and count absent for all other question types

**`QuestionDetailComponent`** — updates
- For coding questions: call `codingQuestionService.getTestCases(question.id)` on load
- Render a read-only test case list (input, expected output, timeout, memory)

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Badge renders `"Java"` for `type === 'CODING'` with `language === 'JAVA'`
- Badge absent for MCQ, TEXT, GROUP questions
- Count displays correct number; zero shows `"0 test cases"`
- Detail view renders one row per test case from mocked `getTestCases()` response

### Done When
- Language badge and test case count visible on all coding question cards
- Question detail view shows read-only test case list for coding questions
- All unit tests pass
