# Phase 2 — Question Bank

> **Epic:** Phase 2 — Question Bank
> **Delivery:** Slice 0 (including `AssessmentQuestion` abstract entity) merges first; all other slices run in parallel.
> **Dependency:** Phase 0 fully merged. Phase 1 not required.

---

## Slice 0: Backend API Contracts + AssessmentQuestion Base *(merge first)*

### Agent Brief
Define all Phase 2 DTOs, stub controllers, and — critically — the `AssessmentQuestion` abstract JPA entity. All concrete question-type slices (MCQ/Text, Doc/Group) extend `AssessmentQuestion`, so it must exist and compile before those slices branch. Also create the Angular `QuestionBankService` stub so the UI slice can start immediately.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    AssessmentQuestion.java       ← abstract entity, TABLE_PER_CLASS
    QuestionType.java             ← enum: MCQ, TEXT, DOC, GROUP
  dto/
    QuestionBankRequest.java      ← record { String name }
    QuestionBankResponse.java     ← record { UUID id, String name }
    McqQuestionRequest.java       ← record { String body, List<String> options, List<String> correctAnswers }
    McqQuestionResponse.java      ← record { UUID id, QuestionType type, String body, List<String> options }
    TextQuestionRequest.java      ← record { String body }
    TextQuestionResponse.java     ← record { UUID id, QuestionType type, String body }
    DocQuestionRequest.java       ← record { String body }
    DocQuestionResponse.java      ← record { UUID id, QuestionType type, String body }
    GroupQuestionRequest.java     ← record { String body, List<UUID> childQuestionIds }
    GroupQuestionResponse.java    ← record { UUID id, QuestionType type, String body, List<UUID> childIds }
    QuestionResponse.java         ← sealed interface / union response type
  controller/
    QuestionBankController.java   ← stub
    QuestionController.java       ← stub
```

**Frontend**
```
src/app/
  core/
    services/
      question-bank.service.ts    ← stub
    models/
      question-bank.model.ts
```

### Entities

**`AssessmentQuestion`** — abstract base
- Table strategy: `@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)`
- Annotations: `@Entity`, `@Table(name = "assessment_question")`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_bank_id") QuestionBank questionBank`
  - `@Column(nullable = false, columnDefinition = "TEXT") String body`
  - `@Enumerated(EnumType.STRING) QuestionType type`

### Liquibase Changesets
None — all question tables exist from Phase 0 baseline.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`QuestionBankController`** — stubs
```
POST /api/question-banks       → 201 QuestionBankResponse (hardcoded)
GET  /api/question-banks       → 200 PageResponse<QuestionBankResponse> (empty)
```

**`QuestionController`** — stubs
```
POST /api/question-banks/{bankId}/questions  → 201 (hardcoded)
GET  /api/question-banks/{bankId}/questions  → 200 PageResponse (empty)
```
Both secured with `@PreAuthorize("hasRole('MARKER')")`.

### Frontend Type Definitions
```typescript
// core/models/question-bank.model.ts
export type QuestionType = 'MCQ' | 'TEXT' | 'DOC' | 'GROUP';

export interface QuestionBankRequest { name: string; }
export interface QuestionBankResponse { id: string; name: string; }

export interface McqQuestionRequest { body: string; options: string[]; correctAnswers: string[]; }
export interface TextQuestionRequest { body: string; }
export interface DocQuestionRequest  { body: string; }
export interface GroupQuestionRequest { body: string; childQuestionIds: string[]; }

export interface BaseQuestionResponse { id: string; type: QuestionType; body: string; }
export interface McqQuestionResponse extends BaseQuestionResponse { options: string[]; }
export interface GroupQuestionResponse extends BaseQuestionResponse { childIds: string[]; }
export type QuestionResponse = McqQuestionResponse | BaseQuestionResponse | GroupQuestionResponse;
```

### Frontend Services

**`QuestionBankService`** — stubs returning `EMPTY`
```typescript
createBank(request: QuestionBankRequest): Observable<QuestionBankResponse>
getBanks(page: number, size: number): Observable<PageResponse<QuestionBankResponse>>
addQuestion(bankId: string, request: unknown): Observable<QuestionResponse>
getQuestions(bankId: string, page: number, size: number): Observable<PageResponse<QuestionResponse>>
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
- Non-MARKER callers receive 403
- Angular `QuestionBankService` compiles with correct type signatures

---

## Slice: Question Bank CRUD

### Agent Brief
Implement full persistence for `QuestionBank`. A question bank is a named container owned (implicitly) by markers. Implement create and paginated list. `QuestionBankController` is already stubbed — replace its service calls with the real implementation.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    QuestionBank.java
  repository/
    QuestionBankRepository.java
  service/
    QuestionBankService.java
```

### Entities

**`QuestionBank`**
- Table: `question_bank`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@Column(nullable = false) String name`
  - `@OneToMany(mappedBy = "questionBank", fetch = FetchType.LAZY) List<AssessmentQuestion> questions`
  - `@ToString.Exclude` on `questions`

### Liquibase Changesets
None.

### Repositories

**`QuestionBankRepository extends JpaRepository<QuestionBank, UUID>`**
```java
Page<QuestionBank> findAll(Pageable pageable);
boolean existsById(UUID id);
```

### Services

**`QuestionBankService`**
```java
QuestionBankResponse create(QuestionBankRequest request);
// Saves new QuestionBank; returns response

PageResponse<QuestionBankResponse> list(int page, int size);

QuestionBank getOrThrow(UUID id);
// Used internally by QuestionService; throws NotFoundException if not found
```

### Controllers
`QuestionBankController` wired to `QuestionBankService` (replace stubs).

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `QuestionBankRepositoryTest` (`@DataJpaTest`): save and retrieve; `findAll` pagination
- `QuestionBankServiceTest` (unit): create → persists; `getOrThrow` → throws `NotFoundException` for unknown UUID
- `QuestionBankControllerTest` (`@WebMvcTest`): POST → 201; GET paginated → 200; CANDIDATE JWT → 403

### Done When
- `POST /api/question-banks` creates and returns a question bank
- `GET /api/question-banks?page=0&size=20` returns paginated list

---

## Slice: MCQ and Text Questions

### Agent Brief
Implement `McqQuestion` and `TextQuestion` concrete entities extending `AssessmentQuestion`. Wire question creation for these two types through `QuestionService` and `QuestionController`. MCQ validation rule: at least one correct answer required; correct answers must be a subset of provided options.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    McqQuestion.java
    TextQuestion.java
  repository/
    AssessmentQuestionRepository.java
  service/
    QuestionService.java
```

### Entities

**`McqQuestion extends AssessmentQuestion`**
- Table: `mcq_question` (TABLE_PER_CLASS)
- Fields:
  - `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") List<String> options`
  - `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") List<String> correctAnswers`

**`TextQuestion extends AssessmentQuestion`**
- Table: `text_question`
- No additional fields

### Liquibase Changesets
None.

### Repositories

**`AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID>`**
```java
Page<AssessmentQuestion> findByQuestionBankId(UUID bankId, Pageable pageable);
List<AssessmentQuestion> findAllByIdIn(List<UUID> ids);
```

### Services

**`QuestionService`**
```java
QuestionResponse addQuestion(UUID bankId, Object request);
// Dispatches based on request type; validates bank exists via QuestionBankService.getOrThrow()
// MCQ: validates correctAnswers non-empty and subset of options
// TEXT: validates body non-empty

McqQuestion createMcq(UUID bankId, McqQuestionRequest request);
TextQuestion createText(UUID bankId, TextQuestionRequest request);

PageResponse<QuestionResponse> listQuestions(UUID bankId, int page, int size);
```

### Controllers
`QuestionController` wired to `QuestionService` for MCQ and TEXT types.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `McqQuestionTest` (unit): zero correct answers → `ValidationException`; correct answer not in options → `ValidationException`
- `QuestionServiceTest` (unit): MCQ and text creation → correct entity type persisted
- `QuestionControllerTest` (`@WebMvcTest`): POST MCQ → 201; POST TEXT → 201; zero correctAnswers → 400

### Done When
- `POST /api/question-banks/{bankId}/questions` with `type=MCQ` persists `mcq_question` row
- `POST /api/question-banks/{bankId}/questions` with `type=TEXT` persists `text_question` row
- Zero correctAnswers returns 400

---

## Slice: Doc and Group Questions

### Agent Brief
Implement `DocQuestion` and `QuestionGroup` concrete entities. Doc questions have no special fields beyond the base entity. Question groups reference child question IDs — all child IDs must resolve to existing questions in the same bank.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    DocQuestion.java
    QuestionGroup.java
```

### Entities

**`DocQuestion extends AssessmentQuestion`**
- Table: `doc_question`
- No additional fields

**`QuestionGroup extends AssessmentQuestion`**
- Table: `question_group`
- Fields:
  - `@ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "question_group_member", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "question_id")) List<AssessmentQuestion> children`
  - `@ToString.Exclude` on `children`

### Liquibase Changesets
None.

### Repositories
Uses `AssessmentQuestionRepository` (already created in MCQ/Text slice).

### Services

Extend `QuestionService`:
```java
DocQuestion createDoc(UUID bankId, DocQuestionRequest request);

QuestionGroup createGroup(UUID bankId, GroupQuestionRequest request);
// Validates all childQuestionIds exist in the same bankId
// Throws ValidationException if any child ID not found in bank
```

### Controllers
`QuestionController` extended to handle `DOC` and `GROUP` types.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `DocQuestionTest` (unit): creates doc question with body
- `QuestionGroupTest` (unit): unknown child question ID → `ValidationException`; child from different bank → `ValidationException`
- `QuestionControllerTest` additions: POST DOC → 201; POST GROUP with valid children → 201; invalid child ID → 400

### Done When
- `POST .../questions` with `type=DOC` persists `doc_question` row
- `POST .../questions` with `type=GROUP` and valid children persists group + `question_group_member` rows
- Invalid child ID returns 400

---

## Slice: Angular Question Bank UI

### Agent Brief
Build the Angular screens for Markers to manage question banks and add questions. Replace all `QuestionBankService` stubs with real HTTP calls. The question creation form is dynamic — the selected question type determines which fields render.

### Package Tree Additions

**Frontend**
```
src/app/features/question-bank/
  components/
    question-bank-list/
      question-bank-list.component.ts
      question-bank-list.component.html
    question-bank-detail/
      question-bank-detail.component.ts
      question-bank-detail.component.html
    question-form/
      question-form.component.ts
      question-form.component.html
    mcq-option-builder/
      mcq-option-builder.component.ts
      mcq-option-builder.component.html
  question-bank.routes.ts
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

**`QuestionBankService`** — real implementation:
```typescript
createBank(request: QuestionBankRequest): Observable<QuestionBankResponse>
// POST /api/question-banks

getBanks(page: number, size: number): Observable<PageResponse<QuestionBankResponse>>
// GET /api/question-banks?page=&size=

addQuestion(bankId: string, type: QuestionType, request: unknown): Observable<QuestionResponse>
// POST /api/question-banks/{bankId}/questions

getQuestions(bankId: string, page: number, size: number): Observable<PageResponse<QuestionResponse>>
// GET /api/question-banks/{bankId}/questions?page=&size=
```

### Frontend Components

**`QuestionBankListComponent`**
- `changeDetection: OnPush`
- Lists all question banks with name and question count
- "Create Question Bank" button → inline form or modal
- Navigates to `QuestionBankDetailComponent` on row click

**`QuestionBankDetailComponent`**
- Receives `bankId` as route param (`input()` signal)
- Shows bank name, paginated question list
- "Add Question" button opens `QuestionFormComponent`

**`QuestionFormComponent`**
- Type selector: MCQ | TEXT | DOC | GROUP
- Reactive form — fields rendered conditionally based on `type` signal:
  - MCQ: body + dynamic option list + correct-answer toggle
  - TEXT / DOC: body only
  - GROUP: body + child question picker (select from existing bank questions)
- Emits `questionAdded` output signal on success

**`McqOptionBuilderComponent`**
- Manages a dynamic list of option strings
- Toggle per option: "mark as correct answer" (supports multi-correct)
- Validates: at least one option, at least one marked correct

### Route Additions
```typescript
// question-bank.routes.ts
export const questionBankRoutes: Routes = [
  { path: '', component: QuestionBankListComponent, canActivate: [RoleGuard], data: { role: 'MARKER' } },
  { path: ':bankId', component: QuestionBankDetailComponent, canActivate: [RoleGuard], data: { role: 'MARKER' } }
];

// app.routes.ts
{ path: 'question-banks', loadChildren: () => import('./features/question-bank/question-bank.routes') }
```

### Testing
- `QuestionBankService` unit test: each method maps to the correct HTTP call
- `QuestionFormComponent` unit test: type change re-renders correct fields; MCQ with no correct answer disables submit; valid MCQ form calls service
- `McqOptionBuilderComponent` unit test: adds options; toggles correct answer; emits correct value

### Done When
- Marker navigates to `/question-banks`, sees list, creates a new bank
- Marker clicks into a bank, sees questions, adds MCQ/text/doc/group questions via form
- MCQ form prevents submission with no correct answer selected
- All API calls succeed end-to-end against the real backend
