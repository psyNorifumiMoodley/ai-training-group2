## Context

`QuestionService` currently dispatches creation and retrieval for five question types (`McqQuestion`, `McqPlusQuestion`, `DocQuestion`, `TextQuestion`, `GroupQuestion`) via `instanceof` branches. Slice A (ATG-60) adds the `CodingQuestion` and `TestCase` JPA entities plus `CodingQuestionRepository`. Without this slice, a `POST /api/questions` with `"type": "CODING"` reaches the fall-through `UnsupportedOperationException` in `QuestionService.create()`.

`AssessmentService` currently enforces `assessment.doc-question-limit` only against `DocQuestion` rows (line 101). With coding questions soft-replacing doc questions, both subtypes must contribute to that count.

## Goals / Non-Goals

**Goals:**
- `QuestionService.create()` handles `CodingQuestionRequest` and returns a `CodingQuestionResponse` with `id`, `questionBanks`, `language`, and `testCases`
- `QuestionService.update()` handles `CodingQuestionRequest` against an existing `CodingQuestion`
- `QuestionService.toResponse()` maps `CodingQuestion` to `CodingQuestionResponse`
- Inline `testCases` from the request are assigned ordinals 1..N and persisted via `CascadeType.ALL` on `CodingQuestion.testCases`
- `AssessmentService` doc limit check counts `DocQuestion` + `CodingQuestion` rows combined
- `CodingQuestionServiceTest` provides integration coverage via `@SpringBootTest` + Testcontainers

**Non-Goals:**
- Angular UI (Slice C / ATG-62)
- Test case execution or grading (Phase 2+)
- Separate test-case sub-resource endpoint — test cases are always managed inline with their parent question
- No new controller endpoints — `QuestionController` already routes to `QuestionService`

## Decisions

### Follow the existing `instanceof` dispatch pattern

Every question type in `QuestionService` uses `instanceof` guards in both `create()` and `toResponse()`. Adding `CodingQuestion` as another branch is the lowest-risk approach — no refactoring required, no risk of breaking existing types, and matches the pattern reviewers already expect.

Alternatives considered: a `QuestionTypeHandler` strategy pattern. Rejected — premature abstraction; five types is not enough to justify the indirection, and the spec explicitly uses the `instanceof` pattern.

### Ordinal assignment in `createCodingQuestion()`

Ordinals are assigned during the loop that builds `TestCase` entities (`ordinal = i + 1`), immediately before the parent is saved. This keeps ordinal logic self-contained in `createCodingQuestion()` without a separate `reorderTestCases()` pass.

### `resolveQuestionBanks()` exception type

The current implementation throws `NoSuchElementException` for unknown bank IDs. The spec calls for `ValidationException`. This slice aligns the method to throw `ValidationException` so callers (controller's `@ExceptionHandler`) map unknown-bank errors to 400, consistent with all other validation failures. This is a safe change since `NoSuchElementException` and `ValidationException` both currently map to 400 via the global handler.

### Doc/coding limit: single combined count

`AssessmentService` currently filters `questions.stream().filter(q -> q instanceof DocQuestion)`. Changing the filter to `q instanceof DocQuestion || q instanceof CodingQuestion` is a one-line change with no schema impact. Alternative (two separate counts) rejected — unnecessary complexity.

## Risks / Trade-offs

- **Slice A dependency**: `CodingQuestion`, `TestCase`, and `CodingQuestionRepository` must exist before this slice compiles. If Slice A is delayed, this slice is blocked. Mitigation: keep on the same `v2/phase-1` branch; merge Slice A first.
- **`questionCount` in `QuestionBankResponse`**: All existing `toXxxResponse()` methods hard-code `0L` for `questionCount`. This slice follows the same pattern for `toCodingQuestionResponse()`. Accurate question counts are a separate concern not yet addressed.
- **CascadeType.ALL on test cases**: If `request.testCases()` is null, iterating it will NPE. Guard with a null-check (`request.testCases() != null ? request.testCases() : List.of()`) before the ordinal loop.

## Open Questions

- Should `update()` for a `CodingQuestion` replace all test cases (clear + re-add) or diff? The spec is silent on partial test case updates — this slice implements full replacement (clear existing + re-add from request), consistent with how `GroupQuestion.children` are handled.
