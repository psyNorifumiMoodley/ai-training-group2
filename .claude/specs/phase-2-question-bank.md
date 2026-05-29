# Phase 2 — Question Management

> **Epic:** Phase 2 — Question Management
> **Delivery:** Slice 0 merges first; remaining three slices run in parallel (one dev each).
> **Dependency:** Phase 0 fully merged. Phase 1 not required.
> **Note:** "Question Bank" is a conceptual term only — there is no `QuestionBank` entity in the domain. Questions are categorised via a `category` string field on `AssessmentQuestion`.

---

## Slice 0: Backend API Contracts + AssessmentQuestion Base *(merge first)*

### Agent Brief
Define all Phase 2 DTOs, stub controllers, and — critically — the `AssessmentQuestion` abstract JPA entity. All concrete question-type slices depend on it compiling first. Also create the Angular `QuestionService` stub so the UI slice can start immediately.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    AssessmentQuestion.java       ← abstract entity, TABLE_PER_CLASS
    QuestionType.java             ← enum: MCQ, TEXT, DOC, GROUP
  dto/
    McqQuestionRequest.java       ← record { String category, String question, List<String> options, List<String> correctAnswers }
    McqQuestionResponse.java      ← record { UUID id, QuestionType type, String category, String question, List<String> options, List<String> correctAnswers }
    TextQuestionRequest.java      ← record { String category, String question, List<String> keywords }
    TextQuestionResponse.java     ← record { UUID id, QuestionType type, String category, String question, List<String> keywords }
    DocQuestionRequest.java       ← record { String category, String question }
    DocQuestionResponse.java      ← record { UUID id, QuestionType type, String category, String question }
    GroupQuestionRequest.java     ← record { String category, String question, boolean ordered, List<UUID> followUpQuestionIds }
    GroupQuestionResponse.java    ← record { UUID id, QuestionType type, String category, String question, boolean ordered, List<TextQuestionResponse> followUpQuestions }
    QuestionResponse.java         ← sealed interface; permits McqQuestionResponse, TextQuestionResponse, DocQuestionResponse, GroupQuestionResponse
  controller/
    QuestionController.java       ← stub
```

**Frontend**
```
src/app/
  core/
    services/
      question.service.ts         ← stub
    models/
      question.model.ts
```

### Entities

**`AssessmentQuestion`** — abstract base
- Table strategy: `@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)`
- Annotations: `@Entity`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@Column(nullable = false) String category`
  - `@Column(nullable = false, columnDefinition = "TEXT") String question`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false) QuestionType type`

### Liquibase Changesets
None — all question tables exist from Phase 0 baseline.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`QuestionController`** — stubs
```
POST /api/questions              → 201 (hardcoded)
GET  /api/questions              → 200 PageResponse (empty)
GET  /api/questions/categories   → 200 List<String> (empty)
```
All endpoints secured with `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")`.

### Frontend Type Definitions
```typescript
// core/models/question.model.ts
export type QuestionType = 'MCQ' | 'TEXT' | 'DOC' | 'GROUP';

export interface McqQuestionRequest  { category: string; question: string; options: string[]; correctAnswers: string[]; }
export interface TextQuestionRequest { category: string; question: string; keywords: string[]; }
export interface DocQuestionRequest  { category: string; question: string; }
export interface GroupQuestionRequest { category: string; question: string; ordered: boolean; followUpQuestionIds: string[]; }

export interface BaseQuestionResponse { id: string; type: QuestionType; category: string; question: string; }
export interface McqQuestionResponse  extends BaseQuestionResponse { options: string[]; correctAnswers: string[]; }
export interface TextQuestionResponse extends BaseQuestionResponse { keywords: string[]; }
export interface DocQuestionResponse  extends BaseQuestionResponse {}
export interface GroupQuestionResponse extends BaseQuestionResponse { ordered: boolean; followUpQuestions: TextQuestionResponse[]; }
export type QuestionResponse = McqQuestionResponse | TextQuestionResponse | DocQuestionResponse | GroupQuestionResponse;
```

### Frontend Services

**`QuestionService`** — stubs returning `EMPTY`
```typescript
createQuestion(request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest): Observable<QuestionResponse>
getQuestions(page: number, size: number, category?: string): Observable<PageResponse<QuestionResponse>>
getCategories(): Observable<string[]>
```

### Frontend Components
None.

### Route Additions
None.

### Testing
- Stubs compile and return correct HTTP status codes
- `AssessmentQuestion` entity: `@Inheritance(TABLE_PER_CLASS)` present; abstract class cannot be instantiated directly

### Done When
- `AssessmentQuestion` abstract entity compiles with `TABLE_PER_CLASS` inheritance
- Stub controllers return hardcoded 201/200 with MARKER role JWT
- Non-MARKER/ADMIN callers receive 403
- Angular `QuestionService` compiles with correct type signatures

---

## Slice: MCQuestion + DocQuestion

### Agent Brief
Implement `McqQuestion` and `DocQuestion` concrete entities extending `AssessmentQuestion`. Wire question creation and listing for both types through `QuestionService` and `QuestionController`. MCQ validation: at least one option required; at least one correct answer required; correct answers must be a strict subset of provided options.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    McqQuestion.java
    DocQuestion.java
  repository/
    AssessmentQuestionRepository.java
  service/
    QuestionService.java
```

### Entities

**`McqQuestion extends AssessmentQuestion`**
- Table: `mcq_question` (TABLE_PER_CLASS — owns all base columns)
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false) List<String> options`
  - `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false) List<String> correctAnswers`

**`DocQuestion extends AssessmentQuestion`**
- Table: `doc_question` (TABLE_PER_CLASS — owns all base columns)
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- No additional fields

### Liquibase Changesets
None.

### Repositories

**`AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID>`**
```java
Page<AssessmentQuestion> findByCategoryIgnoreCase(String category, Pageable pageable);

// Polymorphic query across all concrete tables via TABLE_PER_CLASS union:
@Query("SELECT DISTINCT q.category FROM AssessmentQuestion q ORDER BY q.category ASC")
List<String> findDistinctCategories();
```

### Services

**`QuestionService`** (initial — MCQ and DOC types only)
```java
QuestionResponse addQuestion(Object request);
// Dispatches on request type; validates per-type rules
// MCQ: options non-empty; correctAnswers non-empty and a subset of options
// DOC: question non-empty

McqQuestion createMcq(McqQuestionRequest request);
DocQuestion createDoc(DocQuestionRequest request);

PageResponse<QuestionResponse> listQuestions(String category, int page, int size);
// category null → all questions; category provided → filter by category (case-insensitive)

List<String> listCategories();
```

### Controllers
`QuestionController` wired to `QuestionService` for MCQ and DOC types.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation deferred to Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `McqQuestionTest` (unit): empty options → `ValidationException`; empty correctAnswers → `ValidationException`; correctAnswer not in options → `ValidationException`
- `QuestionServiceTest` (unit): MCQ and DOC creation → correct entity type persisted
- `QuestionControllerTest` (`@WebMvcTest`): POST MCQ → 201; POST DOC → 201; zero correctAnswers → 400; CANDIDATE JWT → 403

### Done When
- `POST /api/questions` with `type=MCQ` persists `mcq_question` row
- `POST /api/questions` with `type=DOC` persists `doc_question` row
- Zero correctAnswers returns 400
- `GET /api/questions?category=Java` returns only questions in that category
- `GET /api/questions/categories` returns sorted, distinct category strings

---

## Slice: TextQuestion + GroupQuestion

### Agent Brief
Implement `TextQuestion` and `GroupQuestion`. `TextQuestion` carries a `keywords` list as a guide for markers when evaluating open-ended responses. `GroupQuestion` extends `TextQuestion` (and thus `AssessmentQuestion`) but does not use the inherited `keywords` field — instead it holds an ordered or unordered collection of `TextQuestion` follow-up questions, controlled by a boolean `ordered` flag set at creation time.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    TextQuestion.java
    GroupQuestion.java
```

### Entities

**`TextQuestion extends AssessmentQuestion`**
- Table: `text_question` (TABLE_PER_CLASS — owns all base columns)
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") List<String> keywords` — optional; may be empty

**`GroupQuestion extends TextQuestion`**
- Table: `group_question` (TABLE_PER_CLASS — inherits all `TextQuestion` columns; `keywords` column is always null for group rows)
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@Column(nullable = false) boolean ordered` — if `true`, the order of `followUpQuestions` is preserved and must be respected at assessment generation time
  - `@ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "group_question_follow_up", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "question_id")) @OrderColumn(name = "display_order") List<TextQuestion> followUpQuestions`
  - `@ToString.Exclude` on `followUpQuestions`

> The `@OrderColumn` always persists `display_order` in the join table. The `ordered` flag is business metadata that determines whether callers and assessment generators respect that order.

### Liquibase Changesets

**`2026-05-29-001-group-question-follow-up-join-table`**
```sql
CREATE TABLE group_question_follow_up (
    group_id      UUID    NOT NULL REFERENCES group_question(id),
    question_id   UUID    NOT NULL REFERENCES text_question(id),
    display_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, question_id)
);
```
Rollback: `DROP TABLE group_question_follow_up;`

### Repositories
Uses `AssessmentQuestionRepository` (created in MCQ/Doc slice).

Add to `AssessmentQuestionRepository`:
```java
@Query("SELECT q FROM TextQuestion q WHERE q.id IN :ids")
List<TextQuestion> findTextQuestionsByIds(@Param("ids") List<UUID> ids);
// Used by GroupQuestion creation to validate followUpQuestionIds
```

### Services

Extend `QuestionService`:
```java
TextQuestion createText(TextQuestionRequest request);

GroupQuestion createGroup(GroupQuestionRequest request);
// Fetches TextQuestions for all followUpQuestionIds
// Throws ValidationException if any ID does not resolve to a TextQuestion
// Preserves insertion order when saving followUpQuestions list

PageResponse<QuestionResponse> listQuestions(...);  // already implemented — no change needed
```

### Controllers
`QuestionController` extended to handle `TEXT` and `GROUP` types.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation deferred to Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `TextQuestionTest` (unit): creates text question with keywords; creates with empty keywords list
- `GroupQuestionTest` (unit): unknown `followUpQuestionId` → `ValidationException`; ID that resolves to non-TextQuestion type → `ValidationException`
- `QuestionControllerTest` additions: POST TEXT → 201; POST GROUP with valid `followUpQuestionIds` → 201; invalid ID → 400
- Integration test: `group_question` row has null `keywords` column

### Done When
- `POST /api/questions` with `type=TEXT` persists `text_question` row with keywords JSONB
- `POST /api/questions` with `type=GROUP` with valid `followUpQuestionIds` persists `group_question` row and `group_question_follow_up` rows with `display_order`
- Invalid or non-TextQuestion `followUpQuestionId` returns 400

---

## Slice: Angular Question UI

### Agent Brief
Build the Angular screens for Markers to manage questions. Replace all `QuestionService` stubs with real HTTP calls. The question creation form is dynamic — the selected type determines which fields render. Questions are browsable by category via a filter dropdown.

### Package Tree Additions

**Frontend**
```
src/app/features/question-management/
  components/
    question-list/
      question-list.component.ts
      question-list.component.html
    question-form/
      question-form.component.ts
      question-form.component.html
    mcq-option-builder/
      mcq-option-builder.component.ts
      mcq-option-builder.component.html
    keyword-list/
      keyword-list.component.ts
      keyword-list.component.html
  question-management.routes.ts
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
None (backend).

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services

**`QuestionService`** — real implementation:
```typescript
createQuestion(request: McqQuestionRequest | TextQuestionRequest | DocQuestionRequest | GroupQuestionRequest): Observable<QuestionResponse>
// POST /api/questions

getQuestions(page: number, size: number, category?: string): Observable<PageResponse<QuestionResponse>>
// GET /api/questions?page=&size=[&category=]

getCategories(): Observable<string[]>
// GET /api/questions/categories
```

### Frontend Components

**`QuestionListComponent`**
- `changeDetection: OnPush`
- Shows paginated question list (type badge, category, question text per row)
- Category filter dropdown populated from `getCategories()`; selecting a category re-fetches with `?category=X`
- "Add Question" button opens `QuestionFormComponent`

**`QuestionFormComponent`**
- Type selector: MCQ | TEXT | DOC | GROUP
- Reactive form — fields rendered conditionally based on `type` signal:
  - **MCQ**: category + question + `McqOptionBuilderComponent`
  - **TEXT**: category + question + `KeywordListComponent` (optional)
  - **DOC**: category + question only
  - **GROUP**: category + question + `ordered` toggle + multi-select picker for existing TextQuestions (loaded from `getQuestions()` filtered to `type=TEXT`)
- Emits `questionAdded` output signal on success

**`McqOptionBuilderComponent`**
- Manages a dynamic list of option strings
- Toggle per option: "mark as correct answer" (supports multi-correct)
- Validates: at least one option, at least one marked correct; submit disabled until valid

**`KeywordListComponent`**
- Manages a dynamic list of keyword strings (chip/tag UI)
- Optional — markers can submit with an empty keyword list

### Route Additions
```typescript
// question-management.routes.ts
export const questionManagementRoutes: Routes = [
  { path: '', component: QuestionListComponent, canActivate: [RoleGuard], data: { role: 'MARKER' } }
];

// app.routes.ts
{ path: 'questions', loadChildren: () => import('./features/question-management/question-management.routes') }
```

### Testing
- `QuestionService` unit test: each method maps to the correct HTTP call; `category` query param included when provided
- `QuestionFormComponent` unit test: type change re-renders correct fields; MCQ with no correct answer disables submit; valid MCQ form calls service
- `McqOptionBuilderComponent` unit test: adds options; toggles correct answer; disables submit when none correct; emits correct value
- `KeywordListComponent` unit test: adds and removes keywords; emits current list

### Done When
- Marker navigates to `/questions` and sees a paginated question list
- Marker filters questions by category using the dropdown
- Marker creates MCQ, TEXT, DOC, and GROUP questions via the type-specific form
- MCQ form prevents submission when no correct answer is selected
- GROUP form allows selecting existing TextQuestions as follow-up questions, with an ordered toggle
- All API calls succeed end-to-end against the real backend
