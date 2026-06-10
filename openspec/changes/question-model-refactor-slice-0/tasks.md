## 1. New DTOs

- [ ] 1.1 Create `QuestionBankRequest` record: `@NotBlank String name`
- [ ] 1.2 Create `QuestionBankResponse` record: `UUID id`, `String name`
- [ ] 1.3 Create `GroupChildRequest` record: `@NotBlank String questionText`, `List<String> keywords`, `@Min(1) int marks`
- [ ] 1.4 Create `GroupChildResponse` record: `UUID id`, `String questionText`, `List<String> keywords`, `int marks`
- [ ] 1.5 Create `McqPlusQuestionRequest` record implementing `QuestionRequest`: `@NotEmpty List<UUID> questionBankIds`, `@NotBlank String question`, `@NotEmpty List<String> options`, `@NotEmpty List<String> correctAnswers`, `@NotBlank String followUpQuestion`, `List<String> followUpKeywords`, `@Min(1) int followUpMarks`
- [ ] 1.6 Create `McqPlusQuestionResponse` record implementing `QuestionResponse`: `UUID id`, `List<QuestionBankResponse> questionBanks`, `String question`, `List<String> options`, `List<String> correctAnswers`, `boolean multiCorrect`, `String followUpQuestion`, `List<String> followUpKeywords`, `int followUpMarks`, `int totalMarks`

## 2. Update Existing Question DTOs

- [ ] 2.1 Update `McqQuestionRequest`: remove `String category`; add `@NotEmpty List<UUID> questionBankIds`
- [ ] 2.2 Update `McqQuestionResponse`: remove `String category`; add `List<QuestionBankResponse> questionBanks`
- [ ] 2.3 Update `TextQuestionRequest`: remove `String category`; add `@NotEmpty List<UUID> questionBankIds`; add `@Min(1) int marks`
- [ ] 2.4 Update `TextQuestionResponse`: remove `String category`; add `List<QuestionBankResponse> questionBanks`; add `int marks`
- [ ] 2.5 Update `DocQuestionRequest`: remove `String category`; add `@NotEmpty List<UUID> questionBankIds`; add `@Min(1) int marks`
- [ ] 2.6 Update `DocQuestionResponse`: remove `String category`; add `List<QuestionBankResponse> questionBanks`; add `int marks`
- [ ] 2.7 Update `GroupQuestionRequest`: remove `String category`; remove `List<UUID> followUpQuestionIds`; add `@NotEmpty List<UUID> questionBankIds`; add `@NotEmpty @Valid List<GroupChildRequest> children`
- [ ] 2.8 Update `GroupQuestionResponse`: remove `String category`; remove `List<TextQuestionResponse> followUpQuestions`; add `List<QuestionBankResponse> questionBanks`; add `List<GroupChildResponse> children`; add `int totalMarks`
- [ ] 2.9 Update `CodingQuestionRequest`: remove `String category`; add `@NotEmpty List<UUID> questionBankIds`
- [ ] 2.10 Update `CodingQuestionResponse`: remove `String category`; add `List<QuestionBankResponse> questionBanks`

## 3. Update Sealed Interfaces

- [ ] 3.1 Update `QuestionRequest` sealed interface: add `McqPlusQuestionRequest` to `permits`; add `@JsonSubTypes.Type(value = McqPlusQuestionRequest.class, name = "MCQ_PLUS")` entry
- [ ] 3.2 Update `QuestionResponse` sealed interface: add `McqPlusQuestionResponse` to `permits`; add `@JsonSubTypes.Type(value = McqPlusQuestionResponse.class, name = "MCQ_PLUS")` entry
- [ ] 3.3 Verify both sealed interfaces compile — every `permits` entry has a matching `@JsonSubTypes.Type`

## 4. Stub Controller

- [ ] 4.1 Create `QuestionBankController` with `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")` on class level
- [ ] 4.2 Implement stub `GET /api/question-banks` → 200 with empty `List<QuestionBankResponse>`
- [ ] 4.3 Implement stub `POST /api/question-banks` → 201 with hardcoded `QuestionBankResponse`
- [ ] 4.4 Implement stub `PUT /api/question-banks/{id}` → 200 with hardcoded `QuestionBankResponse`
- [ ] 4.5 Implement stub `DELETE /api/question-banks/{id}` → 204

## 5. Update QuestionController

- [ ] 5.1 Remove `GET /api/questions/categories` endpoint handler entirely
- [ ] 5.2 Replace `@RequestParam(required = false) String category` with `@RequestParam(required = false) UUID questionBankId` on `GET /api/questions`

## 6. Angular Type Definitions

- [ ] 6.1 Replace `question.model.ts` content: remove all `category` fields; add `QuestionBankResponse`, `GroupChildRequest`, `GroupChildResponse` interfaces; add `McqPlusQuestionRequest`, `McqPlusQuestionResponse` interfaces; add `'MCQ_PLUS'` to `QuestionType`; add `marks` to `TextQuestionRequest/Response` and `DocQuestionRequest/Response`; add `children`/`totalMarks` to Group interfaces; update `CodingQuestion` interfaces; update `QuestionRequest` and `QuestionResponse` union types
- [ ] 6.2 Create `question-bank.service.ts` stub with four methods returning `EMPTY`: `getQuestionBanks()`, `createQuestionBank(name)`, `renameQuestionBank(id, name)`, `deleteQuestionBank(id)`
- [ ] 6.3 Update `question.service.ts`: change `getQuestions()` signature from `category?: string` to `questionBankId?: string`; remove `getCategories()` method

## 7. Verification

- [ ] 7.1 Backend compiles with no errors: `mvn compile`
- [ ] 7.2 Stub tests pass: `POST /api/question-banks` with MARKER JWT → 201; `DELETE /api/question-banks/{id}` with CANDIDATE JWT → 403
- [ ] 7.3 Stub test: `POST /api/questions` type `MCQ_PLUS` with valid body → 201 (hardcoded)
- [ ] 7.4 Stub test: `POST /api/questions` type `TEXT` without `marks` → 400 (bean validation)
- [ ] 7.5 Stub test: `POST /api/questions` type `GROUP` without `children` → 400 (bean validation)
- [ ] 7.6 Stub test: `GET /api/questions/categories` → 404
- [ ] 7.7 Angular compiles with no TypeScript errors: `ng build --no-optimization`
