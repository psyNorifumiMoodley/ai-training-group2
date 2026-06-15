## 1. New Backend DTOs

- [ ] 1.1 Create `CodingResponseRequest` record in `com.psybergate.dap.dto`: single field `@NotBlank String code`; implements `ResponseRequest`
- [ ] 1.2 Create `TestCaseResultResponse` record: `UUID testCaseId`, `boolean passed`, `String actualOutput`, `Long executionTimeMs`, `Long memoryUsedMb`, `String errorMessage`, `int ordinal`
- [ ] 1.3 Create `CodeExecuteResponse` record: `List<TestCaseResultResponse> results`, `Instant executedAt`

## 2. Update ResponseRequest Sealed Interface

- [ ] 2.1 Add `CodingResponseRequest` to the `permits` clause of `ResponseRequest.java`
- [ ] 2.2 Add `@JsonSubTypes.Type(CodingResponseRequest.class)` entry to the `@JsonSubTypes` annotation on `ResponseRequest.java`
- [ ] 2.3 Verify the sealed interface still compiles — every `permits` entry has a matching `@JsonSubTypes.Type`

## 3. Stub Execute Endpoint

- [ ] 3.1 Add `POST /api/assessments/{id}/responses/{questionId}/execute` stub handler to `AssessmentController`: `@PreAuthorize("hasRole('CANDIDATE')")`, path variables `UUID id` and `UUID questionId`, returns `ResponseEntity<CodeExecuteResponse>` with HTTP 200 and a hardcoded `CodeExecuteResponse` (empty `results` list, `executedAt = Instant.now()`)

## 4. Angular Model Types

- [ ] 4.1 Add `CodingResponseRequest` interface to `assessment-session.model.ts`: `{ code: string }`
- [ ] 4.2 Add `TestCaseResult` interface to `assessment-session.model.ts`: `{ testCaseId: string; passed: boolean; actualOutput: string | null; executionTimeMs: number | null; memoryUsedMb: number | null; errorMessage: string | null; ordinal: number }`
- [ ] 4.3 Add `CodeExecuteResponse` interface to `assessment-session.model.ts`: `{ results: TestCaseResult[]; executedAt: string }`
- [ ] 4.4 Add `CodingResponseRequest` to the `ResponseRequest` union type in `assessment-session.model.ts`

## 5. Angular Service Stub

- [ ] 5.1 Add `executeCode(assessmentId: string, questionId: string, code: string): Observable<CodeExecuteResponse>` stub method to `CandidateAssessmentService`: import `EMPTY` from `rxjs`; return `EMPTY as unknown as Observable<CodeExecuteResponse>` (same pattern as other stubs in the project)

## 6. Verification

- [ ] 6.1 Backend compiles with no errors: `mvn compile` from `dap-backend/`
- [ ] 6.2 Stub test: `PUT /api/assessments/{id}/responses/{questionId}` with body `{ "code": "class X{}" }` and CANDIDATE JWT → 204
- [ ] 6.3 Stub test: `PUT /api/assessments/{id}/responses/{questionId}` with body `{ "code": "" }` → 400 (blank code rejected by bean validation)
- [ ] 6.4 Stub test: `POST /api/assessments/{id}/responses/{questionId}/execute` with CANDIDATE JWT → 200 with empty `results` array
- [ ] 6.5 Stub test: `POST /api/assessments/{id}/responses/{questionId}/execute` with MARKER JWT → 403
- [ ] 6.6 Angular compiles with no TypeScript errors: `ng build --no-optimization` from `dap-frontend/`
