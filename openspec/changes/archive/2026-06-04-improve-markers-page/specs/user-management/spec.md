## MODIFIED Requirements

### Requirement: Admin Lists Markers
An Admin SHALL be able to retrieve a paginated, searchable, and sortable list of all registered markers.

#### Scenario: Paginated marker list
- **WHEN** an authenticated Admin sends `GET /api/markers?page=0&size=20`
- **THEN** the response is HTTP 200 with a paginated body containing marker records and total count

#### Scenario: Search by name or email
- **WHEN** an authenticated Admin sends `GET /api/markers?search=jane`
- **THEN** the response contains only markers whose name or email contains "jane" (case-insensitive)

#### Scenario: Sort by name ascending
- **WHEN** an authenticated Admin sends `GET /api/markers?sortBy=name&sortDir=asc`
- **THEN** the response is sorted alphabetically by marker name in ascending order

#### Scenario: Sort by registration date descending
- **WHEN** an authenticated Admin sends `GET /api/markers?sortBy=createdAt&sortDir=desc`
- **THEN** the response is sorted by registration date newest-first

#### Scenario: Combined search and sort
- **WHEN** an authenticated Admin sends `GET /api/markers?search=smith&sortBy=email&sortDir=asc`
- **THEN** the response contains only markers matching "smith" sorted by email ascending

---

## ADDED Requirements

### Requirement: Admin Updates a Marker
An Admin SHALL be able to update a marker's name and email via `PUT /api/markers/{id}`.

#### Scenario: Successful update
- **WHEN** an authenticated Admin sends `PUT /api/markers/{id}` with a valid name and email
- **THEN** the response is HTTP 200 with the updated marker record

#### Scenario: Duplicate email rejected on update
- **WHEN** an Admin attempts to update a marker's email to one already in use by another user
- **THEN** the response is HTTP 409 with a message indicating the email is already registered

#### Scenario: Marker not found
- **WHEN** an Admin sends `PUT /api/markers/{id}` with a UUID that does not exist
- **THEN** the response is HTTP 404

#### Scenario: Non-admin update attempt rejected
- **WHEN** a user without role `ADMIN` sends a PUT to `/api/markers/{id}`
- **THEN** the response is HTTP 403

---

### Requirement: Admin Deletes a Marker
An Admin SHALL be able to delete a marker via `DELETE /api/markers/{id}`, provided the marker has no submitted markings.

#### Scenario: Successful delete
- **WHEN** an authenticated Admin sends `DELETE /api/markers/{id}` for a marker with no feedback records
- **THEN** the response is HTTP 204 and the marker's `app_user` record is removed

#### Scenario: Delete blocked by existing feedback
- **WHEN** an Admin sends `DELETE /api/markers/{id}` for a marker who has submitted markings
- **THEN** the response is HTTP 409 with a message indicating the marker cannot be deleted

#### Scenario: Marker not found
- **WHEN** an Admin sends `DELETE /api/markers/{id}` with a UUID that does not exist
- **THEN** the response is HTTP 404

#### Scenario: Non-admin delete attempt rejected
- **WHEN** a user without role `ADMIN` sends a DELETE to `/api/markers/{id}`
- **THEN** the response is HTTP 403

---

## MODIFIED Requirements

### Requirement: Angular User Management UI
The Angular application SHALL provide an admin UI for registering and listing candidates and markers.

#### Scenario: Admin registers candidate via form
- **WHEN** an Admin submits the candidate registration form with valid data
- **THEN** the candidate is created via the API and the list view is refreshed to include the new candidate

#### Scenario: Validation errors displayed inline
- **WHEN** the Admin submits the registration form with missing required fields
- **THEN** inline validation errors are shown next to each invalid field without making an API call

#### Scenario: View button navigates to candidate detail
- **WHEN** an Admin or Marker clicks the View button on a candidate row
- **THEN** the browser navigates to the candidate detail page at `/candidates/{id}`

#### Scenario: No inline Edit or Delete on candidate list rows
- **WHEN** the candidates list is displayed
- **THEN** there are no Edit or Delete buttons on individual rows; only a View button is shown per row

#### Scenario: Search input filters the candidate list
- **WHEN** an Admin types in the search input on the candidates list page
- **THEN** after a short debounce the list is refreshed to show only candidates matching the search term

#### Scenario: Clicking a column header sorts the candidate list
- **WHEN** an Admin clicks a sortable column header (Name, Email, or Registration Date) on the candidates list
- **THEN** the list is re-fetched sorted by that column; clicking again toggles between ascending and descending

#### Scenario: Pagination controls navigate candidate pages
- **WHEN** the total number of candidates exceeds the page size
- **THEN** pagination controls are displayed showing the current page, total pages, and previous/next navigation

#### Scenario: Filter control narrows candidates by assessment status
- **WHEN** an Admin selects an assessment status from the filter control
- **THEN** the list is refreshed to show only candidates who have at least one assessment with that status

#### Scenario: Search input filters the marker list
- **WHEN** an Admin types in the search input on the markers list page
- **THEN** after a short debounce the list is refreshed to show only markers matching the search term

#### Scenario: Clicking a column header sorts the marker list
- **WHEN** an Admin clicks a sortable column header (Name, Email, or Registration Date) on the markers list
- **THEN** the list is re-fetched sorted by that column; clicking again toggles between ascending and descending

#### Scenario: Pagination controls navigate marker pages
- **WHEN** the total number of markers exceeds the page size
- **THEN** pagination controls are displayed showing the current page, total pages, and previous/next navigation

#### Scenario: Admin edits a marker inline
- **WHEN** an Admin clicks Edit on a marker row and submits valid changes
- **THEN** the marker's name and/or email is updated via the API and the list row reflects the new values

#### Scenario: Admin deletes a marker with confirmation
- **WHEN** an Admin clicks Delete on a marker row and confirms the action
- **THEN** the marker is deleted via the API and removed from the list

#### Scenario: Delete blocked marker shows error
- **WHEN** an Admin attempts to delete a marker who has submitted markings
- **THEN** an error message is displayed indicating the marker cannot be deleted
