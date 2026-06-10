## Why

v1 assessments support file uploads for coding questions but provide no automated validation — markers must manually run and evaluate candidate code. Automated testing eliminates this bottleneck, gives candidates instant feedback on correctness, and ensures consistent, objective grading for coding submissions.

## What Changes

- Soft-deprecate `doc_question`: existing plain file-upload questions remain valid and accessible, but markers can no longer create new `doc_question` entries via the UI or API
- Introduce `coding_question` as a new `TABLE_PER_CLASS` subtype of `assessment_question`; candidates submit source code as inline text; the question carries a **required** `language` field (JAVA, PYTHON, CSHARP)
- Add test case management: markers define per-question test cases on a `coding_question` (input, expected output, timeout, memory limit) via a new `test_case` child table
- Integrate a Docker-based sandboxed execution engine on the backend host; per-submission containers run candidate code against each test case
- Trigger auto-grading on assessment submission for all coding questions; store per-test-case execution results
- Expose coding results to candidates (pass/fail count only) and to markers (full detail: input, expected output, actual output, stderr, execution time)
- Update the question bank UI: new "Coding Question" creation form with language selector and test case editor panel; "Doc Question" creation option is removed
- Update the marker review UI: per-test-case accordion on coding questions

## Capabilities

### New Capabilities

- `coding-question`: New `coding_question` subtype with required language and inline source-code submission; test case management — creation, editing, and deletion of test cases per coding question in the question bank; `doc_question` is soft-deprecated (no new creation)
- `code-execution-engine`: Docker-based sandboxed code execution supporting Java 17, Python 3.12, and C# .NET 8 with per-test resource limits (timeout, memory)
- `coding-auto-grading`: Auto-grading triggered on assessment submission — runs all test cases via the execution engine, stores results, and records a pass/fail score per coding question
- `coding-result-reporting`: Role-differentiated result views — candidates see pass/fail count per coding question; markers see full per-test detail (input, expected, actual, stderr, execution time)

### Modified Capabilities

_(none — no existing spec-level requirements are changing)_

## Impact

- **Backend**: New `coding_question` DB table (TABLE_PER_CLASS subtype of `assessment_question`); new `test_case` table (FK → `coding_question`); new `execution_result` table (FK → `coding_question`); new `ExecutionEngine` service and language runner strategies; `GradingService` triggered from existing submission flow; two new result endpoints; `doc_question` creation endpoints are blocked (soft deprecation)
- **Frontend**: Question bank adds a new "Coding Question" creation form with language selector and test case editor; "Doc Question" creation option removed; marker review panel gains coding result accordion; candidate result view gains pass/fail badge per coding question
- **Dependencies**: `com.github.docker-java:docker-java` added to Maven POM; Docker socket must be accessible to the backend process in local and deployed environments. The Question Model Refactor (`.claude/specs/v1-assessment-platform/phase-6-question-model-refactor.md`) must be merged before Phase 1 Slice 0 — it removes `category` from every question type (including `coding_question`) in favour of `questionBankIds`/`questionBanks` (`Set<QuestionBank>`)
- **Existing rules**: The doc question limit (configurable, default 1 per assessment) applies to both `doc_question` (legacy) and `coding_question` rows — a coding question counts toward that limit
- **Jira Epic**: ATG-55
