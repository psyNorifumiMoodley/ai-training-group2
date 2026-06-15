## 1. Wire executeCode() in CandidateAssessmentService

- [x] 1.1 Replace the `executeCode()` stub body in `dap-frontend/src/app/core/services/candidate-assessment.service.ts` with: `return this.http.post<CodeExecuteResponse>(\`${this.base}/assessments/${assessmentId}/responses/${questionId}/execute\`, {});`

## 2. Create CodingAnswerComponent

- [x] 2.1 Create `dap-frontend/src/app/features/assessment/components/coding-answer/coding-answer.component.ts` — standalone `OnPush` component; inputs: `question = input.required<CodingQuestionResponse>()`, `savedAnswer = input<CodingResponseRequest | undefined>(undefined)`, `assessmentId = input.required<string>()`; output: `answerChanged = output<AnswerChangedEvent>()`; inject `CandidateAssessmentService`; signals: `code = signal('')`, `running = signal(false)`, `results = signal<TestCaseResult[] | null>(null)`
- [x] 2.2 Add constructor `effect()` to pre-populate `code` signal from `savedAnswer().code` when `savedAnswer` changes (use `{ allowSignalWrites: true }`)
- [x] 2.3 Implement `onCodeChange(value: string)`: update `code` signal; emit `answerChanged` with `{ questionId: question().id, request: { code: value } }`
- [x] 2.4 Implement `runCode()`: guard if `running()` is true; set `running(true)`; call `service.executeCode(assessmentId(), question().id, code())`; on success set `results(response.results)` and `running(false)`; on error set `running(false)` (results unchanged)
- [x] 2.5 Create `dap-frontend/src/app/features/assessment/components/coding-answer/coding-answer.component.html` — language badge (`question().language`), `<textarea>` bound to `code()` with `(input)` calling `onCodeChange()`, "Run" button calling `runCode()` disabled when `running()`, loading spinner on button when `running()`, results panel (`@if (results())`) showing summary badge and a table of per-test-case rows (ordinal, pass/fail badge, actual output truncated at 500 chars, error message)

## 3. Wire CodingAnswerComponent into QuestionRendererComponent

- [x] 3.1 Add `assessmentId = input.required<string>()` to `QuestionRendererComponent` class
- [x] 3.2 Add `isCoding(): boolean` and `asCoding(): CodingQuestionResponse` helpers to `QuestionRendererComponent`
- [x] 3.3 Import `CodingAnswerComponent` in `QuestionRendererComponent` imports array
- [x] 3.4 Add `CodingQuestionResponse` and `CodingResponseRequest` to imports in `question-renderer.component.ts`
- [x] 3.5 Add `@if (isCoding())` block to `question-renderer.component.html` rendering `<dap-coding-answer [question]="asCoding()" [savedAnswer]="savedAnswer() as CodingResponseRequest | undefined" [assessmentId]="assessmentId()" (answerChanged)="answerChanged.emit($event)">`

## 4. Pass assessmentId from AssessmentTakingComponent

- [x] 4.1 Add `[assessmentId]="session()!.assessmentId"` binding to the `<dap-question-renderer>` usage in `assessment-taking.component.html`

## 5. Verification

- [x] 5.1 Run `ng build` (or `npm run build`) — no TypeScript compilation errors
- [ ] 5.2 Manual smoke test: start the dev server, open an assessment with a CODING question, verify the textarea, language badge, and Run button render; type code and verify auto-save fires; click Run and verify results panel populates
