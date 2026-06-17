## Context

`CodingAnswerComponent` initialises the `code` signal from `savedAnswer().code`; when there is no saved answer the editor is blank, leaving candidates to write boilerplate before they can start solving the problem. Additionally, question authors have no way to run their own code against a question's test cases without being a candidate in a live assessment.

`CodeExecutionService.execute(question, code)` already exists and is used by `ResponseService`. The `QuestionController` already manages question CRUD. The missing pieces are: (1) a scaffold lookup in the frontend and (2) a preview endpoint that wires `QuestionController` → `QuestionService` → `CodeExecutionService` without touching the response/submission layer.

## Goals / Non-Goals

**Goals:**
- Pre-populate the candidate code editor with a per-language skeleton when no prior code is saved
- Give ADMIN/MARKER a "Test question" run panel on each coding question card in question management
- Expose `POST /api/questions/{id}/execute` so the frontend can run code against a question's test cases without creating an assessment

**Non-Goals:**
- Storing the scaffold template in the database or making it per-question-customisable (future extension)
- Persisting test-run results triggered from the preview panel
- Rate-limiting the preview endpoint (v1 scope)

## Decisions

**Decision 1 — Scaffold as a frontend constant map, not a DB column**

The scaffold is a `Record<CodingQuestionLanguage, string>` constant in the frontend. No migration needed and it can be updated without a deployment coupling. If per-question scaffold customisation is needed later, a nullable `code_scaffold` column can be added to `coding_question` without breaking existing data.

**Decision 2 — Preview panel is inline (expand/collapse), not a modal**

`QuestionListComponent` already has an `expandedQuestionId` signal for toggling detail rows. The test panel slots into that same pattern rather than introducing a new modal component. This keeps the question list self-contained and avoids a second overlay layer.

**Decision 3 — New `CodingQuestionPreviewComponent` rather than reusing `CodingAnswerComponent`**

`CodingAnswerComponent` is wired to `CandidateAssessmentService.executeCode(assessmentId, questionId, code)`. The preview uses `QuestionService.executeQuestion(questionId, code)` — a different service, different inputs. A thin dedicated component keeps the coupling clean and avoids adding conditional logic to the candidate component.

**Decision 4 — Preview endpoint lives in `QuestionController`, delegates through `QuestionService`**

`POST /api/questions/{id}/execute` fits the existing noun-based REST structure. `QuestionService.executeQuestion(id, code)` loads the `CodingQuestion` and delegates to `CodeExecutionService.execute(question, code)`, returning `CodeExecuteResponse` directly. No `CodingResponse` or `TestCaseResult` entities are written.

**Decision 5 — Scaffold applied only when `savedAnswer` is absent/empty**

The effect in `CodingAnswerComponent` that reads `savedAnswer()` already handles re-population on re-access. The scaffold is set as the default value in the same effect: `if (!saved?.code) { this.code.set(scaffold); }`. This ensures an already-started candidate never loses their work when the component re-initialises.

## Risks / Trade-offs

- [Scaffold mismatch] The pre-built template may not match the method signature the test harness expects → Mitigation: Templates use standard entry-point conventions (Java `main`, Python `__main__`, C# `Main`); test case authors should design inputs/outputs around these.
- [Preview misuse] Markers could run expensive code via the preview endpoint → Mitigation: Out of scope for v1; the same Judge0 timeout/memory limits apply as for candidate submissions.
