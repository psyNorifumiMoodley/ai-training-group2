## Why

The marking page currently renders all question types in a single flat list, hides useful MCQ context (full option set, correct answers), silently discards scores entered for Text/Doc/Group questions, and uses generic feedback templates that don't match the real outcomes markers communicate to candidates.

## What Changes

- Add tabbed navigation to the marking page: **All**, **MCQ**, **Text**, **Group**, **Doc** — only tabs with at least one question are shown.
- On MCQ cards, display **all** answer options the candidate was provided, with the correct answer(s) highlighted and the candidate's selection clearly marked.
- Fix the score entry flow so that scores entered on Text, Doc, and Group question cards are persisted to the backend and reflected in the score summary panel.
- Replace the three existing feedback templates ("Strong performance", "Good effort", "Needs improvement") with three new domain-appropriate templates: **"Set up interview"** (with a `[DATE/TIME]` placeholder), **"Additional assessment"**, and **"Unfortunately looking elsewhere"**.

## Capabilities

### New Capabilities

- `marking-question-tabs`: Tabbed question navigation on the marking page — filters the visible question cards by type; All tab shows every card.
- `mcq-full-options`: MCQ answer cards show all provided options, highlight correct answer(s) in green, and mark the candidate's selection distinctly from the outcome styling.
- `manual-score-entry`: Score inputs on Text, Doc, and Group cards are interactive; score changes are persisted to the backend and aggregated in the score summary panel.
- `marking-feedback-templates`: New pre-filled feedback templates — "Set up interview" (includes [DATE/TIME] placeholder), "Additional assessment", and "Unfortunately looking elsewhere".

### Modified Capabilities

## Impact

- **Frontend**: `marking.component.ts/html`, `feedback-item-editor.component.ts/html`, `score-input.component.ts`, `marking.model.ts`, `marking.service.ts`
- **Backend**: `ResponseReviewItem.java` (add `allOptions`, `correctAnswers` to MCQ payload), `MarkingService.java` (populate options, add score-update method), `ResponseRepository.java` (score update query), new `ScoreUpdateRequest` DTO
- **API**: New `PATCH /api/assessments/{assessmentId}/responses/{responseId}/score` endpoint
