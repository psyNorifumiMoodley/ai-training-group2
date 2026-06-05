## Why

Two defects in the marking finalise flow were discovered after end-to-end testing: the backend blocks finalisation when MCQ questions have no manual feedback (they are auto-marked and should never require it), and the feedback email sent to the candidate dumps raw question UUIDs with per-question drafts instead of the overall feedback text the marker composed in the UI.

## What Changes

- **Backend — exempt MCQ questions from the finalise feedback check**: `AssessmentService.finalise()` currently rejects finalisation when any question has empty feedback. This check must be scoped to TEXT and DOC question types only; MCQ feedback is set automatically at submission time and must not gate finalisation.
- **Backend — replace per-question email body with overall feedback text**: `POST /assessments/{id}/finalise` now accepts an `overallFeedback` string in the request body. `AssessmentService.finalise()` passes this string to `EmailService.sendFeedback()` instead of the per-question map. `EmailService.sendFeedback()` signature changes accordingly.
- **Frontend — pass overall feedback on finalise**: `MarkingComponent.finalise()` sends the `overallFeedback()` signal value in the request body when calling `POST /assessments/{id}/finalise`.

## Capabilities

### New Capabilities
- `marking-finalise`: Rules governing when a marker can finalise an assessment and what the resulting feedback email contains — MCQ exemption from feedback gate, overall-feedback-only email content.

### Modified Capabilities

## Impact

- **Backend**: `AssessmentService.finalise()`, `FeedbackRepository.findQuestionsWithEmptyFeedback()` query (add MCQ type exclusion), `EmailService` interface + `EmailServiceImpl`, new `FinaliseRequest` DTO.
- **Frontend**: `MarkingService.finaliseMarking()` adds request body; `MarkingComponent.finalise()` passes `overallFeedback()`.
- **API**: `POST /api/assessments/{id}/finalise` gains an optional JSON body `{ "overallFeedback": "..." }`.
- **No DB changes** required.
