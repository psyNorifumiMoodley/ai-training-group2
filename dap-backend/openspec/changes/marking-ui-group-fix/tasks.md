## 1. Backend — Filter child responses from flat list

- [ ] 1.1 Add `ResponseRepository.findTopLevelByAssessmentId(UUID assessmentId)` using JPQL that excludes responses referenced as children in `question_group_response_children` (use `r.id NOT IN (SELECT c.id FROM Response c JOIN QuestionGroupResponse g ON c MEMBER OF g.childResponses WHERE g.assessment.id = :assessmentId)`)
- [ ] 1.2 Update `MarkingService.getResponsesForReview()` to call `findTopLevelByAssessmentId` instead of `findByAssessmentId`

## 2. Backend — Nest child items in GROUP review item

- [ ] 2.1 Add `List<ResponseReviewItem> childItems` field to `ResponseReviewItem` record (nullable — null for non-GROUP types)
- [ ] 2.2 Update `MarkingService.mapToReviewItem()` GROUP branch: for a `QuestionGroupResponse`, map each child `Response` to a nested `ResponseReviewItem` and set it as `childItems` on the parent item
- [ ] 2.3 Ensure the nested child items carry `questionBody`, `questionType = "TEXT"`, and the text `answer` payload; `feedbackDraft` and `correct` should be null for children

## 3. Backend — Tests

- [ ] 3.1 Add a unit or integration test verifying that `getResponsesForReview()` for an assessment with a group question returns the correct count (group counts as 1, not 1 + N children)
- [ ] 3.2 Add a test verifying that the returned GROUP item has a populated `childItems` list with the correct question bodies and answers

## 4. Frontend — Model and rendering

- [ ] 4.1 Update `ResponseReviewItem` interface in `marking.model.ts` — add `childItems?: ResponseReviewItem[]` and add `'GROUP'` to the `questionType` union type
- [ ] 4.2 Update `FeedbackItemEditorComponent` to handle `questionType === 'GROUP'`: render the group question body, then loop over `childItems` showing each child's `questionBody` and text answer as a read-only row
- [ ] 4.3 Update `tagVariant()` in `FeedbackItemEditorComponent` to return a valid variant for `'GROUP'` (use `'text'` as fallback or add a `'group'` variant if the `TagComponent` supports it)
- [ ] 4.4 Polish card layout in `FeedbackItemEditorComponent`: improve spacing between question stem and answer block, add a stronger visual separator before the feedback textarea, ensure active card border is more prominent
