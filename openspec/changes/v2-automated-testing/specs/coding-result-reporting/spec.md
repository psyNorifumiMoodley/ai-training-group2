## ADDED Requirements

### Requirement: Candidate views pass/fail count for coding questions in their result
After an assessment is marked, a candidate SHALL be able to see how many test cases passed out of the total for each coding question in their result view. Test case inputs and expected outputs are NOT disclosed to the candidate.

#### Scenario: Candidate views coding question result summary
- **WHEN** a candidate with role `CANDIDATE` sends `GET /api/assessments/{id}/coding-results`
- **THEN** the response is HTTP 200 with a list of `{ codingQuestionId, passed, total }` — one entry per coding question in the assessment

#### Scenario: Candidate cannot access another candidate's coding results
- **WHEN** a candidate sends `GET /api/assessments/{id}/coding-results` for an assessment not assigned to them
- **THEN** the response is HTTP 403

#### Scenario: No coding questions returns empty list
- **WHEN** a candidate sends `GET /api/assessments/{id}/coding-results` for an assessment with no coding questions
- **THEN** the response is HTTP 200 with an empty array

#### Scenario: Coding result visible in candidate UI as a badge
- **WHEN** a candidate views their result page and one of the questions is a coding question
- **THEN** the question displays a badge showing "X / Y passed" (e.g., "3 / 5 passed")

#### Scenario: Test case inputs and expected outputs are absent from the candidate response
- **WHEN** a candidate sends `GET /api/assessments/{id}/coding-results`
- **THEN** the response body contains no `input` or `expectedOutput` fields

---

### Requirement: Marker views full per-test-case detail for coding questions
A Marker or Admin SHALL be able to view the complete execution result for each test case on a coding question — including input, expected output, actual output, stderr, execution time, and error type.

#### Scenario: Marker views full coding result detail
- **WHEN** a user with role `MARKER` or `ADMIN` sends `GET /api/assessments/{id}/coding-results/detail`
- **THEN** the response is HTTP 200 with a list of execution results, each containing: `testCaseId`, `codingQuestionId`, `passed`, `input`, `expectedOutput`, `actualOutput`, `stderr`, `executionTimeMs`, `errorType`

#### Scenario: Candidate role is rejected from detail endpoint
- **WHEN** a user with role `CANDIDATE` sends `GET /api/assessments/{id}/coding-results/detail`
- **THEN** the response is HTTP 403

#### Scenario: Marker UI shows per-test accordion on coding question
- **WHEN** a Marker opens the submission review for an assessment that contains a coding question
- **THEN** the coding question panel displays an expandable accordion row per test case, showing: pass/fail indicator, input, expected output, actual output, stderr (if any), and execution time in milliseconds

#### Scenario: Compile error is clearly identified in marker view
- **WHEN** a test case result has `errorType = COMPILE_ERROR`
- **THEN** the marker UI displays "Compile Error" as the status and shows the `stderr` content (compiler error message)

#### Scenario: Timeout is clearly identified in marker view
- **WHEN** a test case result has `errorType = TIMEOUT`
- **THEN** the marker UI displays "Timeout" as the status with the configured timeout limit
