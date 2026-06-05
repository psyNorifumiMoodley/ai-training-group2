## ADDED Requirements

### Requirement: All coding questions are auto-graded on assessment submission
When an assessment is submitted (explicitly by the candidate or by server-side auto-submit), the system SHALL automatically execute all test cases for every coding `doc_question` in that assessment and store the per-test-case results.

#### Scenario: Submission triggers auto-grading for coding questions
- **WHEN** a candidate submits an assessment that contains one or more coding questions
- **THEN** for each coding question, all of its test cases are executed via the execution engine and one `execution_result` row is stored per test case

#### Scenario: Auto-grading does not block the submission response
- **WHEN** a candidate submits an assessment
- **THEN** the submission response is returned with status `SUBMITTED`; grading runs asynchronously and does not hold the HTTP response

#### Scenario: Assessments with no coding questions are unaffected
- **WHEN** a candidate submits an assessment that contains no coding questions (only MCQ, text, or plain doc questions)
- **THEN** no execution engine calls are made and grading proceeds as in v1

---

### Requirement: Output comparison uses trimmed string equality
The grading system SHALL compare the candidate's actual stdout to the expected output using trimmed string equality (leading and trailing whitespace stripped from both sides before comparison).

#### Scenario: Output matches with trailing newline difference
- **WHEN** the expected output is `"42"` and the actual stdout is `"42\n"`
- **THEN** the test case result is `passed = true`

#### Scenario: Output does not match
- **WHEN** the expected output is `"42"` and the actual stdout is `"43"`
- **THEN** the test case result is `passed = false`

---

### Requirement: Each test case execution is stored as an execution result
The system SHALL persist one `execution_result` record per test case per submission. Each record stores: `submissionId`, `testCaseId`, `docQuestionId`, `passed`, `actualOutput`, `stderr`, `executionTimeMs`, and `errorType`.

#### Scenario: Successful test case result is stored
- **WHEN** a test case executes and the output matches the expected output
- **THEN** an `execution_result` row is created with `passed = true` and `errorType = NONE`

#### Scenario: Failed test case result is stored with error detail
- **WHEN** a test case execution fails due to a timeout
- **THEN** an `execution_result` row is created with `passed = false`, `errorType = TIMEOUT`, and `executionTimeMs` reflecting the actual elapsed time

#### Scenario: Duplicate execution result for same submission and test case is prevented
- **WHEN** an `execution_result` already exists for a given `(submissionId, testCaseId)` pair
- **THEN** the system does not create a duplicate (UNIQUE constraint enforced at the DB level)

---

### Requirement: Grading runs test cases concurrently per submission
The grading system SHALL execute all test cases for a given submission concurrently (not sequentially) to minimise total grading time.

#### Scenario: Multiple test cases execute in parallel
- **WHEN** an assessment submission triggers grading for a coding question with 5 test cases
- **THEN** all 5 test case containers are started concurrently and results are collected when all have completed
