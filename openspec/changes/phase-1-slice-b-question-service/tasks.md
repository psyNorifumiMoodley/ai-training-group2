## 1. QuestionService — CodingQuestion creation

- [ ] 1.1 Constructor-inject `CodingQuestionRepository` into `QuestionService` (add field, update constructor)
- [ ] 1.2 Add `instanceof CodingQuestionRequest` branch in `QuestionService.create()` calling a new `createCodingQuestion()` private method
- [ ] 1.3 Implement `createCodingQuestion(CodingQuestionRequest)`: resolve question banks via `resolveQuestionBanks()`, set language, iterate `testCases` assigning ordinals 1..N (guard null testCases), save via `CodingQuestionRepository`
- [ ] 1.4 Update `resolveQuestionBanks()` to throw `ValidationException` (instead of `NoSuchElementException`) when a bank ID is not found

## 2. QuestionService — CodingQuestion retrieval and mapping

- [ ] 2.1 Add `instanceof CodingQuestion` branch in `QuestionService.toResponse()` calling a new `toCodingQuestionResponse()` private method
- [ ] 2.2 Implement `toCodingQuestionResponse(CodingQuestion)`: map test cases (id, input, expectedOutput, timeoutSeconds, memoryMb, ordinal) and question banks to `CodingQuestionResponse`

## 3. QuestionService — CodingQuestion update

- [ ] 3.1 Add `instanceof CodingQuestionRequest` + `instanceof CodingQuestion` branch in `QuestionService.update()`: update language, question, replace question banks (clear + addAll), replace test cases (clear existing + re-add with fresh ordinals), save

## 4. AssessmentService — combined doc/coding limit

- [ ] 4.1 Update the doc-question count filter in `AssessmentService` from `q instanceof DocQuestion` to `q instanceof DocQuestion || q instanceof CodingQuestion`

## 5. Tests

- [ ] 5.1 Create `CodingQuestionServiceTest` (`@SpringBootTest`, Testcontainers): `POST /api/questions` with `type: "CODING"`, valid `language` and `questionBankIds` → 201 with persisted language and question banks
- [ ] 5.2 Add test: `POST /api/questions` with missing `language` → 400
- [ ] 5.3 Add test: `POST /api/questions` with empty `questionBankIds` → 400
- [ ] 5.4 Add test: `POST /api/questions` with unknown `questionBankIds` entry → 400
- [ ] 5.5 Add test: `GET /api/questions/{id}` for a coding question → response includes `language`, `questionBanks`, and `testCases: []`
- [ ] 5.6 Add test: assessment already has one `DocQuestion` + attempt to include one `CodingQuestion` → 409
- [ ] 5.7 Add test: assessment already has one `CodingQuestion` + attempt to include another → 409
