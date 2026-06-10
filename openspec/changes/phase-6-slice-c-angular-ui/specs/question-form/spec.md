## MODIFIED Requirements

### Requirement: Question form sends questionBankIds instead of category
The `QuestionFormComponent` SHALL replace the `category` text field with a question bank multi-select. At least one bank must be selected before the form can be submitted. The selected bank IDs are sent as `questionBankIds` in all question request types.

#### Scenario: Form loads available banks on open
- **WHEN** the question form opens (create or edit mode)
- **THEN** all existing `QuestionBank` records are shown as selectable checkboxes, populated from `QuestionBankService.getQuestionBanks()`

#### Scenario: Submitting without a bank selected shows an error
- **WHEN** the user submits the form with no bank checked
- **THEN** an error message "Select at least one question bank." appears and no API call is made

#### Scenario: Edit mode pre-selects the question's existing banks
- **WHEN** the form opens in edit mode for a question that belongs to one or more banks
- **THEN** those banks are pre-checked in the multi-select

#### Scenario: Created question carries the selected questionBankIds
- **WHEN** the user selects two banks and submits a valid MCQ form
- **THEN** `POST /api/questions` is called with `questionBankIds` containing both selected bank IDs

### Requirement: TEXT and DOC question forms include a marks field
The `QuestionFormComponent` SHALL render a `marks` number input (min 1) when the selected type is `TEXT` or `DOC`.

#### Scenario: Marks field defaults to 1 on create
- **WHEN** the user selects TEXT or DOC type in a new question form
- **THEN** the marks input is visible and pre-filled with 1

#### Scenario: Marks field is pre-filled in edit mode
- **WHEN** the form opens in edit mode for a TEXT question with `marks: 5`
- **THEN** the marks input shows 5

#### Scenario: Marks below 1 blocks submission
- **WHEN** the user sets marks to 0 and submits
- **THEN** the form is invalid and no API call is made

#### Scenario: Submitted TEXT question includes marks
- **WHEN** the user sets marks to 3 and submits a valid TEXT question
- **THEN** `POST /api/questions` is called with `"marks": 3` in the request body

### Requirement: GROUP question form uses inline children builder
The `QuestionFormComponent` SHALL use `GroupChildrenBuilderComponent` when type is `GROUP`. Submission requires at least one child.

#### Scenario: GROUP form shows GroupChildrenBuilderComponent
- **WHEN** the user selects GROUP as the question type
- **THEN** a children builder section appears with an "Add child" button; the old follow-up picker is not shown

#### Scenario: Each child requires questionText and marks ≥ 1
- **WHEN** a child is added with empty questionText or marks < 1 and the form is submitted
- **THEN** the form is invalid and no API call is made

#### Scenario: Submitting GROUP with zero children shows an error
- **WHEN** the user submits a GROUP form with no children added
- **THEN** an error "Add at least one child question." is shown and no API call is made

#### Scenario: Edit mode pre-populates children
- **WHEN** the form opens in edit mode for a GROUP question with two existing children
- **THEN** both children are shown in the builder with their existing questionText, keywords, and marks

#### Scenario: Submitted GROUP question contains inline children
- **WHEN** the user adds two children and submits a valid GROUP question
- **THEN** `POST /api/questions` is called with a `children` array containing both child objects (no `followUpQuestionIds`)

## ADDED Requirements

### Requirement: MCQ_PLUS question type is available in the form
The `QuestionFormComponent` SHALL include `MCQ_PLUS` in the type selector and render follow-up fields when it is selected.

#### Scenario: MCQ_PLUS appears in the type selector
- **WHEN** the user opens the question type dropdown
- **THEN** `MCQ_PLUS` is listed as a selectable option alongside MCQ, TEXT, DOC, GROUP

#### Scenario: MCQ_PLUS form shows MCQ section and follow-up section
- **WHEN** the user selects MCQ_PLUS
- **THEN** the standard MCQ option builder is shown AND a follow-up section below it containing: followUpQuestion textarea (required), followUpKeywords keyword list (optional), followUpMarks number input (required, min 1)

#### Scenario: Submitting MCQ_PLUS without followUpQuestion is blocked
- **WHEN** the user leaves followUpQuestion empty and submits
- **THEN** the form is invalid and no API call is made

#### Scenario: Submitted MCQ_PLUS includes all follow-up fields
- **WHEN** the user completes a valid MCQ_PLUS form and submits
- **THEN** `POST /api/questions` is called with `type: "MCQ_PLUS"`, `followUpQuestion`, `followUpKeywords`, and `followUpMarks` in the body

#### Scenario: Edit mode pre-fills MCQ_PLUS follow-up fields
- **WHEN** the form opens in edit mode for an MCQ_PLUS question
- **THEN** followUpQuestion, followUpKeywords, and followUpMarks are pre-populated from the existing question response
