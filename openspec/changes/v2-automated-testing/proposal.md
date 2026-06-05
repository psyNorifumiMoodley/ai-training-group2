## Why

v1 assessments support file uploads for coding questions but provide no automated validation — markers must manually run and evaluate candidate code. Automated testing eliminates this bottleneck, gives candidates instant feedback on correctness, and ensures consistent, objective grading for coding submissions.

## What Changes

- Extend `doc_question` with a nullable `language` field (JAVA, PYTHON, CSHARP) and a linked `test_case` table; a doc question with a non-null language becomes a "coding question"
- Add test case management: markers can define per-question test cases (input, expected output, timeout, memory limit)
- Integrate a Docker-based sandboxed execution engine on the backend host; per-submission containers run candidate code against each test case
- Trigger auto-grading on assessment submission for all coding questions; store per-test-case execution results
- Expose coding results to candidates (pass/fail count only) and to markers (full detail: input, expected output, actual output, stderr, execution time)
- Update the question bank UI: language selector and test case editor panel
- Update the marker review UI: per-test-case accordion on coding questions

## Capabilities

### New Capabilities

- `coding-question`: Extending doc_question with language and test case management — creation, editing, and deletion of test cases per coding question in the question bank
- `code-execution-engine`: Docker-based sandboxed code execution supporting Java 17, Python 3.12, and C# .NET 8 with per-test resource limits (timeout, memory)
- `coding-auto-grading`: Auto-grading triggered on assessment submission — runs all test cases via the execution engine, stores results, and records a pass/fail score per coding question
- `coding-result-reporting`: Role-differentiated result views — candidates see pass/fail count per coding question; markers see full per-test detail (input, expected, actual, stderr, execution time)

### Modified Capabilities

_(none — no existing spec-level requirements are changing)_

## Impact

- **Backend**: `doc_question` entity and `doc_question` DB table extended; new `test_case` table; new `execution_result` table; new `ExecutionEngine` service and language runner strategies; `GradingService` triggered from existing submission flow; two new result endpoints
- **Frontend**: Question bank question editor gains language dropdown and test case editor; marker review panel gains coding result accordion; candidate result view gains pass/fail badge per coding question
- **Dependencies**: `com.github.docker-java:docker-java` added to Maven POM; Docker socket must be accessible to the backend process in local and deployed environments
- **Existing rules**: The doc question limit (configurable, default 1 per assessment) continues to apply to coding questions — a coding question counts toward that limit
- **Jira Epic**: ATG-55
