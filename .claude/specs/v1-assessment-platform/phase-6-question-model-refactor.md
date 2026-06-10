# Question Model Refactor — QuestionBanks, McqPlus, Marks & GroupQuestion Separation

> **Prerequisite for:** Phase 6-8. This refactor MUST be fully merged before Phase 6 resumes.
> **Touches:** Phase 2 (question management) and Phase 3 (assessment generation).
> **Delivery:** Slice 0 merges first; Slices A–C run in parallel (one dev each).

---

## Summary of Changes

| Area | Old | New |
|---|---|---|
| Question scope | `String category` on each question | `Set<QuestionBank>` (min 1 QB required) |
| Question list filter | `?category=Java` | `?questionBankId=<uuid>` |
| QB management | No entity; conceptual only | Full CRUD + inline creation in question form |
| GroupQuestion hierarchy | `extends TextQuestion` | `extends AssessmentQuestion` directly |
| GroupQuestion children | `ManyToMany → TextQuestion` (existing questions picked from list) | `OneToMany → GroupQuestionChild` (created inline, embedded in parent) |
| Standalone TextQuestion list | Could inadvertently include group children | Never includes embedded children (architecture enforces this) |
| MCQ marks | Implied 1 | Constant 1 — no DB column needed |
| Text/Doc marks | None | Required integer `marks` field (min 1) |
| Group marks | None | Computed at runtime: sum of `children[].marks` |
| McqPlusQuestion | Does not exist | New type: MCQ + embedded text follow-up; always shown to candidate regardless of MCQ correctness; counts as MCQ in composition quota |
| McqPlusQuestion marks | N/A | 1 (MCQ part, auto-marked) + `followUpMarks` (text part, manually marked) |
| Assessment generation scope | Category string filter | `questionBankIds` list |

---

## Domain Model Changes

### Removed
- `AssessmentQuestion.category: String` — dropped from all concrete tables
- `GroupQuestion extends TextQuestion` hierarchy
- `GroupQuestion.followUpQuestions: List<TextQuestion>` (ManyToMany)
- `group_question_follow_up` join table
- `group_question.keywords` column (was always null — GroupQuestion never used it)
- `GET /api/questions/categories` endpoint (QBs replace this)

### Added
- `QuestionBank` entity: `id` (UUID, PK), `name` (VARCHAR 255, unique, not null)
- `question_question_bank` join table: `question_id` (UUID, no FK — TABLE_PER_CLASS pattern), `question_bank_id` (UUID, FK → question_bank.id)
- `AssessmentQuestion.questionBanks: Set<QuestionBank>` (ManyToMany, lazy)
- `GroupQuestionChild` entity: `id`, `group_id` (FK → group_question.id), `question_text`, `keywords`, `marks`, `display_order`
- `GroupQuestion.children: List<GroupQuestionChild>` (OneToMany, CascadeType.ALL, orphanRemoval)
- `McqPlusQuestion extends McqQuestion`: adds `follow_up_question`, `follow_up_keywords`, `follow_up_marks`
- `McqPlusResponse extends McqResponse`: adds `follow_up_answer`, `follow_up_score`
- `marks` column on `text_question` and `doc_question`

### Also Updated
- `CodingQuestionRequest` / `CodingQuestionResponse` (merged in Phase 6 Slice 0): replace `category` with `questionBankIds`/`questionBanks`

---

## Slice 0: API Contracts *(merge first)*

### Agent Brief
Update all question DTOs to remove `category` and add `questionBankIds`/`questionBanks`. Add `marks` to TextQuestion and DocQuestion DTOs. Add new `McqPlusQuestion` DTOs, `GroupChildRequest`/`GroupChildResponse`, and `QuestionBankRequest`/`QuestionBankResponse`. Update the sealed interfaces. Update `CodingQuestion` DTOs (already merged in Phase 6 Slice 0). Stub `QuestionBankController`. No business logic.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  dto/
    QuestionBankRequest.java          ← new record
    QuestionBankResponse.java         ← new record
    GroupChildRequest.java            ← new record
    GroupChildResponse.java           ← new record
    McqPlusQuestionRequest.java       ← new record; implements QuestionRequest
    McqPlusQuestionResponse.java      ← new record; implements QuestionResponse
    McqQuestionRequest.java           ← update: remove category, add questionBankIds
    McqQuestionResponse.java          ← update: remove category, add questionBanks
    TextQuestionRequest.java          ← update: remove category, add questionBankIds, add marks
    TextQuestionResponse.java         ← update: remove category, add questionBanks, add marks
    DocQuestionRequest.java           ← update: remove category, add questionBankIds, add marks (deprecated but consistent)
    DocQuestionResponse.java          ← update: remove category, add questionBanks, add marks
    GroupQuestionRequest.java         ← update: remove category; replace followUpQuestionIds with children: List<GroupChildRequest>; add questionBankIds
    GroupQuestionResponse.java        ← update: remove category; replace followUpQuestions with children: List<GroupChildResponse>; add questionBanks, totalMarks
    CodingQuestionRequest.java        ← update: remove category, add questionBankIds
    CodingQuestionResponse.java       ← update: remove category, add questionBanks
    QuestionRequest.java              ← update: add McqPlusQuestionRequest to permits + @JsonSubTypes
    QuestionResponse.java             ← update: add McqPlusQuestionResponse to permits + @JsonSubTypes
  controller/
    QuestionBankController.java       ← new stub
```

### DTO Definitions

**`QuestionBankRequest`**
```java
public record QuestionBankRequest(@NotBlank String name) {}
```

**`QuestionBankResponse`**
```java
public record QuestionBankResponse(UUID id, String name) {}
```

**`GroupChildRequest`**
```java
public record GroupChildRequest(
    @NotBlank String questionText,
    List<String> keywords,
    @Min(1) int marks
) {}
```

**`GroupChildResponse`**
```java
public record GroupChildResponse(UUID id, String questionText, List<String> keywords, int marks) {}
```

**`McqPlusQuestionRequest`** — implements `QuestionRequest`
```java
public record McqPlusQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,
    @NotBlank String question,
    @NotEmpty List<String> options,
    @NotEmpty List<String> correctAnswers,
    @NotBlank String followUpQuestion,
    List<String> followUpKeywords,
    @Min(1) int followUpMarks
) implements QuestionRequest {}
```

**`McqPlusQuestionResponse`** — implements `QuestionResponse`
```java
public record McqPlusQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,
    String question,
    List<String> options,
    List<String> correctAnswers,
    boolean multiCorrect,
    String followUpQuestion,
    List<String> followUpKeywords,
    int followUpMarks,
    int totalMarks          // always 1 + followUpMarks; computed by service
) implements QuestionResponse {}
```

**Updated `McqQuestionRequest`**
```java
public record McqQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,
    @NotBlank String question,
    @NotEmpty List<String> options,
    @NotEmpty List<String> correctAnswers
) implements QuestionRequest {}
```

**Updated `McqQuestionResponse`**
```java
public record McqQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,
    String question,
    List<String> options,
    List<String> correctAnswers,
    boolean multiCorrect
    // MCQ is always 1 mark — no marks field; constant at the type level
) implements QuestionResponse {}
```

**Updated `TextQuestionRequest`**
```java
public record TextQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,
    @NotBlank String question,
    List<String> keywords,
    @Min(1) int marks
) implements QuestionRequest {}
```

**Updated `TextQuestionResponse`**
```java
public record TextQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,
    String question,
    List<String> keywords,
    int marks
) implements QuestionResponse {}
```

**Updated `DocQuestionRequest`**
```java
public record DocQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,
    @NotBlank String question,
    @Min(1) int marks
) implements QuestionRequest {}
```

**Updated `DocQuestionResponse`**
```java
public record DocQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,
    String question,
    int marks
) implements QuestionResponse {}
```

**Updated `GroupQuestionRequest`**
```java
public record GroupQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,
    @NotBlank String question,
    boolean ordered,
    @NotEmpty @Valid List<GroupChildRequest> children
) implements QuestionRequest {}
```

**Updated `GroupQuestionResponse`**
```java
public record GroupQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,
    String question,
    boolean ordered,
    List<GroupChildResponse> children,
    int totalMarks      // sum of children[].marks; computed by service
) implements QuestionResponse {}
```

**Updated `CodingQuestionRequest`** (was merged in Phase 6 Slice 0 with `category`)
```java
public record CodingQuestionRequest(
    @NotEmpty List<UUID> questionBankIds,   // replaces @NotBlank String category
    @NotBlank String question,
    @NotNull CodingQuestionLanguage language,
    List<@Valid TestCaseRequest> testCases
) implements QuestionRequest {}
```

**Updated `CodingQuestionResponse`**
```java
public record CodingQuestionResponse(
    UUID id,
    List<QuestionBankResponse> questionBanks,   // replaces String category
    String question,
    CodingQuestionLanguage language,
    List<TestCaseResponse> testCases
) implements QuestionResponse {}
```

**Updated `QuestionRequest`** sealed interface
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = McqQuestionRequest.class,     name = "MCQ"),
    @JsonSubTypes.Type(value = McqPlusQuestionRequest.class, name = "MCQ_PLUS"),
    @JsonSubTypes.Type(value = DocQuestionRequest.class,     name = "DOC"),
    @JsonSubTypes.Type(value = TextQuestionRequest.class,    name = "TEXT"),
    @JsonSubTypes.Type(value = GroupQuestionRequest.class,   name = "GROUP"),
    @JsonSubTypes.Type(value = CodingQuestionRequest.class,  name = "CODING")
})
public sealed interface QuestionRequest
    permits McqQuestionRequest, McqPlusQuestionRequest, DocQuestionRequest,
            TextQuestionRequest, GroupQuestionRequest, CodingQuestionRequest {}
```

**Updated `QuestionResponse`** sealed interface
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = McqQuestionResponse.class,     name = "MCQ"),
    @JsonSubTypes.Type(value = McqPlusQuestionResponse.class, name = "MCQ_PLUS"),
    @JsonSubTypes.Type(value = DocQuestionResponse.class,     name = "DOC"),
    @JsonSubTypes.Type(value = TextQuestionResponse.class,    name = "TEXT"),
    @JsonSubTypes.Type(value = GroupQuestionResponse.class,   name = "GROUP"),
    @JsonSubTypes.Type(value = CodingQuestionResponse.class,  name = "CODING")
})
public sealed interface QuestionResponse
    permits McqQuestionResponse, McqPlusQuestionResponse, DocQuestionResponse,
            TextQuestionResponse, GroupQuestionResponse, CodingQuestionResponse {}
```

### Controllers

**`QuestionBankController`** — stubs only
```
GET    /api/question-banks          → 200 List<QuestionBankResponse> (empty list hardcoded)
POST   /api/question-banks          → 201 QuestionBankResponse (hardcoded)
PUT    /api/question-banks/{id}     → 200 QuestionBankResponse (hardcoded)
DELETE /api/question-banks/{id}     → 204
```
All four secured with `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")`.

**`QuestionController`** — update only
- Remove `GET /api/questions/categories` endpoint entirely
- `GET /api/questions` query param: replace `?category=` with `?questionBankId=` (no logic yet — still returns empty page)

### Frontend Type Definitions

**`question.model.ts`** — full replacement (all types updated)
```typescript
export type QuestionType = 'MCQ' | 'MCQ_PLUS' | 'TEXT' | 'DOC' | 'GROUP' | 'CODING';

export interface QuestionBankResponse { id: string; name: string; }

export interface GroupChildRequest  { questionText: string; keywords: string[]; marks: number; }
export interface GroupChildResponse { id: string; questionText: string; keywords: string[]; marks: number; }

export interface BaseQuestionRequest  { type: QuestionType; questionBankIds: string[]; question: string; }
export interface BaseQuestionResponse { id: string; type: QuestionType; questionBanks: QuestionBankResponse[]; question: string; }

export interface McqQuestionRequest extends BaseQuestionRequest {
  type: 'MCQ';
  options: string[];
  correctAnswers: string[];
}
export interface McqQuestionResponse extends BaseQuestionResponse {
  type: 'MCQ';
  options: string[];
  correctAnswers: string[];
  multiCorrect: boolean;
}

export interface McqPlusQuestionRequest extends BaseQuestionRequest {
  type: 'MCQ_PLUS';
  options: string[];
  correctAnswers: string[];
  followUpQuestion: string;
  followUpKeywords: string[];
  followUpMarks: number;
}
export interface McqPlusQuestionResponse extends BaseQuestionResponse {
  type: 'MCQ_PLUS';
  options: string[];
  correctAnswers: string[];
  multiCorrect: boolean;
  followUpQuestion: string;
  followUpKeywords: string[];
  followUpMarks: number;
  totalMarks: number;
}

export interface TextQuestionRequest  extends BaseQuestionRequest  { type: 'TEXT'; keywords: string[]; marks: number; }
export interface TextQuestionResponse extends BaseQuestionResponse { type: 'TEXT'; keywords: string[]; marks: number; }

export interface DocQuestionRequest  extends BaseQuestionRequest  { type: 'DOC'; marks: number; }
export interface DocQuestionResponse extends BaseQuestionResponse { type: 'DOC'; marks: number; }

export interface GroupQuestionRequest extends BaseQuestionRequest {
  type: 'GROUP';
  ordered: boolean;
  children: GroupChildRequest[];
}
export interface GroupQuestionResponse extends BaseQuestionResponse {
  type: 'GROUP';
  ordered: boolean;
  children: GroupChildResponse[];
  totalMarks: number;
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
export interface CodingQuestionRequest extends BaseQuestionRequest {
  type: 'CODING';
  language: CodingQuestionLanguage;
  testCases?: TestCaseRequest[];
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

**`question-bank.service.ts`** — new stub (returns `EMPTY` for all methods)
```typescript
getQuestionBanks(): Observable<QuestionBankResponse[]>
createQuestionBank(name: string): Observable<QuestionBankResponse>
renameQuestionBank(id: string, name: string): Observable<QuestionBankResponse>
deleteQuestionBank(id: string): Observable<void>
```

**`question.service.ts`** — signature updates only
- `getQuestions(page, size, questionBankId?: string)` — replaces `category?` param
- Remove `getCategories()` method

### Testing (Slice 0)
- All backend stubs compile and return correct HTTP status codes
- CANDIDATE JWT → 403 on all `/api/question-banks` endpoints
- `QuestionRequest` and `QuestionResponse` sealed interfaces compile with `McqPlusQuestionRequest`/`McqPlusQuestionResponse`
- Angular `question.model.ts` compiles with no TypeScript errors; all new types accessible

### Done When
- All backend stubs compile
- `GET /api/questions/categories` returns 404 (endpoint removed)
- `QuestionType` in Angular includes `'MCQ_PLUS'`
- `question.model.ts` compiles with no TypeScript errors
- Merged to `main` before any other slice begins

---

## Slice A: Schema Migration

### Agent Brief
Apply all DB changes for the refactor. No entity or service changes in this slice — only Liquibase changesets. Run against a Testcontainers DB to verify `ddl-auto=validate` passes at the end.

### Liquibase Changesets

All changesets authored with the developer's name. All include rollback blocks.

---

**`2026-06-10-001-ensure-question-bank-table.xml`**

The baseline schema created a `question_bank` table. Verify it has the correct structure; add missing columns if needed. The required final state: `id UUID PK`, `name VARCHAR(255) NOT NULL UNIQUE`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`, `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`.

```xml
<!-- If the table already matches, this changeset is a no-op via preconditions. -->
<!-- If it lacks created_at/updated_at or the unique constraint on name, add them here. -->
<rollback><!-- reverse any additions --></rollback>
```

---

**`2026-06-10-002-create-question-question-bank-join.xml`**

```sql
CREATE TABLE question_question_bank (
    question_id      UUID NOT NULL,
    question_bank_id UUID NOT NULL,
    PRIMARY KEY (question_id, question_bank_id)
);
ALTER TABLE question_question_bank
    ADD CONSTRAINT fk_qqb_question_bank
    FOREIGN KEY (question_bank_id) REFERENCES question_bank(id);
```

No FK on `question_id` — same TABLE_PER_CLASS pattern as `assessment_question_link`.

Rollback: `DROP TABLE question_question_bank;`

---

**`2026-06-10-003-drop-category-from-mcq-question.xml`**
```sql
ALTER TABLE mcq_question DROP COLUMN category;
```
Rollback: `ALTER TABLE mcq_question ADD COLUMN category VARCHAR(255);`

---

**`2026-06-10-004-drop-category-from-text-question.xml`**
```sql
ALTER TABLE text_question DROP COLUMN category;
```
Rollback: `ALTER TABLE text_question ADD COLUMN category VARCHAR(255);`

---

**`2026-06-10-005-drop-category-from-doc-question.xml`**
```sql
ALTER TABLE doc_question DROP COLUMN category;
```
Rollback: `ALTER TABLE doc_question ADD COLUMN category VARCHAR(255);`

---

**`2026-06-10-006-drop-category-and-keywords-from-group-question.xml`**
```sql
ALTER TABLE group_question DROP COLUMN category;
ALTER TABLE group_question DROP COLUMN keywords;
```
Rollback:
```sql
ALTER TABLE group_question ADD COLUMN category VARCHAR(255);
ALTER TABLE group_question ADD COLUMN keywords JSONB;
```

---

**`2026-06-10-007-add-marks-to-text-question.xml`**
```sql
ALTER TABLE text_question ADD COLUMN marks INTEGER NOT NULL DEFAULT 1;
```
Rollback: `ALTER TABLE text_question DROP COLUMN marks;`

---

**`2026-06-10-008-add-marks-to-doc-question.xml`**
```sql
ALTER TABLE doc_question ADD COLUMN marks INTEGER NOT NULL DEFAULT 1;
```
Rollback: `ALTER TABLE doc_question DROP COLUMN marks;`

---

**`2026-06-10-009-replace-group-follow-up-with-children.xml`**
```sql
DROP TABLE group_question_follow_up;

CREATE TABLE group_question_child (
    id            UUID                     NOT NULL,
    group_id      UUID                     NOT NULL,
    question_text TEXT                     NOT NULL,
    keywords      JSONB,
    marks         INTEGER                  NOT NULL,
    display_order INTEGER                  NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE group_question_child
    ADD CONSTRAINT fk_gqc_group
    FOREIGN KEY (group_id) REFERENCES group_question(id);

CREATE INDEX idx_gqc_group_id ON group_question_child(group_id);
```
Rollback:
```sql
DROP TABLE group_question_child;
CREATE TABLE group_question_follow_up (
    group_id      UUID    NOT NULL REFERENCES group_question(id),
    question_id   UUID    NOT NULL REFERENCES text_question(id),
    display_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, question_id)
);
```

---

**`2026-06-10-010-create-mcq-plus-question-table.xml`**
```sql
CREATE TABLE mcq_plus_question (
    id                 UUID                     NOT NULL,
    question           TEXT                     NOT NULL,
    options            JSONB                    NOT NULL,
    correct_answers    JSONB                    NOT NULL,
    follow_up_question TEXT                     NOT NULL,
    follow_up_keywords JSONB,
    follow_up_marks    INTEGER                  NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);
```
Rollback: `DROP TABLE mcq_plus_question;`

> `mcq_plus_question` owns ALL inherited columns (TABLE_PER_CLASS) — same pattern as all other concrete question tables.

---

**`2026-06-10-011-create-mcq-plus-response-table.xml`**

`McqPlusResponse` uses the existing JOINED `response` hierarchy. The `response` table's `dtype` discriminator column handles type identification.

```sql
CREATE TABLE mcq_plus_response (
    response_id       UUID    NOT NULL,
    follow_up_answer  TEXT,
    follow_up_score   INTEGER,
    PRIMARY KEY (response_id)
);

ALTER TABLE mcq_plus_response
    ADD CONSTRAINT fk_mcq_plus_response_response
    FOREIGN KEY (response_id) REFERENCES response(id);
```
Rollback: `DROP TABLE mcq_plus_response;`

---

### Done When
- All changesets apply cleanly on a fresh Testcontainers DB in sequence
- Hibernate `ddl-auto=validate` passes after all changesets applied
- `group_question_follow_up` absent; `group_question_child` and `mcq_plus_question` and `mcq_plus_response` present
- `marks` column present on `text_question` and `doc_question`
- `category` column absent from all question tables

---

## Slice B: Backend Logic

### Agent Brief
Implement all domain entity changes, new repositories, and updated service logic. Key work: `QuestionBank` entity + CRUD service, `AssessmentQuestion` ManyToMany to `QuestionBank`, `GroupQuestion` re-parented to `AssessmentQuestion` with embedded `GroupQuestionChild`, `McqPlusQuestion` and `McqPlusResponse` entities, and `QuestionService` updates. Wire `QuestionBankController` to `QuestionBankService`. Update `QuestionController` for QB-based filtering.

### Entities

**`QuestionBank`** — new entity
```java
@Entity
@Table(name = "question_bank")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestionBank extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
}
```

**`AssessmentQuestion`** — updated abstract base
- Remove `String category` field and its column annotation
- Add:
```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "question_question_bank",
    joinColumns = @JoinColumn(name = "question_id"),
    inverseJoinColumns = @JoinColumn(name = "question_bank_id")
)
@ToString.Exclude
private Set<QuestionBank> questionBanks = new HashSet<>();
```

**`TextQuestion`** — add marks field
```java
@Column(nullable = false)
private int marks;
```

**`DocQuestion`** — add marks field
```java
@Column(nullable = false)
private int marks;
```

**`GroupQuestionChild`** — new entity (NOT an AssessmentQuestion)
```java
@Entity
@Table(name = "group_question_child")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupQuestionChild extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude
    private GroupQuestion group;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywords;

    @Column(nullable = false)
    private int marks;

    @Column(nullable = false)
    private int displayOrder;
}
```

**`GroupQuestion`** — updated entity
- Change: `class GroupQuestion extends TextQuestion` → `class GroupQuestion extends AssessmentQuestion`
- Remove: `keywords` field (dropped from table; GroupQuestion never used it)
- Remove: `followUpQuestions: List<TextQuestion>` (ManyToMany) and its `@JoinTable`
- Keep: `boolean ordered` field unchanged
- Add:
```java
@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderColumn(name = "display_order")
@ToString.Exclude
private List<GroupQuestionChild> children = new ArrayList<>();
```

**`McqPlusQuestion`** — new entity extending `McqQuestion`
```java
@Entity
@Table(name = "mcq_plus_question")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class McqPlusQuestion extends McqQuestion {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String followUpQuestion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> followUpKeywords;

    @Column(nullable = false)
    private int followUpMarks;
}
```

**`McqPlusResponse`** — new entity extending `McqResponse`
```java
@Entity
@Table(name = "mcq_plus_response")
@DiscriminatorValue("MCQ_PLUS")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class McqPlusResponse extends McqResponse {

    @Column(columnDefinition = "TEXT")
    private String followUpAnswer;

    @Column(name = "follow_up_score")
    private Integer followUpScore;
}
```

### Repositories

**`QuestionBankRepository extends JpaRepository<QuestionBank, UUID>`** — new
```java
Optional<QuestionBank> findByNameIgnoreCase(String name);
boolean existsByNameIgnoreCase(String name);

// Used when deleting a QB: find questions that would be orphaned
@Query("SELECT q FROM AssessmentQuestion q JOIN q.questionBanks qb WHERE qb.id = :bankId AND SIZE(q.questionBanks) = 1")
List<AssessmentQuestion> findQuestionsWithOnlyThisBank(@Param("bankId") UUID bankId);
```

**`AssessmentQuestionRepository`** — updated
- Remove: `findByCategoryIgnoreCase()`, `findDistinctCategories()`, `findTextQuestionsByIds()`
- Add:
```java
// Filter by one QB
Page<AssessmentQuestion> findByQuestionBanksId(UUID questionBankId, Pageable pageable);

// Filter by multiple QBs (questions in ANY of the specified QBs, deduplicated)
@Query("SELECT DISTINCT q FROM AssessmentQuestion q JOIN q.questionBanks qb WHERE qb.id IN :bankIds")
Page<AssessmentQuestion> findByQuestionBankIds(@Param("bankIds") Collection<UUID> bankIds, Pageable pageable);

// Used by assessment generation (no-repeat check still applies on top)
@Query("SELECT DISTINCT q FROM AssessmentQuestion q JOIN q.questionBanks qb WHERE qb.id IN :bankIds AND q.id NOT IN :excludedIds")
List<AssessmentQuestion> findByQuestionBankIdsExcluding(
    @Param("bankIds") Collection<UUID> bankIds,
    @Param("excludedIds") Collection<UUID> excludedIds
);
```

**`GroupQuestionChildRepository extends JpaRepository<GroupQuestionChild, UUID>`** — new
```java
List<GroupQuestionChild> findByGroupIdOrderByDisplayOrderAsc(UUID groupId);
```

### Services

**`QuestionBankService`** — new service
```java
@Service
@Transactional
public class QuestionBankService {

    // Constructor-inject: QuestionBankRepository, AssessmentQuestionRepository

    public QuestionBankResponse createQuestionBank(QuestionBankRequest request);
    // Throws ConflictException if a QB with same name already exists (case-insensitive)

    @Transactional(readOnly = true)
    public List<QuestionBankResponse> listQuestionBanks();

    public QuestionBankResponse renameQuestionBank(UUID id, QuestionBankRequest request);
    // Throws ValidationException if id not found
    // Throws ConflictException if another QB with same name already exists

    public void deleteQuestionBank(UUID id);
    // Throws ValidationException if id not found
    // Throws ValidationException if any question would be left with 0 QBs after deletion
    // (check via findQuestionsWithOnlyThisBank; return message listing the affected question count)
    // On success: removes QB from all question_question_bank rows, then deletes QB
}
```

**`QuestionService`** — updated
- Constructor-inject `QuestionBankRepository` (replaces category references)
- Remove all `category` references; replace with QB resolution:
  ```java
  // Shared helper used by all create* methods:
  private Set<QuestionBank> resolveQuestionBanks(List<UUID> ids) {
      // Fetch all; throw ValidationException if any ID not found
  }
  ```
- `createMcq(McqQuestionRequest)`: set `questionBanks` from `resolveQuestionBanks()`; no marks field needed
- `createMcqPlus(McqPlusQuestionRequest)`: new — same MCQ validations (correctAnswers ⊆ options) + set QB + set followUp fields
- `createText(TextQuestionRequest)`: set `questionBanks`; set `marks`
- `createDoc(DocQuestionRequest)`: set `questionBanks`; set `marks`
- `createGroup(GroupQuestionRequest)`:
  - `children` must not be empty (throw `ValidationException` if empty)
  - For each `GroupChildRequest`, create a `GroupQuestionChild` with `displayOrder` = list index
  - Set `questionBanks` on the `GroupQuestion`; children are cascaded automatically
  - Do NOT look up any `TextQuestion` by ID
- `listQuestions(UUID questionBankId, int page, int size)`:
  - `questionBankId` null → all questions (no QB filter)
  - `questionBankId` present → `findByQuestionBanksId(questionBankId, pageable)`
- Remove `listCategories()` method
- `toGroupQuestionResponse()`:
  ```java
  private GroupQuestionResponse toGroupQuestionResponse(GroupQuestion q) {
      List<GroupChildResponse> children = q.getChildren().stream()
          .map(c -> new GroupChildResponse(c.getId(), c.getQuestionText(), c.getKeywords(), c.getMarks()))
          .toList();
      int totalMarks = children.stream().mapToInt(GroupChildResponse::marks).sum();
      return new GroupQuestionResponse(q.getId(), toQbResponses(q.getQuestionBanks()),
          q.getQuestion(), q.isOrdered(), children, totalMarks);
  }
  ```

**Auto-marking update** — when submitting a `McqPlusResponse`:
- Auto-mark MCQ part exactly as `McqResponse` (all-or-nothing for multi-correct)
- Set `correct` flag; set `score` = `correct ? 1 : 0`
- Leave `followUpScore` null (Marker assigns during marking phase)

### Controllers

**`QuestionBankController`** — wired to `QuestionBankService`
```
GET    /api/question-banks          @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
POST   /api/question-banks          @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
PUT    /api/question-banks/{id}     @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
DELETE /api/question-banks/{id}     @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
```

**`QuestionController`** — updated
- `GET /api/questions` — replace `?category=` with `?questionBankId=` (optional UUID)
- Remove `GET /api/questions/categories` handler entirely

### Testing
- `QuestionBankServiceTest`: duplicate name → ConflictException; delete QB when question has only that QB → ValidationException (with message); rename to existing name → ConflictException; rename non-existent id → ValidationException
- `QuestionServiceTest`: any `questionBankId` not found → ValidationException for all question types; `createGroup` with empty children → ValidationException; `createMcqPlus` with correctAnswer not in options → ValidationException
- `McqPlusResponseTest`: MCQ part auto-marked on submission; `followUpScore` null after submission; `correct` and `score` set correctly for both single and multi-correct MCQ options
- `QuestionControllerTest`: `POST MCQ` with unknown QB ID → 400; `POST GROUP` with empty children → 400; `GET /api/question-banks` with CANDIDATE JWT → 403; `DELETE /api/question-banks/{id}` when last QB for a question → 400

### Done When
- All question types persist with QB associations
- QB CRUD works end-to-end (create, list, rename, delete with guard)
- GroupQuestion stores inline children — no TextQuestion lookup
- McqPlusQuestion persists with followUp fields; McqPlusResponse auto-marks MCQ part on submission
- `GET /api/questions?questionBankId=<uuid>` returns questions in that QB
- `GET /api/questions` (no filter) returns all questions (paginated)
- `GET /api/questions/categories` returns 404

---

## Slice C: Angular UI Updates

### Agent Brief
Replace the category text input with QB multi-select (with inline creation). Build the new `GroupChildBuilderComponent` for inline group child creation. Add the MCQ_PLUS question form. Add the QB management screen. Update question list to filter by QB.

### Package Tree Additions / Changes

**Frontend**
```
src/app/features/
  question-management/
    components/
      question-list/
        question-list.component.ts         ← updated: QB filter replaces category filter
        question-list.component.html       ← updated
      question-form/
        question-form.component.ts         ← updated: new types, QB selector, marks fields
        question-form.component.html       ← updated
      group-child-builder/                 ← new component
        group-child-builder.component.ts
        group-child-builder.component.html
      question-bank-selector/             ← new reusable component
        question-bank-selector.component.ts
        question-bank-selector.component.html
  question-bank-management/               ← new feature
    components/
      question-bank-list/
        question-bank-list.component.ts
        question-bank-list.component.html
    question-bank-management.routes.ts
```

### Frontend Services

**`QuestionBankService`** — real implementation
```typescript
getQuestionBanks(): Observable<QuestionBankResponse[]>                        // GET /api/question-banks
createQuestionBank(name: string): Observable<QuestionBankResponse>            // POST /api/question-banks
renameQuestionBank(id: string, name: string): Observable<QuestionBankResponse> // PUT /api/question-banks/{id}
deleteQuestionBank(id: string): Observable<void>                              // DELETE /api/question-banks/{id}
```

**`QuestionService`** — updated signatures
```typescript
createQuestion(request: QuestionRequest): Observable<QuestionResponse>
getQuestions(page: number, size: number, questionBankId?: string): Observable<PageResponse<QuestionResponse>>
// GET /api/questions?page=&size=[&questionBankId=]
// Remove getCategories() entirely
```

### Frontend Components

**`QuestionBankSelectorComponent`** — new reusable component
- `changeDetection: OnPush`; standalone
- Output signal: `selectedIds: OutputEmitterRef<string[]>`
- Loads QBs from `QuestionBankService.getQuestionBanks()` on init
- Shows selected QBs as chips with remove button
- "Add QB" dropdown shows unselected QBs; select one to add
- "New QB" inline input: user types a name, clicks "Create" → calls `QuestionBankService.createQuestionBank()` immediately, adds returned QB to selection
- At least one selection required (invalid state shown if empty)

**`GroupChildBuilderComponent`** — new component
- `changeDetection: OnPush`; standalone
- Output signal: `childrenChange: OutputEmitterRef<GroupChildRequest[]>`
- Manages a dynamic ordered list of child questions
- Each row: `questionText` textarea (required), `keywords` chip input (optional — reuse `KeywordListComponent` pattern), `marks` number input (min 1, required)
- "Add question" button appends a new blank row
- "Remove" button on each row deletes that row (cannot delete last remaining row once submitted — show disabled state)
- At least one complete row required; form invalid until the first child is valid
- Emits current `GroupChildRequest[]` on each change

**`QuestionFormComponent`** — updated
- Replace `category` text input with `QuestionBankSelectorComponent`
- Type selector: MCQ | MCQ_PLUS | TEXT | DOC | GROUP
- **MCQ** form: unchanged except QB selector replaces category
- **MCQ_PLUS** form: same as MCQ (options + `McqOptionBuilderComponent`) plus a follow-up section below:
  - `followUpQuestion` textarea (required, labeled "Follow-up question text")
  - `followUpKeywords` chip input (optional, reuse `KeywordListComponent`)
  - `followUpMarks` number input (min 1, required, labeled "Marks for follow-up")
  - Displayed as a clearly labelled section below the MCQ options
- **TEXT** form: add `marks` number input (min 1, required)
- **DOC** form: add `marks` number input (min 1, required)
- **GROUP** form: replace TextQuestion picker with `GroupChildBuilderComponent`; `ordered` toggle retained

**`QuestionListComponent`** — updated
- QB filter dropdown (populated from `QuestionBankService.getQuestionBanks()`) replaces category dropdown
- Selecting a QB re-fetches with `?questionBankId=<id>`; clearing filter shows all questions

**`QuestionBankListComponent`** — new
- `changeDetection: OnPush`; standalone
- Paginated table: QB name, action buttons (Rename, Delete)
- "New QB" button shows an inline name input + "Create" button; calls `createQuestionBank()` on confirm
- **Rename**: click "Rename" → name becomes an inline text input; press Enter or "Save" to confirm; calls `renameQuestionBank()`
- **Delete**: shows an inline confirmation prompt; calls `deleteQuestionBank()`; on 400 error (last QB guard), shows the server error message to the user

### Route Additions
```typescript
// question-bank-management.routes.ts
export const questionBankManagementRoutes: Routes = [
  { path: '', component: QuestionBankListComponent, canActivate: [RoleGuard], data: { roles: ['MARKER', 'ADMIN'] } }
];

// app.routes.ts — add:
{ path: 'question-banks', loadChildren: () => import('./features/question-bank-management/question-bank-management.routes') }
```

### Testing (Slice C)
- `QuestionBankSelectorComponent`: selecting a QB adds it to selection; removing a chip removes it; "New QB" calls service and adds to selection; invalid when empty
- `GroupChildBuilderComponent`: add row appends blank; remove row deletes that row; emits correct `GroupChildRequest[]`; invalid with zero rows
- `QuestionFormComponent`: type MCQ_PLUS renders follow-up section; follow-up question and marks fields required; type GROUP renders `GroupChildBuilderComponent` instead of TextQuestion picker; TEXT/DOC forms show marks field
- `QuestionListComponent`: QB filter dropdown replaces category; selecting QB re-fetches with correct param
- `QuestionBankListComponent`: list renders QBs; create calls service; delete with 400 error shows message

### Done When
- Marker can create all question types with QBs selected via the new selector (including inline QB creation)
- GROUP form uses inline child builder — no TextQuestion picker visible
- MCQ_PLUS form shows follow-up section with question text, keywords, and marks
- TEXT and DOC forms show marks field
- QB management screen at `/question-banks` supports full CRUD
- Question list filters by QB
- All TypeScript compiles without errors

---

## Phase 3 Integration Note — Assessment Generation

These changes must be applied to `phase-3-assessment-generation.md` when that spec is next updated.

### DTO change

**`AssessmentRequest`** — updated (replaces category-based scoping)
```java
public record AssessmentRequest(
    @NotNull UUID candidateId,
    @NotEmpty List<UUID> questionBankIds,   // replaces any category field; scopes generation pool
    @Min(1) int timeLimitMinutes,
    List<UUID> questionIds                  // optional: null/empty = auto-generate from QBs; populated = manual selection
) {}
```

**`assessment.model.ts`** — updated frontend type
```typescript
export interface AssessmentRequest {
  candidateId: string;
  questionBankIds: string[];    // replaces category
  timeLimitMinutes: number;
  questionIds?: string[];       // null/absent = auto-generate
}
```

### Service change (brief)

`AssessmentService.generate()`:
- Question pool = all questions belonging to ANY of the `questionBankIds`
- No-repeat rule applied on top: exclude questions seen in prior submitted assessments
- `McqPlusQuestion` fills the MCQ composition quota (counts as one MCQ slot)
- Manual override (`questionIds` provided): validate all listed questions belong to at least one of the specified QBs; skip auto-generation

---

## Implementation Order

```
Slice 0  → merge to main first
Slice A  → schema only; runs in parallel with B and C (no code dependencies)
Slice B  → backend logic; depends on Slice A schema being present in DB
Slice C  → Angular UI; depends on Slice 0 types and Slice B endpoints
```

After all slices are merged: update `phase-3-assessment-generation.md` to reflect QB-scoped generation before resuming Phase 3 work (if not yet complete) or Phase 6.
