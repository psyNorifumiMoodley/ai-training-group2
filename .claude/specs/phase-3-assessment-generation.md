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
Implement `Assessment` entity and the core generation logic in `AssessmentService`. Two business rules are enforced here: (1) no-repeat questions scoped to the current calendar year, (2) configurable doc question limit read from `assessment.doc-question-limit`. The endpoint wires to this service and returns the saved assessment.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    Assessment.java
    AssessmentStatus.java         ← enum: PENDING, IN_PROGRESS, SUBMITTED, MARKED
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
  - `@ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "assessment_question_link", ...) List<AssessmentQuestion> questions`
  - `@ToString.Exclude` on `questions`

### Liquibase Changesets
None.

### Repositories

**`AssessmentRepository extends JpaRepository<Assessment, UUID>`**
```java
// For no-repeat rule: find all question IDs seen by candidate in submitted assessments this year
@Query("""
  SELECT DISTINCT aq.id FROM Assessment a
  JOIN a.questions aq
  WHERE a.candidate.id = :candidateId
    AND a.status = 'SUBMITTED'
    AND YEAR(a.createdAt) = :year
""")
List<UUID> findSeenQuestionIdsByCandidateAndYear(UUID candidateId, int year);

List<Assessment> findByCandidateId(UUID candidateId);
Page<Assessment> findByStatus(AssessmentStatus status, Pageable pageable);
```

### Services

**`AssessmentService`**
```java
@Value("${assessment.doc-question-limit}")
private int docQuestionLimit;

AssessmentResponse generate(AssessmentRequest request);
// 1. Resolve candidate (throws NotFoundException if not found)
// 2. Resolve question entities from questionIds (throws NotFoundException for any missing)
// 3. Filter out questions seen by candidate this calendar year (no-repeat rule)
// 4. If all requested questions filtered → throw UnprocessableException (422)
// 5. Count DOC-type questions; if > docQuestionLimit → throw ValidationException (400)
// 6. Persist Assessment(status=PENDING, questions=filtered, timeLimitMinutes)
// 7. Trigger invitation token generation (async, handled in token slice)
// 8. Return AssessmentResponse

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
  - Candidate seen question X this year → X excluded from generated assessment
  - Candidate seen question X last year → X included
  - All questions excluded → throws `UnprocessableException`
  - DOC questions > `docQuestionLimit` → throws `ValidationException`
  - `docQuestionLimit` read from `@Value` not hardcoded (test with limit=2 to confirm configurable)
- `AssessmentRepositoryTest` (`@DataJpaTest`): `findSeenQuestionIdsByCandidateAndYear` returns correct question IDs

### Done When
- `POST /api/assessments` with valid body creates `assessment` row with status `PENDING`
- Previously seen question (same year) excluded silently
- Too many DOC questions returns 400
- All questions excluded returns 422
- `assessment.doc-question-limit=1` in `application.properties` controls the limit

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
Build the Marker-facing screen for generating and assigning assessments. The form lets the Marker pick a candidate, browse question banks, select questions, and set a time limit. Questions already seen by the candidate this year should show a warning indicator. After generation, display the invitation link.

### Package Tree Additions

**Frontend**
```
src/app/features/assessment-generation/
  components/
    assessment-generate/
      assessment-generate.component.ts
      assessment-generate.component.html
    question-picker/
      question-picker.component.ts
      question-picker.component.html
    assessment-confirmation/
      assessment-confirmation.component.ts
      assessment-confirmation.component.html
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

getSeenQuestionIds(candidateId: string): Observable<string[]>
// GET /api/candidates/{candidateId}/seen-questions
// Backend endpoint needed: returns list of question UUIDs seen this year by candidate
```

### Frontend Components

**`AssessmentGenerateComponent`**
- `changeDetection: OnPush`
- Step 1: Candidate selector (dropdown backed by `UserService.getCandidates()`)
- Step 2: Question bank selector + `QuestionPickerComponent`
- Step 3: Time limit input (number, min 5 minutes)
- On submit: calls `AssessmentService.generateAssessment()`
- On success: navigates to `AssessmentConfirmationComponent` with result

**`QuestionPickerComponent`**
- `input()`: `bankId: string`, `seenQuestionIds: string[]` (Signal inputs)
- Displays paginated question list from selected bank
- Each question row: checkbox to select + warning badge if `id` is in `seenQuestionIds`
- `output()`: `questionsSelected` — emits `string[]` of selected question IDs

**`AssessmentConfirmationComponent`**
- `input()`: `assessment: AssessmentResponse` (Signal input)
- Displays assessment ID, candidate, time limit, and copyable invitation link

### Route Additions
```typescript
// assessment-generation.routes.ts
export const assessmentGenerationRoutes: Routes = [
  { path: '', component: AssessmentGenerateComponent, canActivate: [RoleGuard], data: { role: 'MARKER' } }
];

// app.routes.ts
{ path: 'assessments/generate', loadChildren: () => import('./features/assessment-generation/assessment-generation.routes') }
```

### Testing
- `AssessmentService` unit test: `generateAssessment()` posts to correct endpoint; error responses mapped correctly
- `QuestionPickerComponent` unit test: seen question shows warning badge; unselected question can be toggled; selection emits correct IDs
- `AssessmentGenerateComponent` unit test: form invalid without candidate/questions/time limit; valid submit calls service

### Done When
- Marker can navigate to `/assessments/generate`, select candidate, pick questions (with seen-question warnings), set time, and submit
- After submission, invitation link is displayed and copyable
- Seen-question warnings visible before submission
