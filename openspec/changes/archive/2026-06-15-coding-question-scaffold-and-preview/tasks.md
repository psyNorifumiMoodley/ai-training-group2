## 1. Backend — DTO and QuestionService preview method

- [x] 1.1 Create `dap-backend/src/main/java/com/psybergate/dap/dto/CodingExecuteRequest.java` — Java record with `String code` field and `@NotBlank` validation
- [x] 1.2 Add `executeQuestion(UUID questionId, String code)` method to `QuestionService` — loads `CodingQuestion` by ID (throws `NoSuchElementException` if not found, `ValidationException` if not a coding question), delegates to `CodeExecutionService.execute(question, code)`, returns `CodeExecuteResponse`

## 2. Backend — QuestionController endpoint

- [x] 2.1 Add `POST /api/questions/{id}/execute` endpoint to `QuestionController` — accepts `@Valid @RequestBody CodingExecuteRequest`, delegates to `questionService.executeQuestion(id, request.code())`, returns `ResponseEntity<CodeExecuteResponse>` with HTTP 200; restrict to ADMIN + MARKER in Spring Security config

## 3. Backend — Tests

- [x] 3.1 Add unit test in `QuestionServiceTest` (or create if not exists) verifying `executeQuestion` delegates to `CodeExecutionService` and returns its result
- [x] 3.2 Add unit test verifying `executeQuestion` throws `NoSuchElementException` for unknown ID
- [x] 3.3 Add unit test verifying `executeQuestion` throws `ValidationException` when question is not a `CodingQuestion`

## 4. Frontend — QuestionService.executeQuestion()

- [x] 4.1 Add `executeQuestion(questionId: string, code: string): Observable<CodeExecuteResponse>` to `dap-frontend/src/app/core/services/question.service.ts` — `POST /api/questions/${questionId}/execute` with body `{ code }`
- [x] 4.2 Ensure `CodeExecuteResponse` is imported/re-exported from the correct model file so `QuestionService` and `CodingQuestionPreviewComponent` can use it (check `assessment-session.model.ts` vs `question.model.ts`)

## 5. Frontend — CodingQuestionPreviewComponent

- [x] 5.1 Create `dap-frontend/src/app/features/question-management/components/coding-question-preview/coding-question-preview.component.ts` — standalone `OnPush` component; inputs: `question = input.required<CodingQuestionResponse>()`; inject `QuestionService`; signals: `code = signal('')`, `running = signal(false)`, `results = signal<TestCaseResult[] | null>(null)`; constructor initialises `code` to the language scaffold via the scaffold constant map
- [x] 5.2 Add scaffold constant map in the same file (or a shared util): `CODING_SCAFFOLDS: Record<CodingQuestionLanguage, string>` with entries for JAVA, PYTHON, CSHARP
- [x] 5.3 Implement `runCode()`: guard if `running()`, set `running(true)`, call `questionService.executeQuestion(question().id, code())`, on success set `results` and `running(false)`, on error set `running(false)`
- [x] 5.4 Create `dap-frontend/src/app/features/question-management/components/coding-question-preview/coding-question-preview.component.html` — language badge, monospace `<textarea>` bound to `code()` with `(input)` handler updating the signal, "Run" button calling `runCode()` (disabled + spinner when `running()`), results panel (`@if (results())`) with summary badge (all-passed green / partial amber) and per-test-case table matching `coding-answer.component.html`

## 6. Frontend — QuestionListComponent wiring

- [x] 6.1 Import `CodingQuestionPreviewComponent` in `QuestionListComponent`
- [x] 6.2 Add `testingQuestionId = signal<string | null>(null)` to `QuestionListComponent` to track which question's preview panel is open (separate from `expandedQuestionId` used for detail expand)
- [x] 6.3 Add `togglePreview(id: string)` method: toggles `testingQuestionId` between the given ID and null
- [x] 6.4 Update `question-list.component.html` — add "Test question" button on coding question cards (next to Edit/Delete); render `<dap-coding-question-preview [question]="q" />` inline below the card when `testingQuestionId() === q.id`

## 7. Frontend — CodingAnswerComponent scaffold initialisation

- [x] 7.1 Add `CODING_SCAFFOLDS` constant map to `coding-answer.component.ts` (or import from shared util if created in task 5.2) mapping each `CodingQuestionLanguage` to its scaffold string
- [x] 7.2 Update the `effect()` in `CodingAnswerComponent`: if `saved?.code` is truthy use saved code; else use `CODING_SCAFFOLDS[question().language]` as the initial value for the `code` signal

## 8. Verification

- [x] 8.1 Run `mvn verify` — all backend tests pass
- [x] 8.2 Run `ng build` — no TypeScript compilation errors
- [ ] 8.3 Manual smoke test: open a coding question as a candidate, verify the editor is pre-filled with the correct scaffold; open the question management page as ADMIN/MARKER, click "Test question" on a coding question, enter code, click Run, verify results panel populates
