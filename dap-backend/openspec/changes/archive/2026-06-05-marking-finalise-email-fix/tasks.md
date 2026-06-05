## 1. Backend — MCQ feedback exemption

- [x] 1.1 Update `FeedbackRepository.findQuestionsWithEmptyFeedback()` to join `mcq_question` and exclude rows where the question is an MCQ type (add `AND TYPE(f.question) != McqQuestion` to the JPQL query)
- [x] 1.2 Verify `AssessmentService.finalise()` still throws `ValidationException` when TEXT or DOC questions have empty feedback
- [x] 1.3 Verify `AssessmentService.finalise()` succeeds when only MCQ questions have empty/null feedback drafts

## 2. Backend — FinaliseRequest DTO and updated endpoint

- [x] 2.1 Create `FinaliseRequest` record in `dto/` with a single nullable `String overallFeedback` field (no `@NotBlank` — field is optional)
- [x] 2.2 Update `AssessmentController.finaliseMarking()` to accept `@RequestBody(required = false) FinaliseRequest request` and pass `overallFeedback` (defaulting to empty string if null) to `assessmentService.finalise()`
- [x] 2.3 Update `AssessmentService.finalise()` signature to accept `String overallFeedback` and thread it through to the email call

## 3. Backend — Email service refactor

- [x] 3.1 Change `EmailService.sendFeedback()` signature from `(String toEmail, String candidateName, Map<UUID, String> feedbackByQuestion)` to `(String toEmail, String candidateName, String overallFeedback)`
- [x] 3.2 Update `EmailServiceImpl.sendFeedback()` to match new signature and rewrite `buildFeedbackBody()` to render `overallFeedback` as a single paragraph instead of per-question lines
- [x] 3.3 Update the `AssessmentService.finalise()` call to `emailService.sendFeedback()` to pass the `overallFeedback` string

## 4. Backend — Tests

- [x] 4.1 Update any test mocking `emailService.sendFeedback()` to use the new `(String, String, String)` signature
- [x] 4.2 Add or update a unit/slice test for `AssessmentService.finalise()` covering: (a) MCQ-only empty feedback does not block finalise, (b) TEXT empty feedback blocks finalise, (c) `overallFeedback` is passed to the email call

## 5. Frontend — Pass overall feedback on finalise

- [x] 5.1 Update `MarkingService.finaliseMarking()` to accept an `overallFeedback: string` parameter and include it in the POST body: `{ overallFeedback }`
- [x] 5.2 Update `MarkingComponent.finalise()` to pass `this.overallFeedback()` to `markingService.finaliseMarking()`
