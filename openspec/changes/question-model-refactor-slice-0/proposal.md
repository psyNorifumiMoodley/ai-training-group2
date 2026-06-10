## Why

The question domain is being refactored to replace the flat `category` string with named `QuestionBank` entities, add `marks` to questions, introduce `McqPlusQuestion`, and embed `GroupQuestion` children inline rather than linking to standalone `TextQuestion` rows. Slice 0 lays the API contract foundation — updated DTOs, sealed interface changes, and frontend type definitions — so that all parallel implementation slices (schema, backend logic, Angular UI) can develop against stable contracts simultaneously without merge conflicts.

## What Changes

- **BREAKING** — `category: String` removed from all question request and response DTOs (`McqQuestionRequest`, `TextQuestionRequest`, `DocQuestionRequest`, `GroupQuestionRequest`, `CodingQuestionRequest` and their response counterparts); replaced with `questionBankIds: List<UUID>` in requests and `questionBanks: List<QuestionBankResponse>` in responses
- **BREAKING** — `GroupQuestionRequest.followUpQuestionIds: List<UUID>` removed; replaced with `children: List<GroupChildRequest>` (inline question data, not ID references)
- **BREAKING** — `GroupQuestionResponse.followUpQuestions: List<TextQuestionResponse>` removed; replaced with `children: List<GroupChildResponse>`
- **BREAKING** — `GET /api/questions/categories` endpoint removed; `?category=` query param on `GET /api/questions` replaced with `?questionBankId=`
- New DTOs: `QuestionBankRequest`, `QuestionBankResponse`, `GroupChildRequest`, `GroupChildResponse`
- New question type: `McqPlusQuestionRequest` and `McqPlusQuestionResponse` added to `QuestionRequest` / `QuestionResponse` sealed interfaces
- `marks: int` added to `TextQuestionRequest`, `TextQuestionResponse`, `DocQuestionRequest`, `DocQuestionResponse`
- `totalMarks: int` (computed sum of children marks) added to `GroupQuestionResponse`
- `totalMarks: int` (always `1 + followUpMarks`) added to `McqPlusQuestionResponse`
- New stub controller: `QuestionBankController` — four CRUD endpoints (`GET`, `POST`, `PUT`, `DELETE` at `/api/question-banks`), all returning hardcoded responses
- Angular `question.model.ts` fully updated; new stub `QuestionBankService` added; `QuestionService.getCategories()` removed
- `CodingQuestionRequest` / `CodingQuestionResponse` (merged in Phase 6 Slice 0) updated: `category` replaced with `questionBankIds` / `questionBanks`

## Capabilities

### New Capabilities

- `question-bank-management`: CRUD API for named QuestionBank entities; question-to-QB many-to-many association replaces the category string; questions must belong to at least one QB
- `mcq-plus-question`: New question type extending MCQ with an embedded text follow-up question; always shown to candidates regardless of MCQ correctness; MCQ part auto-marked (1 mark), text part manually marked by a Marker; counts as one MCQ slot in assessment composition
- `question-marks`: Integer marks field on TextQuestion and DocQuestion; GroupQuestion total marks derived from sum of child marks; MCQ constant at 1 mark; McqPlus total = 1 + followUpMarks
- `group-question-inline-children`: GroupQuestion children created inline (not linked from standalone TextQuestions); stored as embedded GroupQuestionChild entities owned by the parent; absent from all standalone TextQuestion queries

### Modified Capabilities

- `assessment-generation`: `AssessmentRequest` gains `questionBankIds` replacing category-based scoping; auto-generation pools questions from the union of selected QBs; McqPlus counts toward the MCQ composition quota

## Impact

- **Backend DTOs** — all files in `com.psybergate.dap.dto` related to questions: breaking changes to all question request and response records
- **Backend sealed interfaces** — `QuestionRequest.java`, `QuestionResponse.java`: new permits and `@JsonSubTypes` entries
- **Backend controller stubs** — `QuestionController.java` (remove categories endpoint, update query param); new `QuestionBankController.java`
- **Frontend models** — `question.model.ts` complete replacement; `question.service.ts` signature update; new `question-bank.service.ts` stub
- **Downstream slices blocked until this merges** — Schema Migration (Slice A), Backend Logic (Slice B), Angular UI (Slice C) all depend on these contracts
- **Phase 6 CodingQuestion DTOs** — already-merged `CodingQuestionRequest` / `CodingQuestionResponse` are breaking-changed in this slice
