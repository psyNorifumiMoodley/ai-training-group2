# Testing Feedback — DAP Backend

**Date:** 2026-06-15
**Estimated overall coverage:** ~70% (68–75% when measured with JaCoCo)
**Test count:** 35 test classes, ~130 individual test methods

Run actual coverage with:
```
mvn test -pl dap-backend jacoco:report
```

---

## How to use this document with OpenSpec

Each section below is a self-contained change unit. Use `/openspec-propose` with the section heading and description as your prompt input. Work through them in priority order (top = highest impact on mutation score and correctness confidence).

---

## Change 1 — Test `UserSettingsService` (0% coverage)

**Priority:** High

**Context:**
`UserSettingsService` (located at `dap-backend/src/main/java/com/psybergate/dap/service/UserSettingsService.java`) has four public methods and zero test coverage. With mutation testing, every mutant in this class will survive.

The methods are:
- `getProfile(AppUser principal)` — trivial mapping, low priority
- `updateProfile(AppUser principal, UpdateProfileRequest request)` — checks `existsByEmailAndIdNot`; throws `ConflictException` if the new email is already in use by another user
- `changePassword(AppUser principal, ChangePasswordRequest request)` — throws `ValidationException` for wrong current password; throws `ValidationException` when new password and confirm password don't match; encodes and saves on success
- `updateTheme(AppUser principal, UpdateThemeRequest request)` — sets theme preference and saves

**What to build:**
A new unit test class `UserSettingsServiceTest` in `dap-backend/src/test/java/com/psybergate/dap/service/` using `@ExtendWith(MockitoExtension.class)`, mocking `AppUserRepository` and `PasswordEncoder`.

**Acceptance criteria:**
- `updateProfile` — happy path saves updated name and email and returns a `UserProfileResponse` with the new values
- `updateProfile` — throws `ConflictException` when `existsByEmailAndIdNot` returns `true`
- `updateProfile` — does not throw when updating email to the same value as the current user's own email (`existsByEmailAndIdNot` returns `false` for same ID)
- `changePassword` — throws `ValidationException` when current password does not match (wrong `passwordEncoder.matches`)
- `changePassword` — throws `ValidationException` when `newPassword` and `confirmPassword` differ
- `changePassword` — on success, calls `passwordEncoder.encode(newPassword)` and saves the updated hash
- `updateTheme` — sets the theme and returns a `UserProfileResponse` reflecting the new theme

---

## Change 2 — Expand `GlobalExceptionHandlerTest` to cover all 7 handlers

**Priority:** High

**Context:**
`GlobalExceptionHandler` (`dap-backend/src/main/java/com/psybergate/dap/config/GlobalExceptionHandler.java`) maps 7 custom exception types to HTTP responses. The existing `GlobalExceptionHandlerTest` only exercises 2 of them: `MethodArgumentNotValidException → 400` and `NoSuchElementException → 404`.

The five untested handlers are:
| Exception | Expected status | Expected error string |
|---|---|---|
| `UnauthorizedException` | 401 | `"Unauthorized"` |
| `ConflictException` | 409 | `"Conflict"` |
| `ValidationException` (domain) | 400 | `"Bad Request"` |
| `UnprocessableException` | 422 | `"Unprocessable Entity"` |
| `AccessDeniedException` | 403 | `"Forbidden"` |
| `Exception` (catch-all) | 500 | `"Internal Server Error"` |

Note: `ValidationException` (domain, `com.psybergate.dap.domain.ValidationException`) is a *different* class from `MethodArgumentNotValidException`. Both map to 400 but via separate handler methods.

**What to build:**
Add test methods to the existing `GlobalExceptionHandlerTest`. Stub the `AuthService` mock to throw each exception type when `authenticate(any())` is called, then perform a valid `POST /api/auth/login` request and assert the HTTP status and JSON error body shape.

**Acceptance criteria:**
Each added test must assert:
- The correct HTTP status code
- `$.status` matches the numeric code
- `$.error` matches the string in the table above
- `$.message` is not blank (except for `AccessDeniedException`, which returns the fixed string `"Access Denied"`)
- `$.timestamp` is present and not blank

---

## Change 3 — Add unit tests for `FeedbackService.getCandidateFeedback`

**Priority:** High

**Context:**
`FeedbackService.getCandidateFeedback` (`dap-backend/src/main/java/com/psybergate/dap/service/FeedbackService.java`, lines 95–113) has two access-control paths that are only exercised indirectly through the endpoint integration test (`CandidateFeedbackEndpointTest`). No unit test covers:
- The ownership check: a candidate who does not own the assessment gets `AccessDeniedException`
- The status check: an assessment that is not yet `MARKED` gets `AccessDeniedException`

`FeedbackService` also has no test for `getOrCreateDraft` directly — it is only exercised through `MarkingServiceTest`, which mocks it away.

**What to build:**
A new unit test class `FeedbackServiceTest` in `dap-backend/src/test/java/com/psybergate/dap/service/` using `@ExtendWith(MockitoExtension.class)`, mocking `FeedbackRepository`, `AssessmentRepository`, `AssessmentQuestionRepository`, and `ResponseRepository`.

**Acceptance criteria:**

`getCandidateFeedback`:
- Happy path: assessment is `MARKED` and `requestingCandidateId` matches the candidate — returns a list of `FeedbackItem` built from the saved feedback records
- Throws `AccessDeniedException` when `requestingCandidateId` does not match `assessment.getCandidate().getId()`
- Throws `AccessDeniedException` when assessment status is `SUBMITTED` (not yet `MARKED`)
- Throws `AccessDeniedException` when assessment status is `IN_PROGRESS`
- Throws `NoSuchElementException` when the assessment ID is not found

`getOrCreateDraft`:
- Returns the existing `Feedback` when one already exists for the `(assessmentId, questionId)` pair
- Creates and saves a new `Feedback` with an empty draft when none exists and question is not MCQ
- Creates a new `Feedback` with draft `"Correct"` when the question is MCQ and the response is marked correct
- Creates a new `Feedback` with draft `"Incorrect — please review this topic"` when the question is MCQ and the response is marked incorrect
- Creates a new `Feedback` with empty draft when the question is MCQ but no response exists yet

---

## Change 4 — Add missing `ResponseService` coverage for Text and Doc response types

**Priority:** Medium

**Context:**
`ResponseServiceTest` covers MCQ and GroupResponse saving but leaves `TextResponse` and `DocResponse` paths untested. The `autoMarkMcqResponses` method also has no test for the case where non-MCQ responses appear in the result set (they should be silently skipped).

**What to build:**
Add test methods to the existing `ResponseServiceTest`.

**Acceptance criteria:**

`saveResponse` — new tests:
- Saving a `TextResponseRequest` on an `IN_PROGRESS` assessment creates a new `TextResponse` with the correct answer field
- Saving a `TextResponseRequest` on an assessment that already has a `TextResponse` for that question updates the existing response (upsert)
- Saving a `DocResponseRequest` creates a `DocResponse` (verify via `ArgumentCaptor` that the saved instance is `instanceof DocResponse`)
- `saveResponse` throws `NoSuchElementException` when the question ID is not found in `assessmentQuestionRepository`

`autoMarkMcqResponses` — new tests:
- When the assessment's response list contains a `TextResponse` (non-MCQ), it is skipped and `responseRepository.save` is only called for the MCQ responses
- When the candidate submitted no answers (empty `selectedAnswers`), the MCQ response is marked incorrect

---

## Change 5 — Test `DashboardService` (0% coverage)

**Priority:** Medium

**Context:**
`DashboardService.getStats` (`dap-backend/src/main/java/com/psybergate/dap/service/DashboardService.java`) computes `weekStart` and `todayStart` timestamps and delegates eight repository calls to assemble the `DashboardStatsResponse`. The date-boundary calculation using `TemporalAdjusters.previousOrSame(MONDAY)` is non-trivial and mutation-sensitive (off-by-one on comparison operators would produce surviving mutants).

**What to build:**
A new unit test class `DashboardServiceTest` in `dap-backend/src/test/java/com/psybergate/dap/service/` using `@ExtendWith(MockitoExtension.class)`, mocking `CandidateRepository` and `AssessmentRepository`.

**Acceptance criteria:**
- `getStats` calls `candidateRepository.count()`, `candidateRepository.countByUserCreatedAtAfter(weekStart)`, and all four `assessmentRepository.countByStatus(...)` invocations — verify with `verify()`
- The returned `DashboardStatsResponse` maps each repository result to the correct response field (e.g., `totalCandidates`, `pendingCount`, `markedToday`)
- `weekStart` passed to `countByUserCreatedAtAfter` is a Monday at midnight UTC — verify using `ArgumentCaptor<Instant>` and asserting `DayOfWeek` of the captured instant
- `todayStart` passed to `countByStatusAndUpdatedAtAfter` is today at midnight UTC — verify via `ArgumentCaptor`

---

## Change 6 — Fix weak and redundant tests

**Priority:** Low–Medium

**Context:**
Several existing tests have structural issues that will reduce mutation score without indicating real bugs.

### 6a — Strengthen `generate_randomMode_seenQuestionLastYear_included`

**File:** `AssessmentServiceTest.java`

The test is named to verify that a last-year question can be included, but the assertion only checks that 5 MCQ were picked (`assertThat(questions.stream().filter(q -> q instanceof McqQuestion).count()).isEqualTo(5)`). This assertion is identical to other tests and proves nothing specific about last-year inclusion.

**Fix:** Either (a) assert that `lastYearMcq` is contained in the selected questions (requires deterministic pool size so it must be selected), or (b) rename the test to `generate_randomMode_unseenQuestionsAvailable_fiveMcqSelected` to accurately describe what it asserts.

### 6b — Consolidate duplicate registration tests in `CandidateServiceTest`

**File:** `CandidateServiceTest.java`

Three tests (`register_newEmail_savesUserAndCandidateAndReturnsResponse`, `register_passwordHashIsEncoded_notRawValue`, `register_candidateRoleIsSet`) perform identical mock setup for the same method call and differ only in which field they assert. Under mutation testing this means the same mutations are attempted three times, wasting test time without adding coverage.

**Fix:** Merge the three happy-path tests into a single `register_validRequest_savesEncodedUserWithCandidateRole` test with multiple assertions, or convert them to a `@ParameterizedTest` if your team's style requires separate test methods per assertion.

### 6c — Merge `McqQuestionTest` into `QuestionServiceTest`

**Files:** `McqQuestionTest.java`, `QuestionServiceTest.java`

Both test `QuestionService` with identical `@Mock` and `@InjectMocks` setups. `McqQuestionTest` covers `createMcq` validation paths; `QuestionServiceTest` covers happy paths for all question types. There is no meaningful organizational reason for the split.

**Fix:** Move the three validation tests from `McqQuestionTest` into `QuestionServiceTest` and delete `McqQuestionTest`.

### 6d — Add ADMIN positive path to `RbacPhase1Test`

**File:** `RbacPhase1Test.java`

`RbacPhase1Test` verifies the forbidden cases for `POST /api/candidates` and `POST /api/markers` but never asserts the allowed case (ADMIN role). If the security config accidentally blocked ADMIN too, these tests would still pass.

**Fix:** Add an `adminToken` (using the same pattern as `markerToken` with email `"admin@test.com"` resolving to `Role.ADMIN` via the existing stub) and add:
- `registerCandidate_asAdmin_returns2xx`
- `registerMarker_asAdmin_returns2xx`

### 6e — Add happy path for `MarkingService.updateResponseFeedback`

**File:** `MarkingServiceTest.java`

Only two error-path tests exist for `updateResponseFeedback`. No test verifies the success path: a valid response belonging to the correct assessment has its feedback updated and the repository `save` is called.

**Fix:** Add `updateResponseFeedback_validResponse_savesFeedback` asserting that `feedbackService.updateFeedback(assessmentId, questionId, request)` is called with the correct arguments.

---

## Coverage Baseline

| Component | Estimated Coverage | Primary Gap |
|---|---|---|
| `AssessmentService` | ~88% | `listAssessments`, `getAssessment` not unit-tested |
| `AuthService` | ~90% | — |
| `CandidateService` | ~75% | `listCandidates`, `getCandidate` not unit-tested |
| `MarkerService` | ~60% | list/get methods not tested |
| `QuestionService` | ~70% | `McqPlusQuestion` create not tested; update/delete not tested |
| `QuestionBankService` | ~90% | — |
| `ResponseService` | ~65% | Text/Doc types missing |
| `MarkingService` | ~75% | `updateResponseFeedback` happy path missing |
| `FeedbackService` | ~45% | `getCandidateFeedback` and `getOrCreateDraft` not directly tested |
| `EmailService` | ~90% | — |
| `UserSettingsService` | **0%** | Entire class uncovered |
| `DashboardService` | **0%** | Entire class uncovered |
| `GlobalExceptionHandler` | ~30% | 5 of 7 handlers untested |
| Controller layer (overall) | ~70% | `DashboardController`, `UserSettingsController` have no tests |

**Estimated overall: ~70%**
