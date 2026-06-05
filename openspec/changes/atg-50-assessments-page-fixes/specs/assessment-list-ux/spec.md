## ADDED Requirements

### Requirement: Table shows only relevant columns
The Assessments table SHALL display: Candidate (avatar + name), Status badge, Assigned date (ISO-8601), and Actions. The Role and Bank columns SHALL be removed.

#### Scenario: Role column absent
- **WHEN** an Admin or Marker views the assessments table
- **THEN** there is no "Role" column

#### Scenario: Bank column absent
- **WHEN** an Admin or Marker views the assessments table
- **THEN** there is no "Bank" column

#### Scenario: Assigned date ISO format
- **WHEN** the assigned date is displayed in the table
- **THEN** it is formatted as `YYYY-MM-DD` (e.g. `2026-06-05`)

### Requirement: Search and status filter bar
The Assessments page SHALL display a filter bar above the table containing a text search input (by candidate name) and a status dropdown (All / PENDING / IN_PROGRESS / SUBMITTED / MARKED). Filtering SHALL apply to the currently loaded page of assessments.

#### Scenario: Filter by candidate name
- **WHEN** the user types a candidate name into the search input
- **THEN** the table shows only rows where the candidate name contains the search text (case-insensitive)

#### Scenario: Filter by status
- **WHEN** the user selects a status from the dropdown
- **THEN** the table shows only rows matching that status

#### Scenario: Clear filter returns all rows
- **WHEN** the user clears the search input and selects "All" in the status dropdown
- **THEN** all loaded assessments are shown

### Requirement: Table auto-refreshes after assessment creation
After a new assessment is successfully created (auto-generate or manual selection), the assessments table SHALL reload from the backend to reflect the new entry.

#### Scenario: Auto-generate creates new entry
- **WHEN** the user creates an assessment via the auto-generate modal and closes/confirms
- **THEN** the assessments table reloads and the new assessment appears in the list

#### Scenario: Manual selection creates new entry
- **WHEN** the user creates an assessment via manual question selection and the success state is shown
- **THEN** navigating back to `/assessments` shows the new assessment in the table

### Requirement: Actions column — Mark available to Admin and Marker
The Mark action in the Actions column SHALL be visible and functional for both ADMIN and MARKER roles when assessment status is SUBMITTED or MARKED.

#### Scenario: Admin can access marking
- **WHEN** an Admin views an assessment with status SUBMITTED or MARKED
- **THEN** the "Mark" button is visible and navigates to the marking page

#### Scenario: Marker can access marking
- **WHEN** a Marker views an assessment with status SUBMITTED or MARKED
- **THEN** the "Mark" button is visible and navigates to the marking page

### Requirement: Actions column — Copy invitation link
The Actions column SHALL show a "Copy link" button for assessments with status PENDING or IN_PROGRESS. Clicking it SHALL copy the invitation link to the clipboard.

#### Scenario: Copy link for PENDING assessment
- **WHEN** the user clicks "Copy link" on a PENDING assessment
- **THEN** the invitation link is copied to the clipboard and a brief visual confirmation is shown

#### Scenario: Copy link not shown for completed assessments
- **WHEN** an assessment has status SUBMITTED or MARKED
- **THEN** the "Copy link" button is not shown

### Requirement: Actions column — Remind candidate
The Actions column SHALL show a "Remind" button for assessments with status PENDING. Clicking it SHALL trigger the backend remind endpoint and show a toast notification.

#### Scenario: Remind sends notification
- **WHEN** the user clicks "Remind" on a PENDING assessment
- **THEN** a POST request is sent to `/api/assessments/{id}/remind` and a success toast is shown on response

#### Scenario: Remind not shown for non-PENDING assessments
- **WHEN** an assessment status is IN_PROGRESS, SUBMITTED, or MARKED
- **THEN** the "Remind" button is not shown
