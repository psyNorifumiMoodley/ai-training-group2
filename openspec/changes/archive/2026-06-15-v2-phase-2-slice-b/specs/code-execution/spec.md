## ADDED Requirements

### Requirement: CodeExecutionService Interface Abstracts Judge0
The system SHALL define a `CodeExecutionService` interface with a single method `List<TestCaseResultResponse> execute(CodingQuestion question, String code)` that the `Judge0CodeExecutionService` implements.

#### Scenario: Interface method is called with question and code
- **WHEN** `CodeExecutionService.execute(question, code)` is called with a valid `CodingQuestion` (containing test cases) and a non-blank code string
- **THEN** a non-null list of `TestCaseResultResponse` objects is returned, one per test case in the question

---

### Requirement: Judge0CodeExecutionService Maps Language Enums to Judge0 Language IDs
The implementation SHALL map `CodingQuestionLanguage.JAVA` to Judge0 language ID 91, `PYTHON` to 71, and `CSHARP` to 51. No other language mapping is required in v1.

#### Scenario: Java code is submitted with language_id 91
- **WHEN** the `CodingQuestion` has `language = JAVA` and `execute()` is called
- **THEN** every submission in the Judge0 batch request uses `language_id: 91`

#### Scenario: Python code is submitted with language_id 71
- **WHEN** the `CodingQuestion` has `language = PYTHON` and `execute()` is called
- **THEN** every submission in the Judge0 batch request uses `language_id: 71`

#### Scenario: C# code is submitted with language_id 51
- **WHEN** the `CodingQuestion` has `language = CSHARP` and `execute()` is called
- **THEN** every submission in the Judge0 batch request uses `language_id: 51`

---

### Requirement: Judge0CodeExecutionService Reads Config from Application Properties
The implementation SHALL read `code-execution.judge0.base-url` (required) and `code-execution.judge0.api-key` (optional, defaults to empty string) via `@Value`. Neither value MAY be hardcoded.

#### Scenario: base-url property drives the HTTP request target
- **WHEN** `code-execution.judge0.base-url=http://localhost:2358` is set
- **THEN** the batch submission HTTP call is made to `http://localhost:2358/submissions/batch`

#### Scenario: api-key header is sent only when non-blank
- **WHEN** `code-execution.judge0.api-key` is set to a non-blank value
- **THEN** the HTTP request includes an `X-RapidAPI-Key` header with that value

#### Scenario: api-key is absent when property is blank
- **WHEN** `code-execution.judge0.api-key` is blank or not set
- **THEN** no `X-RapidAPI-Key` header is sent in the Judge0 request

---

### Requirement: Per-Test-Case Results Are Returned Regardless of Individual Failure
If a test case fails or causes a runtime/compile error, the implementation SHALL still return a `TestCaseResultResponse` for it with `passed = false` and `errorMessage` populated. A single test case failure MUST NOT throw an exception from `execute()`.

#### Scenario: Compile error returns passed=false with error message
- **WHEN** the candidate's code fails to compile and Judge0 returns a `compile_output` for a test case
- **THEN** the result for that test case has `passed = false`, `actualOutput = null`, and `errorMessage` containing the compile output

#### Scenario: Runtime error returns passed=false with stderr
- **WHEN** the candidate's code throws a runtime exception and Judge0 returns `stderr` for a test case
- **THEN** the result for that test case has `passed = false`, `actualOutput = null`, and `errorMessage` containing the stderr content

#### Scenario: Accepted test case returns passed=true
- **WHEN** Judge0 returns status ID 3 (Accepted) for a test case
- **THEN** the result has `passed = true` and `errorMessage = null`

#### Scenario: Judge0 completely unreachable — execute() throws
- **WHEN** Judge0 cannot be reached (connection refused, timeout) and the HTTP call fails
- **THEN** `execute()` throws a runtime exception; callers are responsible for handling this (auto-execution wraps it; on-demand execution surfaces HTTP 500)
