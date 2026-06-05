## Why

The Assessments list page has several UX gaps: stale columns (Role, Bank) clutter the table, the date is not ISO-formatted, the table doesn't refresh after a new assessment is created, Admins can't navigate to marking, and the action set is incomplete (no copy-link or remind actions). These gaps reduce usability for Admins and Markers managing daily assessment workflows.

## What Changes

- Remove the **Role** and **Bank** columns from the assessments table (they add noise without value at this level)
- Display `assignedDate` in ISO-8601 format (`YYYY-MM-DD`)
- Reload the assessments table automatically after a new assessment is successfully created (both auto-generated and manual selection flows)
- Add **search + status filter** bar above the table so users can quickly find assessments by candidate name and filter by status
- Expand the **Actions** column:
  - **Mark** — already present; ensure Admins can see/use it (currently gated to MARKER role on the frontend only; backend already allows ADMIN)
  - **Copy link** — copy the assessment invitation link to clipboard for PENDING and IN_PROGRESS assessments (requires `invitationLink` added to the list DTO on the backend)
  - **Remind** — re-send the invitation email when status is PENDING (requires a new `POST /api/assessments/{id}/remind` backend endpoint)
- After **manual question selection** completes successfully, show a confirmation modal (matching the auto-generate success style) instead of just an inline page message
- The question count (10) is a system constant; the dialog should display "10 questions" as a label so users understand the target

## Capabilities

### New Capabilities
- `assessment-list-ux`: Search/filter bar, column removal, ISO date, table auto-refresh, and expanded action buttons on the Assessments list page
- `assessment-remind`: New backend endpoint that re-sends the invitation email for a PENDING assessment

### Modified Capabilities
- `assessment-generation`: After manual question selection succeeds, show a modal-style confirmation consistent with the auto-generate flow

## Impact

- **Frontend**: `assessment-list.component` (ts + html), `assessment.service.ts`, `assessment.model.ts`, `assessment-generate.component` (ts + html), `question-selection.component` (ts + html)
- **Backend**: `AssessmentSummaryResponse` (add `invitationLink` field), `AssessmentController` (new remind endpoint), `AssessmentService` (remind method re-sends invitation email)
- No DB schema changes required
- No breaking API changes — only additive DTO field and new endpoint
