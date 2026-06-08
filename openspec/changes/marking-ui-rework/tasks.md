## 1. Backend — MCQ Option Data

- [ ] 1.1 Add `allOptions` and `correctAnswers` fields to `McqAnswerPayload` DTO record
- [ ] 1.2 Update `ResponseReviewItem` record if needed to accommodate the richer MCQ payload (verify type is `Object answer` — no change required if already generic)
- [ ] 1.3 In `MarkingService.mapToReviewItem`, cast the MCQ question to `McqQuestion` and populate `allOptions` and `correctAnswers` when building the MCQ `ResponseReviewItem`

## 2. Backend — Manual Score Endpoint

- [ ] 2.1 Create `ScoreUpdateRequest` DTO record with a single `Integer score` field and `@NotNull` / `@Min(0)` validation
- [ ] 2.2 Add `updateResponseScore(UUID assessmentId, UUID responseId, ScoreUpdateRequest request)` method to `MarkingService` — validates score ≤ question marks, rejects MCQ responses, persists score
- [ ] 2.3 Add `PATCH /api/assessments/{assessmentId}/responses/{responseId}/score` endpoint to the marking controller, delegating to `MarkingService.updateResponseScore`
- [ ] 2.4 Add unit test to `MarkingServiceTest` covering: valid score accepted, score out of range rejected, MCQ response rejected

## 3. Frontend — Model & Service Updates

- [ ] 3.1 Update `McqAnswerPayload` interface in `marking.model.ts` to add `allOptions: string[]` and `correctAnswers: string[]`
- [ ] 3.2 Add `updateResponseScore(assessmentId: string, responseId: string, score: number): Observable<void>` to `MarkingService` (Angular), calling the new PATCH endpoint

## 4. Frontend — ScoreInput Component

- [ ] 4.1 Add `scoreChanged` output to `ScoreInputComponent` emitting the new numeric value on blur/Enter
- [ ] 4.2 Wire the `(input)` and `(blur)/(keydown.enter)` events in the `ScoreInputComponent` template to emit `scoreChanged`

## 5. Frontend — FeedbackItemEditor — Score Wiring

- [ ] 5.1 Add `scoreChanged` output to `FeedbackItemEditorComponent` emitting `{ responseId: string; score: number }`
- [ ] 5.2 Bind `(scoreChanged)` on the `<dap-score-input>` in `feedback-item-editor.component.html` and bubble it up via the new output

## 6. Frontend — FeedbackItemEditor — MCQ Full Options Display

- [ ] 6.1 Add `mcqAllOptions()` and `mcqCorrectAnswers()` helper methods to `FeedbackItemEditorComponent` extracting from the `McqAnswerPayload`
- [ ] 6.2 Rework the MCQ section in `feedback-item-editor.component.html` to iterate over `mcqAllOptions()` instead of `mcqAnswers()`, applying green + checkmark styling when the option is in `mcqCorrectAnswers()`, and a candidate-selection indicator when the option is in `mcqAnswers()`

## 7. Frontend — Marking Component — Score Aggregation

- [ ] 7.1 Add `manualScore` computed signal summing `score ?? 0` for non-MCQ items where `score !== null`
- [ ] 7.2 Add `manualMaxScore` computed signal summing `marks` for all non-MCQ items
- [ ] 7.3 Add `hasAnyManualScore` computed signal (true if any non-MCQ item has `score !== null`)
- [ ] 7.4 Handle `scoreChanged` event from `FeedbackItemEditorComponent` in `MarkingComponent`: call `MarkingService.updateResponseScore`, then update the matching item's `score` in `reviewItems` signal in-place
- [ ] 7.5 Update the score summary panel in `marking.component.html` to show `manualScore()/manualMaxScore() pts` when `hasAnyManualScore()`, otherwise "— pts"

## 8. Frontend — Marking Component — Tabs

- [ ] 8.1 Add `activeTab` signal (`'ALL' | 'MCQ' | 'TEXT' | 'GROUP' | 'DOC'`) defaulting to `'ALL'`
- [ ] 8.2 Add `availableTabs` computed signal returning the distinct `questionType` values present in `reviewItems()`, always prepending `'ALL'`
- [ ] 8.3 Add `filteredItems` computed signal returning `reviewItems()` filtered by `activeTab()` (all items when `'ALL'`)
- [ ] 8.4 Replace the `@for` loop target in `marking.component.html` from `reviewItems()` to `filteredItems()`
- [ ] 8.5 Render the tab bar in `marking.component.html` above the question card list, iterating `availableTabs()`, with active-tab styling applied to the matching tab

## 9. Frontend — Marking Component — Feedback Templates

- [ ] 9.1 Replace `FEEDBACK_TEMPLATES` constant in `marking.component.ts` with the three new templates: `interview`, `additional`, `elsewhere`
- [ ] 9.2 Update `marking.component.html` to render three buttons — "Set up interview", "Additional assessment", "Unfortunately looking elsewhere" — each calling `setFeedbackTemplate` with the corresponding key
