## Why

The candidate-facing assessment UI has no handler for `CODING` questions — `QuestionRendererComponent` renders MCQ, TEXT, DOC, and GROUP types but silently skips `CODING`, leaving candidates who receive a coding assessment with a blank question and no way to enter or run their code. Slices A and B delivered the schema and backend service; this slice completes the feature by wiring the Angular UI.

## What Changes

- New `CodingAnswerComponent` (standalone, `OnPush`) rendering a textarea code editor, language badge, "Run" button with loading state, and a test case results panel
- `QuestionRendererComponent` receives a new `assessmentId` input and a `@if (isCoding())` branch that renders `CodingAnswerComponent`
- `AssessmentTakingComponent` passes `assessmentId` down to `QuestionRendererComponent`
- `CandidateAssessmentService.executeCode()` stub (`return EMPTY`) replaced with the real `POST /api/assessments/{id}/responses/{questionId}/execute` HTTP call
- Code auto-save plugs into the existing `autoSave$` pipeline in `AssessmentTakingComponent` by emitting `AnswerChangedEvent` with a `CodingResponseRequest` — no new save infrastructure needed

## Capabilities

### New Capabilities

_(none — all UI behaviour is already specified in `coding-assessment-ui`)_

### Modified Capabilities

- `coding-assessment-ui`: Implementation of the existing spec — `CodingAnswerComponent` creation, `QuestionRendererComponent` CODING branch, auto-save wiring, and execute service call

## Impact

- **Modified files**:
  - `dap-frontend/src/app/features/assessment/components/question-renderer/question-renderer.component.ts` — new `assessmentId` input, `isCoding()` / `asCoding()` helpers, `CodingAnswerComponent` import
  - `dap-frontend/src/app/features/assessment/components/question-renderer/question-renderer.component.html` — new `@if (isCoding())` block
  - `dap-frontend/src/app/features/assessment/components/assessment-taking/assessment-taking.component.html` — pass `[assessmentId]` binding
  - `dap-frontend/src/app/core/services/candidate-assessment.service.ts` — replace `executeCode()` stub
- **New files**:
  - `dap-frontend/src/app/features/assessment/components/coding-answer/coding-answer.component.ts`
  - `dap-frontend/src/app/features/assessment/components/coding-answer/coding-answer.component.html`
- **No new routes, no new services, no backend changes**
