## ADDED Requirements

### Requirement: Coding question type is selectable in the question form
The `QuestionFormComponent` type selector SHALL include `CODING` and SHALL NOT include `DOC` as a creatable type. Existing questions of type `DOC` SHALL remain viewable and editable, with their type shown as a locked badge.

#### Scenario: CODING appears in the type selector
- **WHEN** a marker opens the "Add question" form
- **THEN** `CODING` is one of the selectable question types

#### Scenario: DOC is absent from the type selector
- **WHEN** a marker opens the "Add question" form
- **THEN** `DOC` is not offered as a selectable question type

#### Scenario: Existing DOC question remains editable
- **WHEN** a marker opens an existing question of type `DOC` for editing
- **THEN** the form opens in edit mode with `DOC` shown as a locked type badge and the existing marks field editable

### Requirement: Language selection is required for coding questions
When the selected type is `CODING`, the form SHALL present a required language dropdown with options Java, Python, and C#, and SHALL NOT accept submission until a language is chosen.

#### Scenario: Language dropdown renders the supported languages
- **WHEN** a marker selects `CODING` as the question type
- **THEN** the language dropdown displays exactly Java, Python, and C#

#### Scenario: Form is invalid without a language
- **WHEN** a marker selects `CODING`, fills in the question text and question banks, but does not select a language, and submits
- **THEN** the form does not submit and `QuestionService.createQuestion()` is not called

#### Scenario: Test case builder hidden until a language is selected
- **WHEN** a marker selects `CODING` as the question type and has not yet selected a language
- **THEN** the test case builder section is not rendered

#### Scenario: Test case builder visible once a language is selected
- **WHEN** a marker selects `CODING` as the question type and selects a language
- **THEN** the test case builder section becomes visible

### Requirement: Inline test case builder for coding questions
The form SHALL provide an inline test case builder for `CODING` questions, allowing markers to add, edit, and remove test case rows. Each row SHALL capture optional input, required expected output, a timeout in seconds (1–60), and memory in MB (64–1024).

#### Scenario: Add row appends a blank test case
- **WHEN** a marker clicks "Add row" in the test case builder
- **THEN** a new test case row is appended with empty input, empty expected output, default timeout, and default memory

#### Scenario: Remove row deletes only that row
- **WHEN** a marker clicks "Remove" on a specific test case row
- **THEN** only that row is removed and the remaining rows retain their values

#### Scenario: Expected output is required per row
- **WHEN** a marker leaves the expected output field blank on a test case row and attempts to submit
- **THEN** the form does not submit and an inline validation message is shown for that row

#### Scenario: Timeout and memory bounds are enforced
- **WHEN** a marker enters a timeout outside 1–60 or a memory value outside 64–1024 on a test case row
- **THEN** the form does not submit and an inline validation message is shown for that row

### Requirement: Submitting a coding question sends a CodingQuestionRequest
On submit of a valid `CODING` question, the form SHALL call `QuestionService.createQuestion()` (or `updateQuestion()` in edit mode) with a `CodingQuestionRequest` containing `type: 'CODING'`, `questionBankIds`, `question`, `language`, and `testCases`.

#### Scenario: Submit with valid data calls the service with a CodingQuestionRequest
- **WHEN** a marker selects `CODING`, enters question text, selects at least one question bank, selects a language, optionally adds test case rows, and submits
- **THEN** `QuestionService.createQuestion()` is called with `type: 'CODING'`, the selected `questionBankIds`, `question`, `language`, and a `testCases` array matching the entered rows

#### Scenario: Submit with no test cases sends an empty list
- **WHEN** a marker submits a valid `CODING` question with zero test case rows
- **THEN** `QuestionService.createQuestion()` is called with `testCases: []`

### Requirement: Editing a coding question pre-populates language and test cases
When opening an existing `CODING` question for editing, the form SHALL pre-select its language and pre-populate the test case builder from `CodingQuestionResponse.testCases`.

#### Scenario: Edit mode pre-populates language and test cases
- **WHEN** a marker opens an existing `CODING` question for editing
- **THEN** the language dropdown shows the question's current language and the test case builder shows one row per existing test case with its saved values
