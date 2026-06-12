# Phase 1 — Coding Question

> **Epic:** Phase 1 — Coding Question
> **Delivery:** Slice 0 merges first; Slices A–D run in parallel (one dev each).
> **Dependency:** Phases 0–5 fully merged. **Question Model Refactor** (`.claude/specs/v1-assessment-platform/phase-6-question-model-refactor.md`) fully merged — `category` has been removed from all question DTOs/tables in favour of `questionBankIds`/`questionBanks` (`Set<QuestionBank>`).
> **Jira Epic:** ATG-58 | **Slice 0:** ATG-59 | **Slice A:** ATG-60 | **Slice B:** ATG-61 | **Slice C:** ATG-62 | **Slice D:** ATG-63

### Key Design Decisions
- `CodingQuestion` extends `AssessmentQuestion` as a new `TABLE_PER_CLASS` subtype — mirrors `McqQuestion`, `DocQuestion`, etc.
- `CodingQuestionRequest` implements the existing `QuestionRequest` sealed interface with `"type": "CODING"` — creation goes through the existing `POST /api/questions` endpoint, not a new one
- `CodingQuestionResponse` implements the existing `QuestionResponse` sealed interface with `"type": "CODING"`
- `QuestionService.create()` gets a new `instanceof CodingQuestionRequest` branch — same dispatch pattern as existing types
- `language` is **required** (NOT NULL); candidates submit source code as inline text (no file upload)
- `doc_question` is **soft-deprecated**: `QuestionController.createQuestion()` returns HTTP 410 when the request is a `DocQuestionRequest`
- Test cases are included **inline** in `CodingQuestionRequest.testCases` and created or updated with the question via `POST /api/questions` / `PUT /api/questions/{id}` — there is **no separate test-case sub-resource**
- `QuestionService.createCodingQuestion()` iterates inline `testCases`, assigns ordinals (1, 2, 3…), and saves via `CascadeType.ALL` on `CodingQuestion.testCases`
- The doc/coding question limit (`assessment.doc-question-limit`, default 1) counts both `DocQuestion` (legacy) and `CodingQuestion` rows
- Like every other question type, `CodingQuestion` is scoped by `Set<QuestionBank>` (min 1 required) via the `question_question_bank` join table — there is no `category` column on `coding_question`
- `QuestionBankResponse` carries `questionCount: long` (backend) / `questionCount: number` (frontend) — added during Question Model Refactor implementation and present in all QB-referencing responses

---

## Slice 0: API Contracts *(merge first)*

### Agent Brief
Define all Phase 1 contracts so Slices A–D can develop simultaneously without merge conflicts:
- Add `CodingQuestionRequest` and `CodingQuestionResponse` to the existing `QuestionRequest` / `QuestionResponse` sealed interfaces
- Add the `CodingQuestionLanguage` enum, `TestCaseRequest`, and `TestCaseResponse` DTOs
- Block `DocQuestionRequest` creation in `QuestionController` (return 410)
- Add TypeScript types for `CodingQuestionLanguage`, `TestCaseRequest`, `TestCase`, and `CodingQuestionRequest`/`CodingQuestionResponse` to the frontend

No business logic — hardcoded responses only.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    CodingQuestionLanguage.java        ← new enum: JAVA, PYTHON, CSHARP
  dto/
    CodingQuestionRequest.java         ← new record; implements QuestionRequest; @JsonSubTypes name = "CODING"
    CodingQuestionResponse.java        ← new record; implements QuestionResponse; @JsonSubTypes name = "CODING"
    TestCaseRequest.java               ← new record
    TestCaseResponse.java              ← new record
    QuestionRequest.java               ← update: add CodingQuestionRequest to permits + @JsonSubTypes
    QuestionResponse.java              ← update: add CodingQuestionResponse to permits + @JsonSubTypes
```

**Frontend**
```
src/app/
  core/
    models/
      question.model.ts                ← update: add 'CODING' to QuestionType; add CodingQuestionLanguage, TestCaseRequest, TestCase, CodingQuestionRequest, CodingQuestionResponse
    services/
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

### DTOs

**`QuestionBankResponse`** — record (defined in Question Model Refactor; `questionCount` added during implementation)
```java
public record QuestionBankResponse(UUID id, String name, long questionCount) {}
```

**`CodingQuestionLanguage`** — enum in `domain/`
```java
public enum CodingQuestionLanguage { JAVA, PYTHON, CSHARP }
```

**`CodingQuestionRequest`** — record, implements `QuestionRequest`
```java
public record CodingQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,
    @NotBlank String question,
    @NotNull CodingQuestionLanguage language,
    List<@Valid TestCaseRequest> testCases   // nullable — test cases optional at creation
) implements QuestionRequest {}
```

**`CodingQuestionResponse`** — record, implements `QuestionResponse`
```java
public record CodingQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,
    String question,
    CodingQuestionLanguage language,
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
> By the time this slice merges, the Question Model Refactor will already have added `McqPlusQuestionRequest` (`"MCQ_PLUS"`). This slice adds `CodingQuestionRequest` (`"CODING"`) alongside it.
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = McqQuestionRequest.class,     name = "MCQ"),
    @JsonSubTypes.Type(value = McqPlusQuestionRequest.class, name = "MCQ_PLUS"),
    @JsonSubTypes.Type(value = DocQuestionRequest.class,     name = "DOC"),
    @JsonSubTypes.Type(value = TextQuestionRequest.class,    name = "TEXT"),
    @JsonSubTypes.Type(value = GroupQuestionRequest.class,   name = "GROUP"),
    @JsonSubTypes.Type(value = CodingQuestionRequest.class,  name = "CODING")   // ← new
})
public sealed interface QuestionRequest
    permits McqQuestionRequest, McqPlusQuestionRequest, DocQuestionRequest,
            TextQuestionRequest, GroupQuestionRequest, CodingQuestionRequest {}
```

**`QuestionResponse`** sealed interface — updated
> Same note as above — `McqPlusQuestionResponse` (`"MCQ_PLUS"`) arrives via the Question Model Refactor; this slice adds `CodingQuestionResponse` (`"CODING"`).
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = McqQuestionResponse.class,     name = "MCQ"),
    @JsonSubTypes.Type(value = McqPlusQuestionResponse.class, name = "MCQ_PLUS"),
    @JsonSubTypes.Type(value = DocQuestionResponse.class,     name = "DOC"),
    @JsonSubTypes.Type(value = TextQuestionResponse.class,    name = "TEXT"),
    @JsonSubTypes.Type(value = GroupQuestionResponse.class,   name = "GROUP"),
    @JsonSubTypes.Type(value = CodingQuestionResponse.class,  name = "CODING")  // ← new
})
public sealed interface QuestionResponse
    permits McqQuestionResponse, McqPlusQuestionResponse, DocQuestionResponse,
            TextQuestionResponse, GroupQuestionResponse, CodingQuestionResponse {}
```

### Frontend Type Definitions

**`question.model.ts`** — already complete (implemented as a full replacement by the Question Model Refactor)

> The Question Model Refactor implemented `question.model.ts` as a full file replacement, not an incremental addition. All coding types are already present. The actual shapes deviate from the original plan in three ways: (1) request types are flat interfaces — there is no `BaseQuestionRequest` interface to extend; (2) `QuestionBankResponse` has a `questionCount: number` field added during implementation; (3) a `QuestionRequest` union type is defined in the model alongside `QuestionResponse`.

Final shape of the relevant types in `question.model.ts`:

```typescript
export type QuestionType = 'MCQ' | 'MCQ_PLUS' | 'TEXT' | 'DOC' | 'GROUP' | 'CODING';

export interface QuestionBankResponse {
  id: string;
  name: string;
  questionCount: number;   // added during implementation — not in original spec
}

export type CodingQuestionLanguage = 'JAVA' | 'PYTHON' | 'CSHARP';

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

// Flat interface — does NOT extend BaseQuestionRequest
export interface CodingQuestionRequest {
  type: 'CODING';
  questionBankIds: string[];
  question: string;
  language: CodingQuestionLanguage;
  testCases?: TestCaseRequest[];
}

export interface BaseQuestionResponse {
  type: QuestionType;
  id: string;
  questionBanks: QuestionBankResponse[];
  question: string;
}

export interface CodingQuestionResponse extends BaseQuestionResponse {
  type: 'CODING';
  language: CodingQuestionLanguage;
  testCases: TestCase[];
}

export type QuestionRequest =
  | McqQuestionRequest | McqPlusQuestionRequest | TextQuestionRequest
  | DocQuestionRequest | GroupQuestionRequest   | CodingQuestionRequest;

export type QuestionResponse =
  | McqQuestionResponse | McqPlusQuestionResponse | TextQuestionResponse
  | DocQuestionResponse | GroupQuestionResponse   | CodingQuestionResponse;
```

### Frontend Services

**`question.service.ts`** — use `QuestionRequest` type alias (already includes all types from the model)
```typescript
createQuestion(request: QuestionRequest): Observable<QuestionResponse>
updateQuestion(id: string, request: QuestionRequest): Observable<QuestionResponse>
```

### Frontend Components
None in this slice.

### Route Additions
None in this slice.

### Testing
- `POST /api/questions` with `"type": "CODING"` and valid body → 201 (hardcoded)
- `POST /api/questions` with `"type": "DOC"` → 410 with deprecation message
- Non-MARKER/ADMIN caller → 403 on `POST /api/questions`
- `QuestionRequest` and `QuestionResponse` sealed interfaces compile with `CodingQuestionRequest`/`CodingQuestionResponse` in permits
- Angular `question.model.ts` compiles with no TypeScript errors; `CodingQuestionLanguage`, `TestCase`, `CodingQuestionRequest`, `CodingQuestionResponse` all accessible from the same file

### Done When
- ✅ All backend DTOs compile (`CodingQuestionRequest`, `CodingQuestionResponse`, `TestCaseRequest`, `TestCaseResponse`, `CodingQuestionLanguage`, `QuestionBankResponse` with `questionCount`)
- ✅ `QuestionRequest`/`QuestionResponse` sealed interfaces include `CODING` subtype
- ✅ `POST /api/questions` with `"type": "DOC"` returns 410
- ✅ `QuestionType` in Angular includes `'CODING'`
- ✅ Angular `question.model.ts` compiles with all coding types present; `QuestionRequest` union includes `CodingQuestionRequest`
- `question.service.ts` uses `QuestionRequest` type alias for `createQuestion`/`updateQuestion` signatures
- Merged to `main` before any other Phase 1 slice begins

---

## Slice A: Schema Migration & Test Case CRUD

### Agent Brief
Create the `coding_question` and `test_case` DB tables via Liquibase. Implement the `CodingQuestion` and `TestCase` JPA entities, `CodingQuestionRepository`, and `TestCaseRepository`. No `QuestionService` changes yet — that is Slice B. No `TestCaseService` is needed: test cases are persisted inline via `CascadeType.ALL` in Slice B.

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
src/main/resources/db/changelog/changesets/
  2026-06-05-001-create-coding-question-table.xml
  2026-06-05-002-create-test-case-table.xml
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
    private CodingQuestionLanguage language;

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

> **Note on columns:** `question`, `created_at`, `updated_at` are inherited from `BaseEntity`/`AssessmentQuestion` but must be declared here because `TABLE_PER_CLASS` creates a fully independent table. Match the column definitions used in existing subtypes (`mcq_question`, `doc_question`, etc.) in the baseline changeset.
>
> **No `category` column:** the Question Model Refactor removed `category` from every concrete question table. `CodingQuestion` is scoped to one or more `QuestionBank`s exclusively via the `question_question_bank` join table (created in that refactor's Slice A) — `coding_question_id` values appear there with no FK, following the same `TABLE_PER_CLASS` pattern as `assessment_question_link`. No additional join-table changeset is needed in this slice.

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
No new service in this slice. Test case persistence is handled inline in `QuestionService` (Slice B) via `CascadeType.ALL` on `CodingQuestion.testCases`.

### Controllers
None.

### Frontend Type Definitions
No changes in this slice.

### Frontend Services
No changes in this slice.

### Frontend Components
None.

### Route Additions
None.

### Testing

- `coding_question` table exists in Testcontainers DB and passes `ddl-auto=validate`
- `test_case` table exists in Testcontainers DB and passes `ddl-auto=validate`
- `CodingQuestion` and `TestCase` entities compile and Hibernate validates the schema
- `CodingQuestionRepository` and `TestCaseRepository` can be injected without errors

### Done When
- `coding_question` and `test_case` tables exist in Testcontainers DB and pass `ddl-auto=validate`
- Entities and repositories compile and load cleanly in the Spring context

---

## Slice B: Coding Question in QuestionService & Language Validation

### Agent Brief
Extend `QuestionService` to handle `CodingQuestion` — following the exact same pattern used for `McqQuestion`, `DocQuestion`, etc. Add a `createCodingQuestion()` private method and a `toCodingQuestionResponse()` private method. Constructor-inject `CodingQuestionRepository`. Fix the assessment doc limit check to count both `DocQuestion` and `CodingQuestion` rows. Reuse the `resolveQuestionBanks(List<UUID>)` and `toQbResponses(Set<QuestionBank>)` helpers added to `QuestionService` by the Question Model Refactor — `CodingQuestion` is scoped by `questionBanks` like every other question type, with no `category`.

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
    // resolveQuestionBanks() throws ValidationException if any questionBankId is not found
    CodingQuestion q = new CodingQuestion();
    q.setQuestionBanks(resolveQuestionBanks(request.questionBankIds()));
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
    return new CodingQuestionResponse(q.getId(), toQbResponses(q.getQuestionBanks()), q.getQuestion(), q.getLanguage(), testCases);
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
- `POST /api/questions` with `"type": "CODING"`, `language: "JAVA"`, and a valid `questionBankIds` → 201, language and question banks persisted
- `POST /api/questions` with `"type": "CODING"` and missing `language` → 400 (bean validation)
- `POST /api/questions` with `"type": "CODING"` and an unknown `questionBankIds` entry → 400 (`ValidationException`, same as other question types)
- `POST /api/questions` with `"type": "CODING"` and empty `questionBankIds` → 400 (`@NotEmpty`)
- `GET /api/questions/{id}` for a coding question → response includes `language`, `questionBanks`, and `testCases: []`
- Doc limit: assessment already has one legacy `DocQuestion` + attempt to add one `CodingQuestion` → 409
- Doc limit: assessment has one `CodingQuestion` + attempt to add another → 409

### Done When
- `POST /api/questions` with `"type": "CODING"` creates a `CodingQuestion` and returns `CodingQuestionResponse`
- Missing `language` field returns 400
- Unknown or empty `questionBankIds` returns 400
- Doc limit check blocks second doc-or-coding question in an assessment (409)
- All `CodingQuestionServiceTest` cases pass

---

## Slice C: Angular Question Editor — Coding Question Form

### Agent Brief
Add the coding question creation and edit form. Replace the "Doc Question" option in the question bank creation menu with a "Coding Question" option. The form requires a language selection before test case rows can be added. On submit, call `QuestionService.createQuestion()` with a `CodingQuestionRequest` (type `'CODING'`) including the inline `testCases` list — no separate service call for test case management.

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
No new service in this slice.

**`question.service.ts`** — no new methods; existing `createQuestion()` already accepts `CodingQuestionRequest` (union updated in Slice 0). Test cases are sent inline in the request body.

### Controllers
None.

### Frontend Components

**`CodingQuestionFormComponent`**
- Standalone, `OnPush` change detection
- Inputs via `input()` signal API: `existingQuestion?: CodingQuestionResponse`
- `QuestionBankSelectorComponent` (from the Question Model Refactor) replaces any category input — at least one question bank must be selected; supports inline QB creation
- Language dropdown: required; options Java, Python, C# (mapped from `CodingQuestionLanguage` type)
- Test case editor panel: visible only when a language is selected
  - Each row: input textarea, expected output textarea (required), timeout number input (1–60), memory MB number input (64–1024)
  - "Add row" button appends a blank row; "Remove" button on each row deletes that row
- On submit: call `questionService.createQuestion({ type: 'CODING', questionBankIds, question, language, testCases: [...] })` — test cases are sent inline in the request body
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
- `QuestionBankSelectorComponent` is rendered; form invalid until at least one question bank is selected
- Test case panel hidden when no language; visible once language selected
- "Add row" appends a new blank test case row
- "Remove" on a row deletes only that row
- Submit with valid data calls `QuestionService.createQuestion()` with `type: 'CODING'`, `questionBankIds`, and correct payload
- Submit with missing language does not call the service

### Done When
- Marker can open "Coding Question" form, select a language and at least one question bank, add test cases, and submit
- "Doc Question" option is absent from the creation menu
- All unit tests pass with no TypeScript errors

---

## Slice D: Angular Question Bank — Language Badge & Test Case Count

### Agent Brief
Update the question bank question list and card components to display a language badge and test case count on every coding question (`type === 'CODING'`). Update the question detail view to show test cases in read-only mode. Test cases are already present in `CodingQuestionResponse.testCases` — no separate API call needed.

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
No service changes. Test cases are already present in `CodingQuestionResponse.testCases` — no separate API call needed.

### Controllers
None.

### Frontend Components

**`QuestionCardComponent`** — updates
- For questions with `type === 'CODING'`: display a language badge (`"Java"` / `"Python"` / `"C#"`) and test case count from `question.testCases.length` (`"Java · 3 test cases"` / `"Java · 0 test cases"`)
- Badge and count absent for all other question types

**`QuestionDetailComponent`** — updates
- For coding questions: read `question.testCases` directly from the already-loaded `CodingQuestionResponse`
- Render a read-only test case list (input, expected output, timeout, memory)

### Route Additions
None.

### Testing

Unit tests (`TestBed`):
- Badge renders `"Java"` for `type === 'CODING'` with `language === 'JAVA'`
- Badge absent for MCQ, TEXT, GROUP questions
- Count displays correct number; zero shows `"0 test cases"`
- Detail view renders one row per test case from `question.testCases` (inline in response)

### Done When
- Language badge and test case count visible on all coding question cards
- Question detail view shows read-only test case list for coding questions
- All unit tests pass
