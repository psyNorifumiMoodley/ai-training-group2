## Why

The marker response review screen has two compounding problems: grouped questions are incorrectly split into separate flat items (a group with 2 follow-ups appears as 3 distinct review cards instead of 1), which inflates the question count and confuses markers; and the overall card layout lacks the visual polish and hierarchy expected of the rest of the application.

## What Changes

- **Backend — filter child responses from the flat review list**: `ResponseRepository.findByAssessmentId()` currently returns all `Response` rows for an assessment, including child responses that belong to a `QuestionGroupResponse`. Add a repository query that excludes responses which are referenced as children in `question_group_response_children`. `MarkingService.getResponsesForReview()` switches to this filtered query.
- **Backend — nest child items inside GROUP review item**: `ResponseReviewItem` gains an optional `childItems` list (non-null only for GROUP type). For each `QuestionGroupResponse`, the mapper includes each child response as a nested `ResponseReviewItem` within the parent.
- **Frontend — render GROUP cards with nested child rows**: `ResponseReviewItem` model adds `childItems?: ResponseReviewItem[]`. `FeedbackItemEditorComponent` renders a GROUP card that shows the group question body and, beneath it, each follow-up question + its text answer as a read-only nested row.
- **Frontend — card layout polish**: Improve visual hierarchy in `FeedbackItemEditorComponent` — cleaner section spacing, better typography contrast between question stem and answer, and a subtle left-accent for the active card state.

## Capabilities

### New Capabilities
- `marking-group-review`: How grouped questions are represented and displayed in the marker review screen — child response exclusion from flat list, nesting within a GROUP card.

### Modified Capabilities

## Impact

- **Backend**: `ResponseRepository` gains a new query method; `MarkingService.mapToReviewItem()` updated; `ResponseReviewItem` DTO gets `childItems` field.
- **Frontend**: `marking.model.ts` `ResponseReviewItem` interface updated; `FeedbackItemEditorComponent` template updated for GROUP rendering and card polish.
- **No DB or Liquibase changes** required.
- **No API path changes** — same `GET /api/assessments/{id}/responses` endpoint, richer response shape.
