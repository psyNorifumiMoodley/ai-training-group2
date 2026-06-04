## ADDED Requirements

### Requirement: Admin Views Candidate Detail
An Admin or Marker SHALL be able to navigate to a dedicated detail page for a candidate, which displays the candidate's profile and provides access to Edit and Delete actions.

#### Scenario: Navigating to the detail page
- **WHEN** an authenticated Admin or Marker clicks the View button on a candidate row in the candidates list
- **THEN** the browser navigates to `/user-management/candidates/{id}` and the candidate's name, email, and registration date are displayed

#### Scenario: Unauthenticated access is rejected
- **WHEN** a request is made to `GET /api/candidates/{id}` without a valid JWT
- **THEN** the response is HTTP 401

#### Scenario: Candidate not found
- **WHEN** a request is made to `GET /api/candidates/{id}` with a UUID that does not exist
- **THEN** the response is HTTP 404

---

### Requirement: Edit and Delete Actions on Candidate Detail Page
Edit and Delete actions for a candidate SHALL only be accessible from within the candidate detail page, not from the candidates list.

#### Scenario: Admin edits a candidate from the detail page
- **WHEN** an Admin clicks Edit on the candidate detail page and submits a valid update
- **THEN** the candidate's name and/or email is updated via the API and the detail page reflects the new values

#### Scenario: Admin deletes a candidate from the detail page
- **WHEN** an Admin clicks Delete on the candidate detail page and confirms the action
- **THEN** the candidate is deleted via the API and the browser navigates back to the candidates list

#### Scenario: Marker cannot edit or delete candidates
- **WHEN** a user with role `MARKER` is viewing the candidate detail page
- **THEN** the Edit and Delete buttons are not visible

---

### Requirement: Candidate Assessments List on Detail Page
The candidate detail page SHALL display a list of all assessments assigned to that candidate.

#### Scenario: Candidate has assessments
- **WHEN** an Admin or Marker views the detail page for a candidate who has one or more assessments
- **THEN** all assessments are listed showing at minimum: assessment status, time limit, and creation date

#### Scenario: Candidate has no assessments
- **WHEN** an Admin or Marker views the detail page for a candidate who has no assessments
- **THEN** an empty state message is displayed indicating no assessments have been assigned

#### Scenario: Backend returns assessments for a candidate
- **WHEN** an authenticated Admin or Marker sends `GET /api/candidates/{id}/assessments`
- **THEN** the response is HTTP 200 with an array of assessment summaries for that candidate (id, status, timeLimitMinutes, createdAt)

#### Scenario: Non-admin/non-marker access is rejected
- **WHEN** a user with role `CANDIDATE` sends `GET /api/candidates/{id}/assessments`
- **THEN** the response is HTTP 403
