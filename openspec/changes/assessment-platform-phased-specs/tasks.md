## 1. Phase 0: Platform Foundation
> Epic: Phase 0 — Platform Foundation | Sequential delivery — each slice must merge before the next begins.

### Slice: Project Scaffolding
- [ ] 1.1 Initialise Spring Boot 3.x Maven project with `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `liquibase-core`
- [ ] 1.2 Initialise Angular 20 project with strict TypeScript, Tailwind CSS, PrimeIcons
- [ ] 1.3 Create `docker-compose.yml` with PostgreSQL on port 5432, backend on 8080, frontend on 4200
- [ ] 1.4 Add `.gitignore`, `README.md`, and root-level dev scripts
- [ ] 1.5 Set up CI pipeline (build + test on push to main)

### Slice: Database Baseline
- [ ] 1.6 Create `BaseEntity` with UUID PK (`gen_random_uuid()`), `createdAt`, `updatedAt`
- [ ] 1.7 Write Liquibase baseline changeset (`2026-01-01-001-baseline-schema`) with all domain tables: `app_user`, `candidate`, `question_bank`, `assessment_question` hierarchy, `assessment`, `submission`, `response` hierarchy
- [ ] 1.8 Add all FK, UNIQUE, and NOT NULL constraints to the baseline migration
- [ ] 1.9 Configure `spring.jpa.hibernate.ddl-auto=validate` and `spring.jpa.open-in-view=false`
- [ ] 1.10 Verify Liquibase applies cleanly on first start and is idempotent on restart

### Slice: JWT Authentication
- [ ] 1.11 Implement `AppUser` entity with `role` enum (`ADMIN`, `MARKER`, `CANDIDATE`) using `@Enumerated(EnumType.STRING)`
- [ ] 1.12 Implement `POST /api/auth/login` controller, service, and JWT generation utility
- [ ] 1.13 Configure Spring Security: stateless sessions, permit `/api/auth/login` and `GET /api/assessments/access/**`, protect all other endpoints
- [ ] 1.14 Implement `JwtAuthenticationFilter` to validate Bearer tokens on each request

### Slice: Shared API Contracts
- [ ] 1.15 Implement `GlobalExceptionHandler` returning `{ status, error, message, timestamp }` for all error types
- [ ] 1.16 Create Angular `AuthService` with login method, JWT storage, and `currentUser$` signal
- [ ] 1.17 Create Angular `JwtInterceptor` (attaches Bearer token to all requests; redirects to login on 401)
- [ ] 1.18 Create Angular `AuthGuard` and `RoleGuard` using the token's `role` claim
- [ ] 1.19 Set up Angular `environment.ts` with `apiBaseUrl` pointing to `http://localhost:8080/api`

---

## 2. Phase 1: User Management
> Epic: Phase 1 — User Management | Slice 0 (Contract) must merge before other slices begin. Slices A–D run in parallel.

### Slice 0: Backend API Contracts *(merge first — unblocks all other slices)*
- [ ] 2.1 Create stub `CandidateController` with empty/hardcoded responses for `POST /api/candidates` and `GET /api/candidates`
- [ ] 2.2 Create stub `MarkerController` with empty/hardcoded responses for `POST /api/markers` and `GET /api/markers` (These URLs shouldn't show the names of the roles in the frontend URLs)
- [ ] 2.3 Define `CandidateRequest`, `CandidateResponse`, `MarkerRequest`, `MarkerResponse` Java Records
- [ ] 2.4 Create Angular `UserService` with stub `registerCandidate()`, `registerMarker()`, `getCandidates()`, `getMarkers()` methods returning `EMPTY`

### Slice: Candidate Registration
- [ ] 2.5 Implement `Candidate` and `AppUser` JPA entities with 1-to-1 relationship
- [ ] 2.6 Implement `CandidateRepository` and `CandidateService` with registration logic and duplicate email check
- [ ] 2.7 Wire `CandidateController` to service; add `@Valid` request body validation
- [ ] 2.8 Write `@DataJpaTest` for `CandidateRepository` and `MockMvc` tests for `CandidateController`

### Slice: Marker Registration
- [ ] 2.9 Implement `AppUser` (Marker role) persistence via `MarkerService`
- [ ] 2.10 Wire `MarkerController` to service with role `ADMIN` access restriction
- [ ] 2.11 Write `@DataJpaTest` for marker persistence and `MockMvc` tests for `MarkerController`

### Slice: Role-Based Access Control
- [ ] 2.12 Configure Spring Security method-level `@PreAuthorize` for `ADMIN`, `MARKER`, `CANDIDATE` roles
- [ ] 2.13 Write integration tests verifying 403 responses for unauthorized role access on all Phase 1 endpoints

### Slice: Angular User Management UI
- [ ] 2.14 Implement `UserManagementComponent` with candidate list and paginated table (OnPush, signals)
- [ ] 2.15 Implement candidate registration reactive form with inline validation
- [ ] 2.16 Implement marker list and marker registration form
- [ ] 2.17 Wire Angular `UserService` to real API endpoints; remove stubs

---

## 3. Phase 2: Question Bank
> Epic: Phase 2 — Question Bank | Slice 0 (Contract) must merge before other slices begin. Slices A–D run in parallel.

### Slice 0: Backend API Contracts *(merge first — unblocks all other slices)*
- [ ] 3.1 Create stub `QuestionBankController` with empty responses for `POST /api/question-banks` and `GET /api/question-banks`
- [ ] 3.2 Create stub `QuestionController` with empty responses for `POST /api/question-banks/{bankId}/questions` and `GET /api/question-banks/{bankId}/questions`
- [ ] 3.3 Define request/response DTOs for all question types (MCQ, text, doc, group)
- [ ] 3.4 Create Angular `QuestionBankService` with stub methods

### Slice: Question Bank CRUD
- [ ] 3.5 Implement `QuestionBank` entity and `QuestionBankRepository`
- [ ] 3.6 Implement `QuestionBankService` with create, list (paginated), and get-by-id
- [ ] 3.7 Wire `QuestionBankController` to service with `MARKER` role restriction
- [ ] 3.8 Write tests for question bank CRUD

### Slice: MCQ and Text Questions
- [ ] 3.9 Implement `AssessmentQuestion` abstract entity (`TABLE_PER_CLASS`), `McqQuestion`, and `TextQuestion` entities
- [ ] 3.10 Implement `QuestionService` with MCQ validation (at least one correct answer required) and text question creation
- [ ] 3.11 Wire `QuestionController` for MCQ and text question creation
- [ ] 3.12 Write tests including validation boundary cases (zero correct answers rejected)

### Slice: Doc and Group Questions
- [ ] 3.13 Implement `DocQuestion` and `QuestionGroup` entities
- [ ] 3.14 Implement `QuestionService` methods for doc and group question creation
- [ ] 3.15 Wire `QuestionController` for doc and group question creation
- [ ] 3.16 Write tests for doc and group question persistence

### Slice: Angular Question Bank UI
- [ ] 3.17 Implement `QuestionBankListComponent` and `QuestionBankDetailComponent`
- [ ] 3.18 Implement dynamic question creation form (type selector drives which fields render)
- [ ] 3.19 Implement MCQ option builder with correct-answer toggle (single vs multi)
- [ ] 3.20 Wire Angular `QuestionBankService` to real API; remove stubs

---

## 4. Phase 3: Assessment Generation & Distribution
> Epic: Phase 3 — Assessment Generation | Slice 0 (Contract) must merge before other slices begin. Slices A–D run in parallel.

### Slice 0: Backend API Contracts *(merge first — unblocks all other slices)*
- [ ] 4.1 Create stub `AssessmentController` with empty response for `POST /api/assessments`
- [ ] 4.2 Define `AssessmentRequest`, `AssessmentResponse` Java Records
- [ ] 4.3 Create Angular `AssessmentService` with stub `generateAssessment()` method

### Slice: Assessment Generation Logic
- [ ] 4.4 Implement `Assessment` entity with `status` enum (`PENDING`, `IN_PROGRESS`, `SUBMITTED`, `MARKED`), `invitation_token`, `time_limit_minutes`
- [ ] 4.5 Implement no-repeat question rule in `AssessmentService` (query prior submitted assessments for candidate)
- [ ] 4.6 Implement doc question limit enforcement (max 1 per assessment) in `AssessmentService`
- [ ] 4.7 Write tests for no-repeat rule and doc question limit boundary cases

### Slice: Invitation Token & Email
- [ ] 4.8 Implement short-lived signed JWT generation for invitation tokens (separate from login JWT)
- [ ] 4.9 Implement email service for sending candidate invitation email with token link
- [ ] 4.10 Ensure email failure does not roll back assessment creation (async or try-catch with logging)
- [ ] 4.11 Write tests for token generation and email service mock

### Slice: Angular Assessment Generation UI
- [ ] 4.12 Implement `AssessmentGenerateComponent`: candidate selector, question bank browser, question picker
- [ ] 4.13 Add no-repeat warning indicator on questions already seen by the selected candidate
- [ ] 4.14 Display generated assessment details and invitation link after creation
- [ ] 4.15 Wire Angular `AssessmentService` to real API; remove stubs

---

## 5. Phase 4: Assessment Experience & Submission
> Epic: Phase 4 — Assessment Experience | Slice 0 (Contract) must merge before other slices begin. Slices A–D run in parallel.

### Slice 0: Backend API Contracts *(merge first — unblocks all other slices)*
- [ ] 5.1 Create stub for `GET /api/assessments/access/{token}` returning hardcoded assessment structure
- [ ] 5.2 Create stub for `PUT /api/assessments/{id}/responses/{questionId}` returning HTTP 200
- [ ] 5.3 Create stub for `POST /api/assessments/{id}/submit` returning HTTP 200
- [ ] 5.4 Create Angular `CandidateAssessmentService` with stub methods

### Slice: Token-Gated Access & Status Machine
- [ ] 5.5 Implement `GET /api/assessments/access/{token}`: validate invitation token, transition `PENDING → IN_PROGRESS`, return questions and remaining time
- [ ] 5.6 Enforce one-submission-only rule in `AssessmentService` (check status before accepting submission)
- [ ] 5.7 Write tests for status transitions and duplicate submission rejection

### Slice: Response Persistence & Auto-Save
- [ ] 5.8 Implement `Response` abstract entity (`TABLE_PER_CLASS`) and concrete types: `McqResponse`, `TextResponse`, `DocResponse`
- [ ] 5.9 Implement `PUT /api/assessments/{id}/responses/{questionId}` for saving/updating each response type
- [ ] 5.10 Enforce that responses cannot be saved on a `SUBMITTED` assessment
- [ ] 5.11 Write tests for each response type persistence

### Slice: Server-Side Timer & MCQ Auto-Marking
- [ ] 5.12 Implement server-side time check in the submission endpoint (`now() > start_time + time_limit_minutes`)
- [ ] 5.13 Implement auto-submission logic: set `auto_submitted = true`, persist current responses, transition to `SUBMITTED`
- [ ] 5.14 Implement MCQ auto-marking on submission: compare `selected_answers` to `correct_answers` for all MCQ responses
- [ ] 5.15 Write tests for time-expired auto-submit and MCQ marking (single-answer, multi-answer, partial-answer)

### Slice: Angular Assessment Taking UI
- [ ] 5.16 Implement token-entry / invitation link landing page
- [ ] 5.17 Implement `AssessmentTakingComponent` with question renderer (MCQ, text, doc upload) and server-initialised countdown timer
- [ ] 5.18 Implement auto-save on answer change (debounced PUT per response)
- [ ] 5.19 Implement submit flow with confirmation dialog and post-submit confirmation screen
- [ ] 5.20 Wire Angular `CandidateAssessmentService` to real API; remove stubs

---

## 6. Phase 5: Manual Marking & Results
> Epic: Phase 5 — Marking & Results | Slice 0 (Contract) must merge before other slices begin. Slices A–D run in parallel.

### Slice 0: Backend API Contracts *(merge first — unblocks all other slices)*
- [ ] 6.1 Create stub for `GET /api/assessments?status=SUBMITTED` returning hardcoded list
- [ ] 6.2 Create stub for `GET /api/assessments/{id}/responses` returning hardcoded responses
- [ ] 6.3 Create stub for `PATCH /api/assessments/{id}/responses/{responseId}` returning HTTP 200
- [ ] 6.4 Create stub for `POST /api/assessments/{id}/finalise` returning HTTP 200
- [ ] 6.5 Create stub for `GET /api/assessments/{id}/results` returning hardcoded results
- [ ] 6.6 Create Angular `MarkingService` and `ResultsService` with stub methods

### Slice: Marking Queue & Response Review
- [ ] 6.7 Implement `GET /api/assessments?status=SUBMITTED` with MARKER role restriction and pagination
- [ ] 6.8 Implement `GET /api/assessments/{id}/responses` returning all responses with MCQ marks and text/doc responses for review
- [ ] 6.9 Implement `PATCH /api/assessments/{id}/responses/{responseId}` for persisting Marker feedback
- [ ] 6.10 Write tests for marking queue filtering and feedback persistence

### Slice: Finalisation & Status Transition
- [ ] 6.11 Implement `POST /api/assessments/{id}/finalise`: validate status is `SUBMITTED`, transition to `MARKED`
- [ ] 6.12 Enforce idempotency: reject finalisation if already `MARKED` or not yet `SUBMITTED`
- [ ] 6.13 Write tests for finalisation boundary cases

### Slice: Result Email & Candidate Results View
- [ ] 6.14 Implement result email triggered by finalisation: sends Marker feedback per question to candidate (no scores)
- [ ] 6.15 Implement `GET /api/assessments/{id}/results` returning feedback per question for the authenticated candidate
- [ ] 6.16 Enforce that results are only accessible after `MARKED` status
- [ ] 6.17 Write tests for result email content (assert no score data) and results endpoint access control

### Slice: Angular Marking UI
- [ ] 6.18 Implement `MarkingQueueComponent` with list of submitted assessments
- [ ] 6.19 Implement `MarkingDetailComponent` with per-question response view, MCQ result display, and feedback text inputs for text/doc responses
- [ ] 6.20 Implement finalise flow with confirmation dialog
- [ ] 6.21 Implement `CandidateResultsComponent` for candidates to view their marked assessment feedback
- [ ] 6.22 Wire Angular `MarkingService` and `ResultsService` to real API; remove stubs