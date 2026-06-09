# Phase 5 — Manual Marking & Results

> **Epic:** Phase 5 — Marking & Results
> **Delivery:** Slice 0 merges first; all other slices run in parallel.
> **Dependency:** Phase 0 + Phase 4 fully merged (submissions must exist before marking can begin).

---

## Slice 0: Backend API Contracts *(merge first)*

### Agent Brief
Stub all Phase 5 endpoints so the marking queue slice, finalisation slice, feedback slice, and Angular UI slice can develop simultaneously. The feedback endpoint (`GET /api/assessments/{id}/feedback`) is the candidate-facing endpoint — stub it as public-ish (requires CANDIDATE JWT) from the start.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  dto/
    AssessmentSummaryResponse.java   ← record { UUID id, String candidateName, String submittedAt, String status }
    ResponseReviewItem.java          ← record { UUID responseId, UUID questionId, String questionBody, String questionType, Object answer, Boolean correct, String feedbackDraft }
    FeedbackUpdateRequest.java       ← record { String feedbackText }
    FeedbackItem.java                ← record { UUID questionId, String questionBody, String feedbackText }
```

**Frontend**
```
src/app/
  core/
    services/
      marking.service.ts             ← stub
      feedback.service.ts            ← stub
    models/
      marking.model.ts
```

### Entities
None in this slice.

### Liquibase Changesets
None — `feedback` table exists from Phase 0 baseline.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`AssessmentController`** additions — stubs
```
GET   /api/assessments?status=SUBMITTED             → 200 PageResponse<AssessmentSummaryResponse> (hardcoded)
  @PreAuthorize("hasRole('MARKER')")

GET   /api/assessments/{id}/responses               → 200 List<ResponseReviewItem> (hardcoded)
  @PreAuthorize("hasRole('MARKER')")

PATCH /api/assessments/{id}/responses/{responseId}  → 200 (hardcoded)
  @PreAuthorize("hasRole('MARKER')")

POST  /api/assessments/{id}/finalise                → 200 (hardcoded)
  @PreAuthorize("hasRole('MARKER')")

PATCH /api/assessments/{id}/feedback/{questionId}   → 200 (hardcoded)
  @PreAuthorize("hasRole('MARKER')")

GET   /api/assessments/{id}/feedback                → 200 List<FeedbackItem> (hardcoded)
  @PreAuthorize("hasRole('CANDIDATE')")
```

### Frontend Type Definitions
```typescript
// core/models/marking.model.ts
export interface AssessmentSummaryResponse {
  id: string;
  candidateName: string;
  submittedAt: string;
  status: AssessmentStatus;
}
export interface ResponseReviewItem {
  responseId: string;
  questionId: string;
  questionBody: string;
  questionType: QuestionType;
  answer: unknown;
  correct: boolean | null;
  feedbackDraft: string;
}
export interface FeedbackUpdateRequest { feedbackText: string; }
export interface FeedbackItem { questionId: string; questionBody: string; feedbackText: string; }
```

### Frontend Services

**`MarkingService`** — stubs returning `EMPTY`
```typescript
getSubmittedAssessments(page: number, size: number): Observable<PageResponse<AssessmentSummaryResponse>>
getResponsesForReview(assessmentId: string): Observable<ResponseReviewItem[]>
updateResponseFeedback(assessmentId: string, responseId: string, request: FeedbackUpdateRequest): Observable<void>
finaliseMarking(assessmentId: string): Observable<void>
updateQuestionFeedback(assessmentId: string, questionId: string, request: FeedbackUpdateRequest): Observable<void>
```

**`FeedbackService`** — stubs returning `EMPTY`
```typescript
getMyFeedback(assessmentId: string): Observable<FeedbackItem[]>
```

### Frontend Components
None.

### Route Additions
None.

### Testing
- Stubs return correct status codes; role enforcement returns 403 for wrong role

### Done When
- All stubs compile and return correct HTTP status codes
- Angular model types compile cleanly

---

## Slice: Marking Queue & Response Review

### Agent Brief
Implement the marking queue (list of submitted assessments) and the per-assessment response review endpoint. The response review must include MCQ auto-mark results and expose text/doc responses for the Marker to annotate. Marker feedback is persisted per response.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    MarkingService.java
```

### Entities
No new entities — uses `Assessment`, `Response` hierarchy from Phase 4.

### Liquibase Changesets
None.

### Repositories

Add to `AssessmentRepository`:
```java
Page<Assessment> findByStatus(AssessmentStatus status, Pageable pageable);
```

Add to `ResponseRepository`:
```java
@Query("SELECT r FROM Response r JOIN FETCH r.question WHERE r.assessment.id = :assessmentId")
List<Response> findWithQuestionByAssessmentId(UUID assessmentId);
```

### Services

**`MarkingService`**
```java
PageResponse<AssessmentSummaryResponse> listSubmitted(int page, int size);
// Returns paginated SUBMITTED assessments with candidate name and submittedAt

List<ResponseReviewItem> getResponsesForReview(UUID assessmentId);
// Loads all responses for assessment
// Maps each to ResponseReviewItem:
//   MCQ: answer = selectedAnswers, correct = mcqResponse.correct, feedbackDraft = auto-generated (see below)
//   TEXT/DOC: answer = text/filePath, correct = null, feedbackDraft = "" (empty, Marker must fill)
// Auto-generated MCQ feedbackDraft: correct == true → "Correct"; correct == false → "Incorrect — please review this topic"

void updateResponseFeedback(UUID assessmentId, UUID responseId, FeedbackUpdateRequest request);
// Validates assessment exists and is SUBMITTED
// Updates response feedback field (add 'markerFeedback' column to response OR use Feedback entity)
```

> **Note:** The `feedback` table (from Phase 0 baseline) stores per-question feedback entries separately from the response. `MarkingService` works with the `feedback` table, not by adding a column to `response`.

**`FeedbackService`** (shared with Phase 5 finalisation slice)
```java
Feedback getOrCreateDraft(UUID assessmentId, UUID questionId);
// If no feedback row exists: create with auto-generated draft text
// MCQ: draft = "Correct" or "Incorrect — please review this topic" based on McqResponse.correct
// TEXT/DOC: draft = ""

void updateFeedback(UUID assessmentId, UUID questionId, FeedbackUpdateRequest request);
```

### Entities

**`Feedback`**
- Table: `feedback`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) Assessment assessment`
  - `@ManyToOne(fetch = FetchType.LAZY) AssessmentQuestion question`
  - `@Column(columnDefinition = "TEXT") String draft`
  - `boolean finalised`

### Liquibase Changesets
None — `feedback` table created in Phase 0 baseline.

### Repositories

**`FeedbackRepository extends JpaRepository<Feedback, UUID>`**
```java
Optional<Feedback> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);
List<Feedback> findByAssessmentId(UUID assessmentId);
boolean existsByAssessmentIdAndDraftIsEmpty(UUID assessmentId);
```

### Controllers
`AssessmentController` GET submissions and GET responses endpoints wired to `MarkingService`.
PATCH response feedback endpoint wired to `FeedbackService.updateFeedback()`.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `MarkingServiceTest` (unit):
  - MCQ correct response → feedbackDraft = "Correct"
  - MCQ incorrect response → feedbackDraft = "Incorrect — please review this topic"
  - TEXT response → feedbackDraft = ""
- `FeedbackRepositoryTest` (`@DataJpaTest`): `findByAssessmentIdAndQuestionId` returns draft; upsert pattern works
- `MarkingControllerTest` (`@WebMvcTest`): GET responses → 200; CANDIDATE JWT → 403

### Done When
- `GET /api/assessments?status=SUBMITTED` returns paginated list of submitted assessments
- `GET /api/assessments/{id}/responses` returns all responses with auto-generated MCQ feedback drafts
- `PATCH .../responses/{responseId}` persists Marker's edited feedback text

---

## Slice: Finalisation & Status Transition

### Agent Brief
Implement the finalisation endpoint. Before transitioning to `MARKED`, validate that every text and doc response for the assessment has a non-empty feedback entry in the `feedback` table. If any are empty, return 400 listing the problematic questions. On success, transition status to `MARKED`.

### Package Tree Additions

**Backend**
No new files — logic added to `AssessmentService` and `MarkingService`.

### Entities
No new entities.

### Liquibase Changesets
None.

### Repositories

Add to `FeedbackRepository`:
```java
// Returns question IDs where feedback draft is null or blank
@Query("""
  SELECT f.question.id FROM Feedback f
  WHERE f.assessment.id = :assessmentId
    AND (f.draft IS NULL OR TRIM(f.draft) = '')
""")
List<UUID> findQuestionsWithEmptyFeedback(UUID assessmentId);
```

### Services

**`AssessmentService`** additions:
```java
@Transactional
AssessmentResponse finalise(UUID assessmentId);
// 1. Load assessment; verify status == SUBMITTED → else 409
// 2. feedbackRepo.findQuestionsWithEmptyFeedback(assessmentId) → if non-empty → 400 with list
// 3. Set all Feedback.finalised = true
// 4. Set assessment.status = MARKED, save
// 5. Trigger feedback email (async, handled by email slice)
// 6. Return updated AssessmentResponse
```

### Controllers
`AssessmentController` `POST /api/assessments/{id}/finalise` wired to `AssessmentService.finalise()`.

### Frontend Type Definitions
None new.

### Frontend Services
Real implementation in Angular UI slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `FinaliseTest` (`@SpringBootTest`, Testcontainers):
  - All text/doc feedback filled → status transitions to MARKED
  - Any text/doc feedback empty → 400 with question IDs listed
  - Already MARKED → 409
  - Not yet SUBMITTED (PENDING/IN_PROGRESS) → 409
  - MCQ-only assessment (all feedback auto-filled) → finalises without 400

### Done When
- `POST /api/assessments/{id}/finalise` with all feedback filled → status = MARKED
- Any empty text/doc feedback → 400 listing unfilled question IDs
- Double finalise → 409

---

## Slice: Feedback Email & Candidate Feedback View

### Agent Brief
Implement the feedback email triggered by finalisation and the `GET /api/assessments/{id}/feedback` endpoint for candidates. The email contains only curated feedback text per question — no scores or correct/incorrect flags. The candidate endpoint is only accessible after `MARKED` status.

### Package Tree Additions

**Backend**
No new files — logic in `EmailService` (already exists from Phase 3) and `FeedbackService`.

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None new.

### Services

**`FeedbackService`** additions:
```java
List<FeedbackItem> getCandidateFeedback(UUID assessmentId, UUID requestingCandidateId);
// 1. Load assessment
// 2. Verify assessment.candidate.id == requestingCandidateId → 403 otherwise
// 3. Verify status == MARKED → 403 if not
// 4. Load all Feedback rows for assessmentId
// 5. Map to FeedbackItem { questionId, questionBody, feedbackText=draft }
// NOTE: feedbackText is the Marker's curated text; no correct/incorrect data included
```

**`EmailService`** additions — already declared in Phase 3 interface:
```java
void sendFeedback(String toEmail, String candidateName, Map<UUID, String> feedbackByQuestion);
// Called by AssessmentService.finalise() asynchronously
// Email body: per-question list of questionBody + feedbackText
// MUST NOT include any correct/incorrect flags, scores, or MCQ answer data
```

**`AssessmentService.finalise()`** — call after marking:
```java
Map<UUID, String> feedbackMap = feedbackRepo.findByAssessmentId(assessmentId)
    .stream().collect(toMap(f -> f.getQuestion().getId(), Feedback::getDraft));
emailService.sendFeedback(candidate.getUser().getEmail(), candidate.getUser().getName(), feedbackMap);
```

### Controllers
`AssessmentController` `GET /api/assessments/{id}/feedback` wired to `FeedbackService.getCandidateFeedback()`.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services

**`FeedbackService`** — real implementation:
```typescript
getMyFeedback(assessmentId: string): Observable<FeedbackItem[]>
// GET /api/assessments/{assessmentId}/feedback
```

### Frontend Components
None — Angular feedback view is part of the Angular Marking UI slice.

### Route Additions
None — routes added in Angular Marking UI slice.

### Testing
- `FeedbackEmailTest` (unit, mock `EmailService`):
  - Email sent with correct recipient after finalisation
  - Email body contains `feedbackText` per question
  - Email body does NOT contain any `correct` boolean, score, or selectedAnswers data
- `CandidateFeedbackEndpointTest` (`@WebMvcTest`):
  - MARKED assessment + correct candidate JWT → 200 with feedback items
  - MARKED assessment + different candidate JWT → 403
  - SUBMITTED (not MARKED) assessment → 403
  - MARKER JWT → 403

### Done When
- Feedback email arrives at candidate email after Marker finalises
- `GET /api/assessments/{id}/feedback` returns feedback for the correct candidate
- Feedback response contains no correct/incorrect data — text only
- Wrong candidate or pre-MARKED status returns 403

---

## Slice: Angular Marking UI

### Agent Brief
Build the Marker-facing marking screens and the Candidate-facing feedback view. The marking detail screen pre-populates MCQ feedback drafts and requires Marker to fill in text/doc feedback before the Finalise button enables. The candidate feedback screen shows per-question feedback text only.

### Package Tree Additions

**Frontend**
```
src/app/features/marking/
  components/
    marking-queue/
      marking-queue.component.ts
      marking-queue.component.html
    marking-detail/
      marking-detail.component.ts
      marking-detail.component.html
    feedback-item-editor/
      feedback-item-editor.component.ts
      feedback-item-editor.component.html
  marking.routes.ts

src/app/features/candidate-feedback/
  components/
    candidate-feedback/
      candidate-feedback.component.ts
      candidate-feedback.component.html
  candidate-feedback.routes.ts
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

**`MarkingService`** — real implementation:
```typescript
getSubmittedAssessments(page: number, size: number): Observable<PageResponse<AssessmentSummaryResponse>>
// GET /api/assessments?status=SUBMITTED&page=&size=

getResponsesForReview(assessmentId: string): Observable<ResponseReviewItem[]>
// GET /api/assessments/{assessmentId}/responses

updateQuestionFeedback(assessmentId: string, questionId: string, request: FeedbackUpdateRequest): Observable<void>
// PATCH /api/assessments/{assessmentId}/feedback/{questionId}

finaliseMarking(assessmentId: string): Observable<void>
// POST /api/assessments/{assessmentId}/finalise
```

**`FeedbackService`** — real implementation:
```typescript
getMyFeedback(assessmentId: string): Observable<FeedbackItem[]>
// GET /api/assessments/{assessmentId}/feedback
```

### Frontend Components

**`MarkingQueueComponent`**
- `changeDetection: OnPush`
- Paginated table of submitted assessments: candidate name, submitted date
- Clicking a row navigates to `MarkingDetailComponent`

**`MarkingDetailComponent`**
- `input()`: `assessmentId: string` (Signal, from route param)
- Loads `ResponseReviewItem[]` on init
- Renders `FeedbackItemEditorComponent` for each question
- Tracks a `Signal<boolean>` — `canFinalise` — true only when all text/doc feedback entries are non-empty
- Finalise button: disabled when `!canFinalise`; on click → confirmation dialog → `MarkingService.finaliseMarking()`
- On finalise success: show success toast, navigate back to queue

**`FeedbackItemEditorComponent`**
- `input()`: `item: ResponseReviewItem` (Signal input)
- `output()`: `feedbackChanged` emits `{ questionId: string, feedbackText: string }`
- MCQ rows: shows question body, candidate's selected answers, pre-filled draft feedback (editable textarea)
- TEXT rows: shows question body, candidate's answer (read-only), empty feedback textarea
- DOC rows: shows question body, file download link, empty feedback textarea
- Feedback textarea triggers `feedbackChanged` output on blur; parent component calls `MarkingService.updateQuestionFeedback()`

**`CandidateFeedbackComponent`**
- `changeDetection: OnPush`
- Reads `assessmentId` from route param (Signal input)
- Calls `FeedbackService.getMyFeedback()` on init
- Renders per-question feedback: question body + Marker's feedback text
- No scores, no correct/incorrect indicators

### Route Additions
```typescript
// marking.routes.ts
export const markingRoutes: Routes = [
  { path: '', component: MarkingQueueComponent, canActivate: [RoleGuard], data: { role: 'MARKER' } },
  { path: ':assessmentId', component: MarkingDetailComponent, canActivate: [RoleGuard], data: { role: 'MARKER' } }
];

// candidate-feedback.routes.ts
export const candidateFeedbackRoutes: Routes = [
  { path: ':assessmentId', component: CandidateFeedbackComponent, canActivate: [AuthGuard] }
];

// app.routes.ts
{ path: 'marking', loadChildren: () => import('./features/marking/marking.routes') },
{ path: 'my-feedback', loadChildren: () => import('./features/candidate-feedback/candidate-feedback.routes') }
```

### Testing
- `MarkingService` unit test: each method calls the correct endpoint
- `FeedbackItemEditorComponent` unit test: MCQ item shows pre-filled draft; TEXT item shows empty textarea; `feedbackChanged` emits on blur
- `MarkingDetailComponent` unit test:
  - `canFinalise` is false when any TEXT/DOC feedback is empty
  - `canFinalise` becomes true when all TEXT/DOC feedback filled
  - MCQ-only assessments → `canFinalise` true immediately
  - Finalise calls service and navigates to queue
- `CandidateFeedbackComponent` unit test: renders feedback items; no score/correct data rendered

### Done When
- Marker navigates to `/marking`, sees submitted assessments
- Clicking an assessment opens marking detail with auto-filled MCQ feedback drafts and empty text/doc feedback fields
- Finalise button disabled until all text/doc fields filled
- After finalisation, Marker sees success message
- Candidate navigates to `/my-feedback/{assessmentId}` and sees per-question written feedback — no scores or marks visible
