## Context

The marking page (`MarkingComponent`) lists every `ResponseReviewItem` in a flat vertical list. Four question types (MCQ, TEXT, DOC, GROUP) coexist in the same scroll area, score entry for TEXT/DOC/GROUP is decorative (the `ScoreInputComponent` has no output and no backend call), MCQ cards show only the candidate's selected answers without context, and the feedback template set ("Strong performance", "Good effort", "Needs improvement") does not reflect the real outcomes a marker communicates to candidates.

Current data flow:
- `GET /api/assessments/{id}/responses/review` → `List<ResponseReviewItem>`
- `PATCH /api/assessments/{id}/responses/{responseId}/feedback` → persists feedback draft
- No endpoint exists to persist a score for a TEXT/DOC/GROUP response

## Goals / Non-Goals

**Goals:**
- Tab-filter the question list by type without re-fetching data (client-side filter only).
- Enrich MCQ cards to display every option the candidate was shown, distinguish correct answer(s), and mark the candidate's selection.
- Make the score input on TEXT/DOC/GROUP cards interactive; persist changes via a new PATCH endpoint and aggregate them into the score summary panel.
- Replace the three legacy feedback templates with three new ones carrying pre-filled body text.

**Non-Goals:**
- Changing when or how the candidate result email is sent.
- Altering MCQ auto-marking logic (scores remain computed at submission time).
- Supporting score entry for GROUP child items individually (only the parent GROUP card gets a score input).
- Adding pagination or lazy loading to the question list.

## Decisions

### D1 — Tab filtering is purely client-side

The full `ResponseReviewItem` list is already loaded on page enter. Filtering by type is a computed signal — no extra HTTP call. Tabs that would be empty are hidden.

*Alternatives considered:* Fetching per-type from the server would add round trips for no benefit; the list is bounded by one assessment's questions.

### D2 — MCQ options added to `McqAnswerPayload`

`McqAnswerPayload` (backend DTO + frontend model) gains two new fields: `allOptions: List<String>` and `correctAnswers: List<String>`. The backend populates them from the `McqQuestion` entity when building the `ResponseReviewItem`. No new endpoint required — the review endpoint already returns the full payload.

*Alternatives considered:* A separate endpoint for MCQ question metadata would add a second fetch; embedding in the existing DTO is simpler and keeps the contract in one place.

### D3 — Score persistence via new PATCH endpoint

A new `PATCH /api/assessments/{assessmentId}/responses/{responseId}/score` endpoint accepts `{ score: int }`. The `MarkingService` validates the score is within [0, marks] and writes it to the `Response` entity. The `ScoreInputComponent` gains a `scoreChanged` output; `FeedbackItemEditorComponent` emits a `scoreChanged` event; `MarkingComponent` calls the service and reloads the `reviewItems` signal to refresh computed scores.

*Alternatives considered:* Bundling score into the feedback PATCH would conflate two concerns and make the feedback endpoint ambiguous. A dedicated endpoint keeps each concern separate.

### D4 — Score summary panel extended to include manual types

`MarkingComponent` adds `manualScore` and `manualMaxScore` computed signals that sum non-MCQ responses where `score != null`. The panel row for "Text score (pending)" is replaced by a dynamic row showing the running total once any manual score is entered; it remains "— pts" until the first score is saved.

### D5 — Feedback templates are compile-time constants

The three new templates are defined as a `const` object in `marking.component.ts`, same pattern as today. No database storage, no admin configuration UI.

## Risks / Trade-offs

- **Stale score in view after patch** → After a score PATCH, the component re-fetches the affected `reviewItem` from the signal and updates it in place using `reviewItems.update(...)`. A full re-fetch is not required.
- **ScoreInputComponent becomes interactive** → Existing usages of `ScoreInputComponent` outside of marking (if any) must be verified to be unaffected by the new optional `scoreChanged` output.
- **MCQ `allOptions` order** → Options are stored as a `List<String>` (JSON array) in the DB; display order matches storage order. No explicit sort is applied.
