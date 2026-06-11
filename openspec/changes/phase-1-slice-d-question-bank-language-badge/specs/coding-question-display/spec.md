## ADDED Requirements

### Requirement: Coding question rows show a language badge
The question list SHALL display a badge showing the programming language for any question with `type === 'CODING'`.

#### Scenario: Java badge
- **WHEN** the question list renders a question with `type === 'CODING'` and `language === 'JAVA'`
- **THEN** the row displays a badge labeled `"Java"`

#### Scenario: Python badge
- **WHEN** the question list renders a question with `type === 'CODING'` and `language === 'PYTHON'`
- **THEN** the row displays a badge labeled `"Python"`

#### Scenario: C# badge
- **WHEN** the question list renders a question with `type === 'CODING'` and `language === 'CSHARP'`
- **THEN** the row displays a badge labeled `"C#"`

#### Scenario: Badge absent for non-coding questions
- **WHEN** the question list renders a question with `type` other than `'CODING'` (e.g., `MCQ`, `TEXT`, `GROUP`)
- **THEN** no language badge is displayed for that row

### Requirement: Coding question rows show a test case count
The question list SHALL display the number of test cases for any question with `type === 'CODING'`, derived from `question.testCases.length`.

#### Scenario: Multiple test cases
- **WHEN** a `CODING` question has 3 entries in `testCases`
- **THEN** the row displays `"3 test cases"`

#### Scenario: Zero test cases
- **WHEN** a `CODING` question has an empty `testCases` array
- **THEN** the row displays `"0 test cases"`

#### Scenario: Count absent for non-coding questions
- **WHEN** the question list renders a question with `type` other than `'CODING'`
- **THEN** no test case count is displayed for that row

### Requirement: Coding question rows can expand to show a read-only test case list
Each `CODING` question row SHALL provide a toggle that expands the row to show a read-only list of its test cases, sourced from the already-loaded `CodingQuestionResponse.testCases` with no additional API call.

#### Scenario: Expanding a coding question row
- **WHEN** a marker clicks the "View test cases" toggle on a `CODING` question row
- **THEN** the row expands to show one entry per test case, each displaying input, expected output, timeout (seconds), and memory (MB)

#### Scenario: Collapsing an expanded row
- **WHEN** a marker clicks the toggle again on an expanded `CODING` question row
- **THEN** the test case list collapses and is hidden

#### Scenario: Expanding a coding question with no test cases
- **WHEN** a marker expands a `CODING` question row whose `testCases` array is empty
- **THEN** the expanded area shows a message indicating there are no test cases, with no errors

#### Scenario: Toggle absent for non-coding questions
- **WHEN** the question list renders a question with `type` other than `'CODING'`
- **THEN** no "View test cases" toggle is displayed for that row

### Requirement: CODING is a selectable type filter
The question list type filter SHALL include `'CODING'` as a selectable option, alongside the existing `MCQ`, `MCQ_PLUS`, `TEXT`, `DOC`, and `GROUP` options.

#### Scenario: Filtering by CODING
- **WHEN** a marker selects `"CODING"` from the type filter dropdown
- **THEN** the question list shows only questions with `type === 'CODING'`
