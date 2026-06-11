## 1. QuestionService — CodingQuestion creation

- [x] 1.1 Constructor-inject `CodingQuestionRepository` into `QuestionService` (add field, update constructor)
- [x] 1.2 Add `instanceof CodingQuestionRequest` branch in `QuestionService.create()` calling a new `createCodingQuestion()` private method
- [x] 1.3 Implement `createCodingQuestion(CodingQuestionRequest)`: resolve question banks via `resolveQuestionBanks()`, set language, iterate `testCases` assigning ordinals 1..N (guard null testCases), save via `CodingQuestionRepository`
- [x] 1.4 Update `resolveQuestionBanks()` to throw `ValidationException` (instead of `NoSuchElementException`) when a bank ID is not found

## 2. QuestionService — CodingQuestion retrieval and mapping

- [x] 2.1 Add `instanceof CodingQuestion` branch in `QuestionService.toResponse()` calling a new `toCodingQuestionResponse()` private method
- [x] 2.2 Implement `toCodingQuestionResponse(CodingQuestion)`: map test cases (id, input, expectedOutput, timeoutSeconds, memoryMb, ordinal) and question banks to `CodingQuestionResponse`

## 3. QuestionService — CodingQuestion update

- [x] 3.1 Add `instanceof CodingQuestionRequest` + `instanceof CodingQuestion` branch in `QuestionService.update()`: update language, question, replace question banks (clear + addAll), replace test cases (clear existing + re-add with fresh ordinals), save

## 4. AssessmentService — combined doc/coding limit

- [x] 4.1 Update the doc-question count filter in `AssessmentService` from `q instanceof DocQuestion` to `q instanceof DocQuestion || q instanceof CodingQuestion`

## 5. Tests

- [x] 5.1 Create `CodingQuestionServiceTest` (`@SpringBootTest`, Testcontainers): `POST /api/questions` with `type: "CODING"`, valid `language` and `questionBankIds` → 201 with persisted language and question banks
- [x] 5.2 Add test: `POST /api/questions` with missing `language` → 400
- [x] 5.3 Add test: `POST /api/questions` with empty `questionBankIds` → 400
- [x] 5.4 Add test: `POST /api/questions` with unknown `questionBankIds` entry → 400
- [x] 5.5 Add test: `GET /api/questions/{id}` for a coding question → response includes `language`, `questionBanks`, and `testCases: []`
- [x] 5.6 Add test (in `AssessmentServiceTest`): assessment already has one `DocQuestion` + attempt to include one `CodingQuestion` → `ValidationException` (400)
- [x] 5.7 Add test (in `AssessmentServiceTest`): assessment already has two `CodingQuestion`s → `ValidationException` (400)
