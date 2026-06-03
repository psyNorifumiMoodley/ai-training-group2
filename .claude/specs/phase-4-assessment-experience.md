# Phase 4 — Assessment Experience & Submission

> **Epic:** Phase 4 — Assessment Experience
> **Delivery:** Slice 0 merges first; all other slices run in parallel.
> **Dependency:** Phase 0 + Phase 3 fully merged (assessments must exist with invitation tokens).

---

## Slice 0: Backend API Contracts *(merge first)*

### Agent Brief
Define all Phase 4 endpoint stubs so the token-access slice, response persistence slice, timer slice, and Angular UI slice can develop simultaneously. The public `GET /api/assessments/access/{token}` endpoint is the most critical — stub it first as it is used by the Angular UI slice from day one.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  dto/
    AssessmentAccessResponse.java  ← record { UUID assessmentId, List<QuestionResponse> questions, int remainingSeconds }
    ResponseRequest.java           ← record { String type, Object answer } (polymorphic — sealed)
    McqResponseRequest.java        ← record { List<String> selectedAnswers }
    TextResponseRequest.java       ← record { String answer }
    DocResponseRequest.java        ← record { String filePath }
    GroupResponseRequest.java      ← record { Map<UUID, ResponseRequest> childResponses }
    SubmitRequest.java             ← record {} (empty — signals intent to submit)
```

**Frontend**
```
src/app/
  core/
    services/
      candidate-assessment.service.ts  ← stub
    models/
      assessment-session.model.ts
```

### Entities
None in this slice.

### Liquibase Changesets
None — all response tables exist from Phase 0 baseline.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`AssessmentController`** additions — stubs
```
GET  /api/assessments/access/{token}                         → 200 AssessmentAccessResponse (hardcoded, public endpoint)
PUT  /api/assessments/{id}/responses/{questionId}            → 200 (hardcoded)
POST /api/assessments/{id}/submit                            → 200 (hardcoded)
```

### Frontend Type Definitions
```typescript
// core/models/assessment-session.model.ts
export interface AssessmentAccessResponse {
  assessmentId: string;
  questions: QuestionResponse[];
  remainingSeconds: number;
  candidateToken: string;  // short-lived candidate JWT; stored via AuthService for subsequent save/submit calls
}
export interface McqResponseRequest  { selectedAnswers: string[]; }
export interface TextResponseRequest { answer: string; }
export interface DocResponseRequest  { filePath: string; }
export interface GroupResponseRequest { childResponses: Record<string, ResponseRequest>; }
export type ResponseRequest = McqResponseRequest | TextResponseRequest | DocResponseRequest | GroupResponseRequest;
```

> **Note:** `McqQuestionResponse` must also include `multiCorrect: boolean` (see Phase 2 / `question.model.ts`). The renderer uses this flag — not `correctAnswers.length` — to decide radio vs checkbox.

### Frontend Services

**`CandidateAssessmentService`** — stubs returning `EMPTY`
```typescript
accessAssessment(token: string): Observable<AssessmentAccessResponse>
saveResponse(assessmentId: string, questionId: string, request: ResponseRequest): Observable<void>
submitAssessment(assessmentId: string): Observable<void>
```

> **Auth note:** `saveResponse` and `submitAssessment` require a valid JWT. Candidates do not log in via the normal login flow — instead the `candidateToken` returned by `accessAssessment` is stored in `AuthService` and attached to subsequent calls by the JWT interceptor.

### Frontend Components
None.

### Route Additions
None.

### Testing
- Public stub: `GET /api/assessments/access/any-token` returns 200 (no auth required)
- Protected stubs: require valid JWT

### Done When
- Stubs compile and return correct status codes
- Angular model types compile with no errors

---

## Slice: Token-Gated Access & Status Machine

### Agent Brief
Implement the `GET /api/assessments/access/{token}` endpoint. Validate the invitation token (signature check), then enforce session expiry (if `start_time` is set and `start_time + timeLimitMinutes` is past → 401). If access is valid and status is `PENDING`, transition to `IN_PROGRESS` and record `startTime`. Return questions and remaining seconds. Enforce that `SUBMITTED`/`MARKED` assessments return 409.

### Package Tree Additions

**Backend**
No new files — logic lives in `AssessmentService` and the existing `AssessmentController`.

### Entities
`Assessment` — no new fields; `startTime` and `status` already mapped.

### Liquibase Changesets
None.

### Repositories

Add to `AssessmentRepository`:
```java
Optional<Assessment> findByInvitationToken(String token);
```

### Services

**`AssessmentService`** additions:
```java
@Transactional
AssessmentAccessResponse access(String token);
// 1. invitationTokenUtil.isSignatureValid(token) → 401 if invalid
// 2. repo.findByInvitationToken(token) → 401 if not found
// 3. status == SUBMITTED || MARKED → 409
// 4. startTime != null && Instant.now() > startTime + timeLimitMinutes → 401 (session expired)
// 5. If status == PENDING: set status=IN_PROGRESS, startTime=now(), save
// 6. Compute remainingSeconds = (startTime + timeLimitMinutes) - now()
// 7. Generate a short-lived candidate JWT via JwtUtil (sub = candidate userId, role = CANDIDATE)
// 8. Blank out correctAnswers on all McqQuestionResponse objects before returning (prevent answer leakage)
// 9. Return AssessmentAccessResponse with questions, remainingSeconds, and candidateToken
```

> **Invitation link format:** The email sent in the assessment generation phase must use the path `{frontendBaseUrl}/assessment/access/{token}` — matching the `access/:token` route.

### Controllers
`AssessmentController` `GET /api/assessments/access/{token}` wired to `AssessmentService.access()`.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `AssessmentAccessTest` (`@SpringBootTest`, Testcontainers):
  - Valid token + PENDING status → 200, status transitions to IN_PROGRESS, startTime set
  - Valid token + already IN_PROGRESS → 200, startTime unchanged
  - Valid token + SUBMITTED → 409
  - Tampered token → 401
  - Expired session (startTime set, time elapsed) → 401
  - Response includes a non-null `candidateToken` (valid JWT)
  - MCQ questions in response have empty `correctAnswers` (answer leakage prevention)

### Done When
- Valid token + PENDING assessment → 200, status = IN_PROGRESS, remainingSeconds > 0
- Already SUBMITTED assessment → 409
- Bad token → 401

---

## Slice: Response Persistence & Auto-Save

### Agent Brief
Implement the `Response` entity hierarchy and the `PUT /api/assessments/{id}/responses/{questionId}` endpoint. Support all four response types: MCQ, text, doc, and question group. Responses are upserted — if one already exists for this assessment + question pair, update it; otherwise create it. Reject saves on a `SUBMITTED` assessment.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    Response.java                  ← abstract entity, TABLE_PER_CLASS
    McqResponse.java
    TextResponse.java
    DocResponse.java
    QuestionGroupResponse.java
  repository/
    ResponseRepository.java
  service/
    ResponseService.java
```

### Entities

**`Response`** — abstract
- Table strategy: `@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)`
- Table: `response`
- Extends: `BaseEntity`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) Assessment assessment`
  - `@ManyToOne(fetch = FetchType.LAZY) AssessmentQuestion question`

**`McqResponse extends Response`**
- Table: `mcq_response`
- Fields:
  - `@JdbcTypeCode(SqlTypes.JSON) List<String> selectedAnswers`
  - `Boolean correct` — null until auto-marked

**`TextResponse extends Response`**
- Table: `text_response`
- Fields: `@Column(columnDefinition = "TEXT") String answer`

**`DocResponse extends Response`**
- Table: `doc_response`
- Fields: `String filePath`

**`QuestionGroupResponse extends Response`**
- Table: `question_group_response`
- Fields:
  - `@OneToMany(mappedBy = "parentGroupResponse", cascade = CascadeType.ALL, orphanRemoval = true) List<Response> childResponses`

### Liquibase Changesets
None.

### Repositories

**`ResponseRepository extends JpaRepository<Response, UUID>`**
```java
Optional<Response> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);
List<Response> findByAssessmentId(UUID assessmentId);
```

### Services

**`ResponseService`**
```java
@Transactional
void saveResponse(UUID assessmentId, UUID questionId, ResponseRequest request);
// 1. Load assessment via AssessmentService.getOrThrow()
// 2. assessment.status == SUBMITTED → throw ConflictException (409)
// 3. assessment.status != IN_PROGRESS → throw ConflictException (409)
// 4. Upsert: findByAssessmentIdAndQuestionId; if present update, else create
// 5. Dispatch to correct response type based on request type
// 6. For GroupResponseRequest: recursively upsert child responses

List<Response> getResponsesForAssessment(UUID assessmentId);
```

### Controllers
`AssessmentController` `PUT /api/assessments/{id}/responses/{questionId}` wired to `ResponseService.saveResponse()`.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `ResponseServiceTest` (unit):
  - Save MCQ response → creates `mcq_response` row
  - Save on SUBMITTED assessment → 409
  - Save on same question twice → updates existing row (upsert)
  - GroupResponse: child responses persisted with correct parent link
- `ResponseRepositoryTest` (`@DataJpaTest`): `findByAssessmentIdAndQuestionId` returns correct record for each type

### Done When
- `PUT .../responses/{questionId}` with MCQ body creates/updates `mcq_response` row
- Same for TEXT, DOC, GROUP (including child responses)
- PUT on SUBMITTED assessment returns 409

---

## Slice: Server-Side Timer & MCQ Auto-Marking

### Agent Brief
Implement the submission endpoint. Check if the time has expired — if so, auto-submit and set `auto_submitted = true`. On any submission, immediately auto-mark all MCQ responses by comparing `selectedAnswers` to `correctAnswers`. Non-MCQ responses are left unmarked for the Marker.

### Package Tree Additions

**Backend**
No new files — logic in `AssessmentService` and `ResponseService`.

### Entities
No new entities.

### Liquibase Changesets
None.

### Repositories
None new.

### Services

**`AssessmentService`** additions:
```java
@Transactional
AssessmentResponse submit(UUID assessmentId, UUID requestingUserId);
// 1. Load assessment; verify requesting user is the assigned candidate
// 2. status == SUBMITTED → throw ConflictException (409)
// 3. status != IN_PROGRESS → throw ConflictException (409)
// 4. Check time: if Instant.now() > startTime + timeLimitMinutes → autoSubmit = true
// 5. Set status = SUBMITTED
// 6. Trigger MCQ auto-marking
// 7. Save and return
```

**`ResponseService`** additions:
```java
void autoMarkMcqResponses(UUID assessmentId);
// For each McqResponse on this assessment:
//   Load corresponding McqQuestion.correctAnswers
//   Compare: selectedAnswers matches correctAnswers (set equality, order-insensitive)
//   Set response.correct = true/false
//   Save
```

### Controllers
`AssessmentController` `POST /api/assessments/{id}/submit` wired to `AssessmentService.submit()`.

### Frontend Type Definitions
None new.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `AssessmentSubmitTest` (`@SpringBootTest`, Testcontainers):
  - Submit in time → status = SUBMITTED, autoSubmitted = false
  - Submit after time elapsed → status = SUBMITTED, autoSubmitted = true
  - Submit twice → 409
- `McqAutoMarkTest` (unit):
  - Single correct answer: exact match → `correct = true`
  - Multi-correct: all selected → `correct = true`
  - Multi-correct: partial selection → `correct = false`
  - Multi-correct: wrong answers added → `correct = false`

### Done When
- `POST /api/assessments/{id}/submit` transitions status to SUBMITTED
- `autoSubmitted = true` when time has elapsed
- All MCQ responses have `correct` field populated after submission
- Second submission returns 409

---

## Slice: Angular Assessment Taking UI

### Agent Brief
Build the candidate-facing assessment experience. Entry is via invitation link (token in URL). The countdown is server-initialised from `remainingSeconds`. Auto-save triggers on each answer change (debounced 1.5s). Submit requires confirmation. After submission, show a confirmation screen. This feature has no login — access is token-only.

### Package Tree Additions

**Frontend**
```
src/app/features/assessment/
  components/
    assessment-access/
      assessment-access.component.ts     ← token landing page
      assessment-access.component.html
    assessment-taking/
      assessment-taking.component.ts
      assessment-taking.component.html
    question-renderer/
      question-renderer.component.ts     ← renders MCQ | TEXT | DOC | GROUP
      question-renderer.component.html
    countdown-timer/
      countdown-timer.component.ts
      countdown-timer.component.html
    assessment-confirmation/
      assessment-confirmation.component.ts
      assessment-confirmation.component.html
  assessment.routes.ts
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

**`CandidateAssessmentService`** — real implementation:
```typescript
accessAssessment(token: string): Observable<AssessmentAccessResponse>
// GET /api/assessments/access/{token} — no Authorization header needed

saveResponse(assessmentId: string, questionId: string, request: ResponseRequest): Observable<void>
// PUT /api/assessments/{assessmentId}/responses/{questionId}

submitAssessment(assessmentId: string): Observable<void>
// POST /api/assessments/{assessmentId}/submit
```

### Frontend Components

**`AssessmentAccessComponent`**
- Reads `token` from route param (Signal input)
- Calls `CandidateAssessmentService.accessAssessment(token)` on init
- On success: stores assessment in signal, navigates to `AssessmentTakingComponent`
- On 409: shows "already submitted" message
- On 401: shows "link expired or invalid" message

**`AssessmentTakingComponent`**
- `changeDetection: OnPush`
- Receives `AssessmentAccessResponse` via navigation state or resolved signal
- Stores `candidateToken` from the response via `AuthService` so the JWT interceptor authenticates save/submit calls
- Renders `CountdownTimerComponent` initialised from `remainingSeconds`
- Maintains `savedAnswers: signal<Record<string, ResponseRequest>>` — updated on every `answerChanged` event; passed as `savedAnswer` input to `QuestionRendererComponent`
- Renders `QuestionRendererComponent` for the current question, passing the saved answer for that question
- Navigation panel: `QuestionNavDotComponent` per question — `clicked` output wired to `goTo(index)`
- Auto-save: `answerChanged$` subject → `debounceTime(1500)` → `saveResponse()` — uses `takeUntilDestroyed()`
- Submit button → confirmation dialog → `submitAssessment()` → navigate to confirmation screen
- On timer reaching zero: triggers submit automatically

**`QuestionRendererComponent`**
- `input()`: `question: QuestionResponse`
- `input()`: `savedAnswer: ResponseRequest | undefined` — restores local UI state when question changes (via `effect()`)
- `output()`: `answerChanged` emits `{ questionId, request: ResponseRequest }`
- Conditionally renders:
  - MCQ: radio group (single correct) or checkbox group (multi-correct) — determined by `question.multiCorrect`, **not** `correctAnswers.length`
  - TEXT: textarea
  - DOC: file upload input
  - GROUP: renders each child `TextQuestion` as a labelled textarea

**`CountdownTimerComponent`**
- `input()`: `initialSeconds: number` (Signal)
- Counts down using `interval(1000)` with `takeUntilDestroyed()`
- `output()`: `expired` emits when countdown reaches zero
- Displays `MM:SS`; turns red at < 5 minutes

### Route Additions
```typescript
// assessment.routes.ts
export const assessmentRoutes: Routes = [
  { path: 'access/:token', component: AssessmentAccessComponent },  // public — no guard
  { path: ':assessmentId/take', component: AssessmentTakingComponent },
  { path: ':assessmentId/confirmation', component: AssessmentConfirmationComponent }
];

// app.routes.ts
{ path: 'assessment', loadChildren: () => import('./features/assessment/assessment.routes') }
```

### Testing
- `CandidateAssessmentService` unit test: each method calls the correct endpoint
- `CountdownTimerComponent` unit test: initialises from input; emits `expired` at zero; displays correct MM:SS
- `QuestionRendererComponent` unit test: MCQ renders radio based on `multiCorrect = false`, checkbox based on `multiCorrect = true`; `savedAnswer` input restores selection state; answer change emits correct payload
- `AssessmentTakingComponent` unit test: auto-save debounces; submit calls service; expired timer triggers submit; navigating between questions preserves and restores answer state via `savedAnswers`

### Done When
- Candidate opens invitation link → assessment loads with questions and countdown
- Selecting answers auto-saves (debounced) with no visible error
- Submit confirmation flow completes → confirmation screen shown
- Countdown reaching zero triggers auto-submission
- Expired/invalid token shows appropriate error message
