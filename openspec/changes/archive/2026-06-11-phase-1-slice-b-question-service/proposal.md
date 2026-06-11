## Why

Phase 1 (ATG-58) introduces `CodingQuestion` as a new question type. Slice A (ATG-60) creates the `coding_question` and `test_case` DB tables and JPA entities. This slice (ATG-61) wires those entities into `QuestionService` so that `POST /api/questions` with `"type": "CODING"` persists a `CodingQuestion` with inline test cases, and the assessment doc/coding limit check is extended to count both `DocQuestion` and `CodingQuestion` rows.

## What Changes

- **`QuestionService`**: add `instanceof CodingQuestionRequest` branch in `create()` and `update()`, add private `createCodingQuestion()` and `toCodingQuestionResponse()` helpers, assign ordinals (1..N) to inline test cases before save, constructor-inject `CodingQuestionRepository`
- **`QuestionService.toResponse()`**: add `instanceof CodingQuestion` branch mapping to `CodingQuestionResponse`
- **`AssessmentService`**: extend the doc-question limit check to count both `DocQuestion` and `CodingQuestion` rows (combined count checked against `assessment.doc-question-limit`)
- **`CodingQuestionServiceTest`**: new `@SpringBootTest` + Testcontainers test covering creation, validation, retrieval, and doc/coding limit enforcement

## Capabilities

### New Capabilities

- `coding-question-service`: `QuestionService` can create, update, and retrieve `CodingQuestion` entities with inline test cases and QB associations; `AssessmentService` limits the combined doc+coding question count per assessment

### Modified Capabilities

<!-- No existing spec-level requirement changes -->

## Impact

- **Backend**: `QuestionService`, `AssessmentService` — both modified
- **Repositories**: `CodingQuestionRepository` injected into `QuestionService` (from Slice A)
- **API behaviour**: `POST /api/questions` with `"type": "CODING"` previously threw `UnsupportedOperationException`; now returns 201 with full `CodingQuestionResponse`
- **Test infrastructure**: Testcontainers required (same as all other service tests in this project)
- **Depends on**: Slice A (ATG-60) — `CodingQuestion`, `TestCase` entities and `CodingQuestionRepository` must be present before this slice can merge
