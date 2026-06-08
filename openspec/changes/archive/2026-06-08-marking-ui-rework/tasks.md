## 1. Backend — MCQ Option Data

- [x] 1.1 Add `allOptions` and `correctAnswers` fields to `McqAnswerPayload` DTO record
- [x] 1.2 Update `ResponseReviewItem` record if needed to accommodate the richer MCQ payload (verify type is `Object answer` — no change required if already generic)
- [x] 1.3 In `MarkingService.mapToReviewItem`, cast the MCQ question to `McqQuestion` and populate `allOptions` and `correctAnswers` when building the MCQ `ResponseReviewItem`

## 2. Backend — Manual Score Endpoint

- [x] 2.1 Create `ScoreUpdateRequest` DTO record with a single `Integer score` field and `@NotNull` / `@Min(0)` validation
- [x] 2.2 Add `updateResponseScore(UUID assessmentId, UUID responseId, ScoreUpdateRequest request)` method to `MarkingService` — validates score ≤ question marks, rejects MCQ responses, persists score
- [x] 2.3 Add `PATCH /api/assessments/{assessmentId}/responses/{responseId}/score` endpoint to the marking controller, delegating to `MarkingService.updateResponseScore`
- [x] 2.4 Add unit test to `MarkingServiceTest` covering: valid score accepted, score out of range rejected, MCQ response rejected

## 3. Frontend — Model & Service Updates

- [x] 3.1 Update `McqAnswerPayload` interface in `marking.model.ts` to add `allOptions: string[]` and `correctAnswers: string[]`
- [x] 3.2 Add `updateResponseScore(assessmentId: string, responseId: string, score: number): Observable<void>` to `MarkingService` (Angular), calling the new PATCH endpoint

## 4. Frontend — ScoreInput Component

- [x] 4.1 Add `scoreChanged` output to `ScoreInputComponent` emitting the new numeric value on blur/Enter
- [x] 4.2 Wire the `(input)` and `(blur)/(keydown.enter)` events in the `ScoreInputComponent` template to emit `scoreChanged`

## 5. Frontend — FeedbackItemEditor — Score Wiring

- [x] 5.1 Add `scoreChanged` output to `FeedbackItemEditorComponent` emitting `{ responseId: string; score: number }`
- [x] 5.2 Bind `(scoreChanged)` on the `<dap-score-input>` in `feedback-item-editor.component.html` and bubble it up via the new output

## 6. Frontend — FeedbackItemEditor — MCQ Full Options Display

- [x] 6.1 Add `mcqAllOptions()` and `mcqCorrectAnswers()` helper methods to `FeedbackItemEditorComponent` extracting from the `McqAnswerPayload`
- [x] 6.2 Rework the MCQ section in `feedback-item-editor.component.html` to iterate over `mcqAllOptions()` instead of `mcqAnswers()`, applying green + checkmark styling when the option is in `mcqCorrectAnswers()`, and a candidate-selection indicator when the option is in `mcqAnswers()`

## 7. Frontend — Marking Component — Score Aggregation

- [x] 7.1 Add `manualScore` computed signal summing `score ?? 0` for non-MCQ items where `score !== null`
- [x] 7.2 Add `manualMaxScore` computed signal summing `marks` for all non-MCQ items
- [x] 7.3 Add `hasAnyManualScore` computed signal (true if any non-MCQ item has `score !== null`)
- [x] 7.4 Handle `scoreChanged` event from `FeedbackItemEditorComponent` in `MarkingComponent`: call `MarkingService.updateResponseScore`, then update the matching item's `score` in `reviewItems` signal in-place
- [x] 7.5 Update the score summary panel in `marking.component.html` to show `manualScore()/manualMaxScore() pts` when `hasAnyManualScore()`, otherwise "— pts"

## 8. Frontend — Marking Component — Tabs

- [x] 8.1 Add `activeTab` signal (`'ALL' | 'MCQ' | 'TEXT' | 'GROUP' | 'DOC'`) defaulting to `'ALL'`
- [x] 8.2 Add `availableTabs` computed signal returning the distinct `questionType` values present in `reviewItems()`, always prepending `'ALL'`
- [x] 8.3 Add `filteredItems` computed signal returning `reviewItems()` filtered by `activeTab()` (all items when `'ALL'`)
- [x] 8.4 Replace the `@for` loop target in `marking.component.html` from `reviewItems()` to `filteredItems()`
- [x] 8.5 Render the tab bar in `marking.component.html` above the question card list, iterating `availableTabs()`, with active-tab styling applied to the matching tab

## 9. Frontend — Marking Component — Feedback Templates

- [x] 9.1 Replace `FEEDBACK_TEMPLATES` constant in `marking.component.ts` with the three new templates: `interview`, `additional`, `elsewhere`
- [x] 9.2 Update `marking.component.html` to render three buttons — "Set up interview", "Additional assessment", "Unfortunately looking elsewhere" — each calling `setFeedbackTemplate` with the corresponding key
