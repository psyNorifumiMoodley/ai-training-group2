### Requirement: ADMIN/MARKER can execute code against a coding question's test cases
`QuestionService` SHALL expose an `executeQuestion(id, code)` method that loads the `CodingQuestion` by ID, delegates to `CodeExecutionService.execute(question, code)`, and returns a `CodeExecuteResponse`. The result is NOT persisted — no `CodingResponse` or `TestCaseResult` entities are written.

#### Scenario: Valid execute request returns test case results
- **WHEN** an ADMIN or MARKER sends `POST /api/questions/{id}/execute` with a valid coding question ID and a non-empty `code` body
- **THEN** the server returns HTTP 200 with a `CodeExecuteResponse` containing a `results` array (one entry per test case) and an `executedAt` timestamp

#### Scenario: Unknown question ID returns 404
- **WHEN** `POST /api/questions/{id}/execute` is called with an ID that does not exist
- **THEN** the server returns HTTP 404

#### Scenario: Question ID is not a coding question returns 400
- **WHEN** `POST /api/questions/{id}/execute` is called with the ID of a non-CODING question
- **THEN** the server returns HTTP 400 (ValidationException)

#### Scenario: CANDIDATE role is rejected
- **WHEN** a CANDIDATE sends `POST /api/questions/{id}/execute`
- **THEN** the server returns HTTP 403

---

### Requirement: QuestionController exposes POST /api/questions/{id}/execute
`QuestionController` SHALL add a `POST /api/questions/{id}/execute` endpoint that accepts a `CodingExecuteRequest { code }` body, delegates to `QuestionService.executeQuestion(id, code)`, and returns the `CodeExecuteResponse` with HTTP 200. The endpoint requires ADMIN or MARKER role.

#### Scenario: Endpoint is accessible to MARKER
- **WHEN** a MARKER sends `POST /api/questions/{id}/execute` with a valid payload
- **THEN** the server processes the request and returns HTTP 200

#### Scenario: Endpoint is accessible to ADMIN
- **WHEN** an ADMIN sends `POST /api/questions/{id}/execute` with a valid payload
- **THEN** the server processes the request and returns HTTP 200

---

### Requirement: Coding question cards in question management show a "Test question" button
Each coding question card in `QuestionListComponent` SHALL display a "Test question" button visible only to ADMIN and MARKER. Clicking the button expands an inline test panel below the card (using the existing expand/collapse pattern on `expandedQuestionId`). Clicking again collapses it.

#### Scenario: Test button is visible on coding question cards
- **WHEN** an ADMIN or MARKER views the question list and a coding question is displayed
- **THEN** a "Test question" button is shown on the question card alongside the existing Edit and Delete buttons

#### Scenario: Clicking "Test question" expands the preview panel
- **WHEN** an ADMIN or MARKER clicks "Test question" on a coding question card
- **THEN** an inline `CodingQuestionPreviewComponent` panel appears below the card

#### Scenario: Clicking "Test question" a second time collapses the panel
- **WHEN** the preview panel is open and the user clicks "Test question" again
- **THEN** the panel collapses

---

### Requirement: CodingQuestionPreviewComponent provides an editor and run panel
`CodingQuestionPreviewComponent` (standalone, `OnPush`) SHALL render: the per-language code scaffold as initial code, a monospace `<textarea>` editor, a "Run" button that calls `QuestionService.executeQuestion()`, a loading state on the button while in flight, and a test case results panel matching the layout of `CodingAnswerComponent`. Results are ephemeral — not persisted between panel open/close cycles.

#### Scenario: Preview panel opens with language scaffold pre-filled
- **WHEN** the preview panel is opened for a Java coding question
- **THEN** the editor is pre-filled with the Java scaffold (e.g. `public class Solution { ... }`)

#### Scenario: Run button triggers execution and shows results
- **WHEN** the user types code and clicks "Run" in the preview panel
- **THEN** `QuestionService.executeQuestion()` is called, the button enters loading state, and on response the results panel populates with one row per test case

#### Scenario: Results are cleared when the panel is re-opened
- **WHEN** the preview panel is closed and reopened for the same question
- **THEN** the results panel is empty and the editor contains only the scaffold (no prior run results)
