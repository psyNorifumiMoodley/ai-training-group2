## Context

The `QuestionRendererComponent` is the single dispatch point for all question types in the assessment-taking flow. It currently handles MCQ, MCQ_PLUS, TEXT, DOC, and GROUP via `@if` branches in its template. The `CODING` type is present in `QuestionType` and `CodingQuestionResponse` is modelled in `question.model.ts`, but no rendering branch exists — a coding question renders as empty.

`CandidateAssessmentService.executeCode()` was scaffolded in Slice 0 but returns `EMPTY` as a stub. The auto-save pipeline (`autoSave$` Subject with `debounceTime(1500)` → `saveResponse()`) in `AssessmentTakingComponent` is already wired and works for all existing response types.

## Goals / Non-Goals

**Goals:**
- Render a usable code editor (styled `<textarea>`) for CODING questions inside the existing assessment-taking flow
- Wire the Run button to `CandidateAssessmentService.executeCode()` and display per-test-case results
- Plug code changes into the existing auto-save pipeline via `AnswerChangedEvent`
- Replace the `executeCode()` stub with a real HTTP call

**Non-Goals:**
- Syntax highlighting or a third-party editor library (Monaco, CodeMirror) — a `<textarea>` is sufficient per spec
- Test case input/expected-output visibility for the candidate (not in spec)
- Marker-side coding response display (separate feature)

## Decisions

### 1. `CodingAnswerComponent` injects `CandidateAssessmentService` directly

**Decision**: `CodingAnswerComponent` injects the service and calls `executeCode()` itself rather than emitting an event and having `AssessmentTakingComponent` handle execution.

**Rationale**: Execute is fire-and-display — the results are local state owned by `CodingAnswerComponent` (the results panel). Bubbling the event up to `AssessmentTakingComponent` would give the parent state it has no use for. Keeping it local reduces coupling and keeps the parent's interface unchanged.

**Alternative considered**: Add an `executeRequested` output on `CodingAnswerComponent` and a `codeExecuted` input for results — rejected because it scatters state management and requires the parent to store per-question result maps.

---

### 2. Pass `assessmentId` as a new input on `QuestionRendererComponent`

**Decision**: Add `assessmentId = input.required<string>()` to `QuestionRendererComponent` and pass it through to `CodingAnswerComponent`.

**Rationale**: `CodingAnswerComponent` needs the assessment ID to call `executeCode(assessmentId, questionId)`. `QuestionRendererComponent` is the immediate parent rendered by `AssessmentTakingComponent`, which already holds the session (and therefore the `assessmentId`). Adding a single input is less invasive than injecting a shared state service.

**Alternative considered**: Have `CodingAnswerComponent` read `assessmentId` from the URL params or a session signal — rejected because `QuestionRendererComponent` is already rendered inside the session context and the parent already has the ID.

---

### 3. Auto-save code through the existing `AnswerChangedEvent` pipeline

**Decision**: `CodingAnswerComponent` emits `AnswerChangedEvent` (same interface as other answer types) with a `CodingResponseRequest`. `QuestionRendererComponent` forwards it via its existing `answerChanged` output. `AssessmentTakingComponent`'s `autoSave$` pipeline handles it identically to text/MCQ answers.

**Rationale**: Zero new infrastructure — the debounce, switchMap, and `saveResponse()` call are already in place. CODING just plugs in as another emitter.

---

### 4. Results state is local to `CodingAnswerComponent`

**Decision**: `results = signal<TestCaseResult[] | null>(null)` lives inside `CodingAnswerComponent` and is not persisted to the parent or to any service state.

**Rationale**: The spec requires displaying results after "Run" — there's no requirement to restore results on page reload (only the code itself is restored via `savedAnswer`). Keeping results ephemeral avoids complexity.

## Risks / Trade-offs

- **`assessmentId` input on `QuestionRendererComponent` is unused for non-CODING questions** → Acceptable — the input is typed `string`, costs nothing at runtime, and is passed unconditionally from the parent which always has it.
- **`switchMap` in auto-save cancels in-flight saves if typing resumes** → Pre-existing behaviour, identical for all response types. Acceptable for v1.
- **No error UI on execute failure** → If the HTTP call to execute fails, the `running` signal is reset and the results panel stays empty (or shows the previous run). A toast or inline error message would be a UX improvement but is not required by the spec.

## Migration Plan

No database or API changes. Angular build only. No backwards compatibility concerns — `CodingAnswerComponent` is a new component; the changes to `QuestionRendererComponent` are additive (new input, new template branch).
