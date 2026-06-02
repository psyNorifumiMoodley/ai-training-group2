# Phase 3 — Assessment Generation & Distribution

> **Epic:** Phase 3 — Assessment Generation
> **Delivery:** Slice 0 merges first; all other slices run in parallel.
> **Dependency:** Phase 0 + Phase 2 fully merged (questions must exist to generate assessments).

---

## Slice 0: Backend API Contracts *(merge first)*

### Agent Brief
Define all DTOs and stub controllers for Phase 3. The assessment generation endpoint is the critical path — stub it so the invitation token slice and the Angular UI slice can develop against it simultaneously.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  dto/
    AssessmentRequest.java       ← record { UUID candidateId, List<UUID> questionIds, int timeLimitMinutes }
    AssessmentResponse.java      ← record { UUID id, UUID candidateId, String status, String invitationLink, int timeLimitMinutes }
  controller/
    AssessmentController.java    ← stub (Phase 3 endpoints only)
```

**Frontend**
```
src/app/
  core/
    services/
      assessment.service.ts      ← stub
    models/
      assessment.model.ts
```

### Entities
None in this slice.

### Liquibase Changesets
None — `assessment` table exists from Phase 0 baseline.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`AssessmentController`** — stub
```
POST /api/assessments
  @PreAuthorize("hasRole('MARKER')")
  → 201 AssessmentResponse (hardcoded)
```

### Frontend Type Definitions
```typescript
// core/models/assessment.model.ts
export type AssessmentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'MARKED';

export interface AssessmentRequest {
  candidateId: string;
  questionIds: string[];
  timeLimitMinutes: number;
}
export interface AssessmentResponse {
  id: string;
  candidateId: string;
  status: AssessmentStatus;
  invitationLink: string;
  timeLimitMinutes: number;
  createdAt: string;
}
```

### Frontend Services

**`AssessmentService`** — stub
```typescript
generateAssessment(request: AssessmentRequest): Observable<AssessmentResponse>
```

### Frontend Components
None.

### Route Additions
None.

### Testing
- Stub returns 201; CANDIDATE JWT returns 403

### Done When
- `POST /api/assessments` returns hardcoded 201 with MARKER JWT
- Angular `AssessmentService` compiles with correct types

---

## Slice: Assessment Generation Logic

### Agent Brief
Implement `Assessment` entity and the core generation logic in `AssessmentService`. Three business rules are enforced: (1) no-repeat questions scoped to the current calendar year, (2) configurable doc question limit read from `assessment.doc-question-limit`, (3) a fixed question composition that every assessment must satisfy. The endpoint wires to this service and returns the saved assessment.

**Question Composition** — every assessment, whether marker-picked or randomly generated, must contain exactly:
- 5 MCQ questions
- 3 Text questions (pure `TextQuestion`, not `GroupQuestion`)
- 1 Document question
- 1 Group question

These counts are controlled by four `application.properties` keys (`assessment.required-mcq`, `assessment.required-text`, `assessment.required-doc`, `assessment.required-group`) so they are easily changed without touching service code.

**Random vs. Marker-Picked** — `AssessmentRequest.questionIds` drives the mode:
- Empty list → system randomly selects questions from all available questions satisfying the composition rules, excluding any seen by the candidate this calendar year.
- Non-empty list → marker has hand-picked the questions; the system validates the provided list satisfies the composition rules, then filters out any the candidate has already seen this year.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    Assessment.java
    AssessmentStatus.java         ← enum: PENDING, IN_PROGRESS, SUBMITTED, MARKED
    UnprocessableException.java   ← maps to HTTP 422
  repository/
    AssessmentRepository.java
  service/
    AssessmentService.java
```

### Entities

**`Assessment`**
- Table: `assessment`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "candidate_id") Candidate candidate`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false) AssessmentStatus status` — default `PENDING`
  - `@Column(columnDefinition = "TEXT") String invitationToken`
  - `@Column(nullable = false) int timeLimitMinutes`
  - `Instant startTime`
  - `boolean autoSubmitted`
  - `@ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "assessment_question_link", joinColumns = @JoinColumn(name = "assessment_id"), inverseJoinColumns = @JoinColumn(name = "question_id")) List<AssessmentQuestion> questions`
  - `@ToString.Exclude` on `questions`

### Liquibase Changesets

**`2026-06-01-003-fix-assessment-question-link-fk`**
The baseline schema added a FK from `assessment_question_link.question_id` to `assessment_question(id)`. Because the question hierarchy uses `TABLE_PER_CLASS` inheritance, question IDs live only in the concrete tables (`mcq_question`, `text_question`, `doc_question`, `group_question`) — not in the abstract `assessment_question` table. This FK must be dropped so Hibernate can insert link rows that reference concrete-table IDs.

```xml
<dropForeignKeyConstraint baseTableName="assessment_question_link"
                          constraintName="fk_aql_question"/>
```

Include a rollback that restores the FK.

### Repositories

**`AssessmentRepository extends JpaRepository<Assessment, UUID>`**
```java
// For no-repeat rule: find question IDs seen by candidate in submitted assessments within a date range
@Query("""
  SELECT DISTINCT aq.id FROM Assessment a
  JOIN a.questions aq
  WHERE a.candidate.id = :candidateId
    AND a.status = :status
    AND a.createdAt >= :yearStart
    AND a.createdAt < :yearEnd
""")
List<UUID> findSeenQuestionIdsByCandidateAndYear(
    @Param("candidateId") UUID candidateId,
    @Param("status") AssessmentStatus status,
    @Param("yearStart") Instant yearStart,
    @Param("yearEnd") Instant yearEnd
);

List<Assessment> findByCandidateId(UUID candidateId);
Page<Assessment> findByStatus(AssessmentStatus status, Pageable pageable);
```

### Services

**`AssessmentService`**
```java
// Configurable composition — change values in application.properties
@Value("${assessment.required-mcq}") int requiredMcq;
@Value("${assessment.required-text}") int requiredText;
@Value("${assessment.required-doc}") int requiredDoc;
@Value("${assessment.required-group}") int requiredGroup;
@Value("${assessment.doc-question-limit}") int docQuestionLimit;

AssessmentResponse generate(AssessmentRequest request);
// Random mode (empty questionIds):
//   1. Resolve candidate (throws NotFoundException if not found)
//   2. Get seen question IDs for current calendar year
//   3. Randomly pick required counts of each type from available (not seen) questions
//   4. If any type has insufficient available questions → throw UnprocessableException (422)
//   5. Count DOC questions; if > docQuestionLimit → throw ValidationException (400)
//   6. Persist Assessment(status=PENDING, questions=selected, timeLimitMinutes)
//   7. Return AssessmentResponse
//
// Marker-picked mode (non-empty questionIds):
//   1. Resolve candidate (throws NotFoundException if not found)
//   2. Resolve question entities (throws NotFoundException for any missing ID)
//   3. Validate composition exactly matches required counts → throw ValidationException (400) if not
//   4. Filter out questions seen this calendar year (no-repeat rule)
//   5. If all questions filtered → throw UnprocessableException (422)
//   6. Count DOC questions; if > docQuestionLimit → throw ValidationException (400)
//   7. Persist Assessment(status=PENDING, questions=filtered, timeLimitMinutes)
//   8. Return AssessmentResponse

Assessment getOrThrow(UUID id);
```

### Controllers
`AssessmentController` POST endpoint wired to `AssessmentService.generate()`.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Already defined in Slice 0.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `AssessmentServiceTest` (unit, mock repositories):
  - Random mode: seen question X this year → X not included in selection
  - Random mode: seen question X last year → X available for selection
  - Random mode: all questions of a type seen → throws `UnprocessableException`
  - Marker-picked mode: provided list matches composition → success
  - Marker-picked mode: wrong composition (e.g., 6 MCQ) → throws `ValidationException`
  - Marker-picked mode: all provided questions seen this year → throws `UnprocessableException`
  - DOC questions > `docQuestionLimit` → throws `ValidationException`
  - `docQuestionLimit` read from `@Value` not hardcoded (test with limit=0 to confirm configurable)
  - Composition counts read from `@Value` not hardcoded (verify with alternate values)
- `AssessmentRepositoryTest` (`@DataJpaTest`): `findSeenQuestionIdsByCandidateAndYear` returns correct question IDs; excludes PENDING assessments; excludes prior-year assessments; excludes other candidates

### Done When
- `POST /api/assessments` with valid body creates `assessment` row with status `PENDING`
- Empty `questionIds` → questions randomly selected following composition rules
- Non-empty `questionIds` → questions validated against composition rules, then filtered for seen
- Previously seen question (same year) excluded silently (random) or filtered (marker-picked)
- Wrong composition returns 400; all questions excluded returns 422
- Too many DOC questions returns 400
- `assessment.doc-question-limit=1` and composition properties in `application.properties` control the limits

---

## Slice: Invitation Token & Email

### Agent Brief
Generate a signed JWT invitation token after assessment creation and send an invitation email. The invitation token encodes only the `assessmentId` — it has no `exp` claim at creation time. Expiry is enforced at access time by checking `start_time + time_limit_minutes`. Email failure must never roll back the assessment transaction.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  config/
    InvitationTokenUtil.java      ← separate from JwtUtil (different secret/purpose)
  service/
    EmailService.java             ← interface
    EmailServiceImpl.java         ← implementation (SMTP or stub)
```

### Entities
None — `invitation_token` column already on `Assessment`.

### Liquibase Changesets
None.

### Repositories
None.

### Services

**`InvitationTokenUtil`**
```java
String generateToken(UUID assessmentId);
// Signs JWT with assessmentId claim; NO exp claim set

UUID extractAssessmentId(String token);
boolean isSignatureValid(String token);
```

**`EmailService`** (interface)
```java
void sendInvitation(String toEmail, String candidateName, String invitationLink);
void sendFeedback(String toEmail, String candidateName, Map<UUID, String> feedbackByQuestion);
```

**`EmailServiceImpl`**
- Implements `EmailService` via Spring Mail (`JavaMailSender`)
- Both methods are `@Async` — failure is caught and logged, never propagated

**`AssessmentService` update**
```java
// After persisting Assessment:
String token = invitationTokenUtil.generateToken(savedAssessment.getId());
assessment.setInvitationToken(token);
// Build invitation link: environment.frontendBaseUrl + "/assessment/" + token
emailService.sendInvitation(candidate.getUser().getEmail(), candidate.getUser().getName(), link);
```

### Controllers
None new.

### Frontend Type Definitions
None.

### Frontend Services
None.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `InvitationTokenUtilTest`: generate → extract assessmentId → assert match; tampered token fails signature check; no `exp` claim in generated token
- `EmailServiceTest` (mock `JavaMailSender`): invitation email sent with correct recipient and link; exception during send does NOT propagate
- `AssessmentServiceIntegrationTest`: email failure (mock throws) → assessment still persisted with token set

### Done When
- After `POST /api/assessments`, the `assessment` row has a non-null `invitation_token`
- Token can be decoded to extract the `assessmentId`
- Invitation email is sent to the candidate's email address
- Email delivery failure returns 201 (assessment saved) and logs a warning

---

## Slice: Angular Assessment Generation UI

### Agent Brief
Build the Marker-facing screen for generating and assigning assessments. The Marker selects a candidate and time limit. A checkbox controls whether questions are manually chosen — unchecked sends an empty `questionIds` array (system assigns); checked navigates to a dedicated question selection page. The question selection page filters by type (MCQ / TEXT / DOC / GROUP) and subject (= category), both independently. Questions previously seen by the candidate this year show a warning badge. Submission happens directly from the question page and shows the invitation link inline.

### Package Tree Additions

**Frontend**
```
src/app/features/assessment-generation/
  components/
    assessment-generate/
      assessment-generate.component.ts       ← candidate selector + time limit + manual checkbox
      assessment-generate.component.html
    question-selection/                       ← separate page, reached only via manual checkbox
      question-selection.component.ts
      question-selection.component.html
    assessment-confirmation/
      assessment-confirmation.component.ts   ← inline, shown after submit on either page
  assessment-generation.routes.ts
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

**`AssessmentService`** — real implementation:
```typescript
generateAssessment(request: AssessmentRequest): Observable<AssessmentResponse>
// POST /api/assessments
// questionIds: [] when auto-assign; populated array when manual

getSeenQuestionIds(candidateId: string): Observable<string[]>
// GET /api/candidates/{candidateId}/seen-questions
// Returns question UUIDs seen by the candidate this calendar year
```

### Frontend Components

**`AssessmentGenerateComponent`** (`/assessments/generate`)
- `changeDetection: OnPush`
- Candidate list (cards, click to select) backed by `UserService.getCandidates()`
- Time limit input (number, min 5 minutes, default 60)
- Checkbox: "Manually select questions"
  - **Unchecked**: "Generate assessment" button → `generateAssessment({ questionIds: [] })` → inline confirmation
  - **Checked**: "Select questions" button → `router.navigate(['questions'], { queryParams: { candidateId, candidateName, timeLimit } })`

**`QuestionSelectionComponent`** (`/assessments/generate/questions`)
- `changeDetection: OnPush`
- Reads `candidateId`, `candidateName`, `timeLimit` from query params on init
- Loads `getSeenQuestionIds(candidateId)` on init
- **Type filter pills**: All / MCQ / TEXT / DOC / GROUP — filters question list
- **Subject filter pills**: All / [unique categories from questions] — filters question list independently
- Both filters apply simultaneously (AND logic)
- Question rows: checkbox + type tag + category label + amber "Already seen" badge if ID is in seenQuestionIds
- "Generate assessment (N selected)" button in header → `generateAssessment(...)` → inline `AssessmentConfirmationComponent`

**`AssessmentConfirmationComponent`**
- Signal inputs: `assessment: AssessmentResponse`, `candidateName: string`, `questionCount: number`
- Displays candidate, question count, time limit, status
- Copyable invitation link with clipboard feedback

### Route Additions
```typescript
// assessment-generation.routes.ts
export const assessmentGenerationRoutes: Routes = [
  { path: '',          loadComponent: () => AssessmentGenerateComponent  },
  { path: 'questions', loadComponent: () => QuestionSelectionComponent   },
];

// app.routes.ts (already wired)
{ path: 'assessments/generate', canActivate: [roleGuard], data: { roles: ['MARKER'] },
  loadChildren: () => import('./features/assessment-generation/assessment-generation.routes') }
```

### Testing
- `AssessmentService` unit test: `generateAssessment()` posts to correct endpoint; empty `questionIds` sends valid request
- `QuestionSelectionComponent` unit test: type + subject filters both narrow the list; seen question shows amber badge; selection emits correct IDs; submit calls service with correct payload
- `AssessmentGenerateComponent` unit test: unchecked checkbox calls service directly; checked checkbox navigates to questions route with correct query params

### Done When
- Marker at `/assessments/generate` can select a candidate + time limit and submit without manual questions
- Marker can tick "Manually select questions" → navigate to `/assessments/generate/questions`
- Question selection page filters by type AND subject independently
- Seen-question amber badge visible before submission
- After submission on either path, invitation link is displayed and copyable
