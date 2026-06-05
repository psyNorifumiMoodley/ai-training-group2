## Context

The marking flow has two defects found in end-to-end testing:

1. **MCQ feedback gate**: `AssessmentService.finalise()` calls `feedbackRepository.findQuestionsWithEmptyFeedback(assessmentId)`, which returns any question whose `Feedback.draft` is null or blank. MCQ feedback is set automatically by `FeedbackService.resolveAutoDraft()` ("Correct" / "Incorrect — please review this topic") at review-load time, but only when the candidate actually answered that question. If an MCQ was skipped, the `Feedback` record may not exist or have an empty draft, blocking finalisation.

2. **Email content**: `AssessmentService.finalise()` collects a `Map<UUID, String>` of per-question feedback and passes it to `EmailServiceImpl.buildFeedbackBody()`, which formats each entry as `Question [uuid]: text`. The marker UI provides an **Overall feedback** textarea (`overallFeedback` signal in `MarkingComponent`) intended for the candidate email, but its value is never sent to the backend.

## Goals / Non-Goals

**Goals:**
- Finalise succeeds as long as all TEXT and DOC questions have non-empty feedback; MCQ questions are exempt.
- The feedback email sent to the candidate contains only the marker-authored overall feedback text.
- The `overallFeedback` textarea value is carried in the finalise API call.

**Non-Goals:**
- Per-question feedback is not removed from the database — it remains stored and accessible for the marker review screen.
- No changes to how MCQ auto-marking works or when it runs.
- No changes to the candidate feedback view (`GET /api/assessments/{id}/feedback`).

## Decisions

### Decision 1 — Exclude MCQs in the repository query rather than the service

**Choice:** Add a `JOIN` to filter out `McqQuestion` rows inside `FeedbackRepository.findQuestionsWithEmptyFeedback()`.

**Rationale:** Keeps the service call-site clean. The repository already owns the query; adding a discriminator filter (`dtype != 'MCQ'` or a JOIN on the `mcq_question` table) is a single-line change. The alternative — filtering in the service after the query — fetches unnecessary data and pushes repository concerns into the service layer.

**Alternative considered:** Move the whole check to the service and iterate `allFeedback` in memory. Rejected: less efficient and violates layer boundaries.

### Decision 2 — Accept `overallFeedback` as an optional request body on the finalise endpoint

**Choice:** Introduce a new `FinaliseRequest` record with a single nullable `String overallFeedback` field. The endpoint signature becomes `POST /api/assessments/{id}/finalise` with `@RequestBody(required = false) FinaliseRequest request`.

**Rationale:** Making the body optional preserves backward compatibility — existing callers (tests, integrations) that send no body continue to work and receive a blank overall feedback in the email. The frontend explicitly sends the field.

**Alternative considered:** Add `overallFeedback` as a query parameter. Rejected: email content as a query param is semantically wrong and awkward for multiline text.

### Decision 3 — Replace `sendFeedback(Map<UUID,String>)` with `sendFeedback(String overallFeedback)`

**Choice:** Change the `EmailService` interface and `EmailServiceImpl` to accept a plain `String` rather than a map, and rebuild the email body to use that string directly.

**Rationale:** The per-question map is no longer needed in the email; eliminating it removes the UUID leak and simplifies the body builder. Any code currently calling `sendFeedback` only does so from `AssessmentService.finalise()`, so the blast radius of the signature change is contained.

## Risks / Trade-offs

- **Risk: Empty overall feedback sent in email** → If the marker does not type anything in the Overall Feedback field, the email body will have a blank feedback section. Mitigation: the frontend already shows a template-loading section to nudge markers; no validation rule added (keeping it optional is consistent with business rules).
- **Risk: Existing test breakage** → `EmailService` interface change will break any test that mocks `sendFeedback` with the old signature. Mitigation: update affected tests as part of this change.

## Migration Plan

No database changes. No deployment steps beyond a standard backend + frontend deploy. Rollback is a redeploy of the previous artifact.
