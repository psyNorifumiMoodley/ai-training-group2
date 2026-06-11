## MODIFIED Requirements

### Requirement: Question bank list is loaded from the API
The Angular `BankListComponent` SHALL load the list of question banks by calling `GET /api/question-banks` via `QuestionBankService.getQuestionBanks()` rather than deriving bank names from loaded question responses.

#### Scenario: Banks appear on page load
- **WHEN** a MARKER or ADMIN navigates to the question banks page
- **THEN** all existing `QuestionBank` records are shown in the sidebar, each with its name, loaded from the API

#### Scenario: Empty bank list shows prompt
- **WHEN** the API returns an empty list from `GET /api/question-banks`
- **THEN** the sidebar shows a "No question banks yet" empty state and the Create Bank form is visible

#### Scenario: Selecting a bank filters the question list
- **WHEN** the user clicks a bank in the sidebar
- **THEN** `GET /api/questions?questionBankId={id}` is called and only questions in that bank are shown

## ADDED Requirements

### Requirement: Markers can create a new question bank from the Angular UI
The `BankListComponent` SHALL provide a Create Bank inline form. Submitting calls `QuestionBankService.createQuestionBank(name)` which posts to `POST /api/question-banks`.

#### Scenario: Create bank with a unique name succeeds
- **WHEN** the user enters a name that does not already exist and clicks Save
- **THEN** the bank list refreshes and the new bank appears; a success toast is shown

#### Scenario: Create bank with a duplicate name shows an error
- **WHEN** the user enters a name that already exists and the API returns HTTP 409
- **THEN** a field-level error "A bank with this name already exists" appears; no toast; the form stays open

#### Scenario: Create bank with empty name is blocked client-side
- **WHEN** the user submits the create form with an empty name
- **THEN** the form is marked invalid and no API call is made

### Requirement: Markers can rename an existing question bank
Each bank row SHALL show a rename action that allows the name to be edited inline and saved via `QuestionBankService.renameQuestionBank(id, name)`.

#### Scenario: Rename to a new unique name succeeds
- **WHEN** the user edits a bank name and saves, and the API returns 200
- **THEN** the bank list refreshes showing the new name; an update toast is shown

#### Scenario: Rename to a duplicate name shows an error
- **WHEN** the API returns HTTP 409 on rename
- **THEN** a field-level error "A bank with this name already exists" is shown inline

#### Scenario: Rename cancelled discards changes
- **WHEN** the user opens the rename input and presses Cancel
- **THEN** the original name is restored with no API call made

### Requirement: Markers can delete a question bank that has no questions
Each bank row SHALL show a delete action. Confirmation is required. Deletion calls `QuestionBankService.deleteQuestionBank(id)`.

#### Scenario: Delete bank with no questions succeeds
- **WHEN** the user confirms deletion and the API returns 204
- **THEN** the bank is removed from the list; a success toast is shown

#### Scenario: Delete bank with questions shows a conflict error
- **WHEN** the API returns HTTP 409 on delete
- **THEN** an error toast "This bank still has questions — remove them first" is shown; the bank remains in the list

#### Scenario: Delete confirmation modal shown before deletion
- **WHEN** the user clicks delete on a bank
- **THEN** a confirmation modal is shown before any API call is made
