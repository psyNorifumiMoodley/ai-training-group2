## MODIFIED Requirements

### Requirement: Admin Lists Candidates
An Admin SHALL be able to retrieve a paginated, searchable, sortable, and filterable list of all registered candidates.

#### Scenario: Paginated candidate list
- **WHEN** an authenticated Admin sends `GET /api/candidates?page=0&size=20`
- **THEN** the response is HTTP 200 with a paginated body containing candidate records and total count

#### Scenario: Search by name or email
- **WHEN** an authenticated Admin sends `GET /api/candidates?search=alice`
- **THEN** the response contains only candidates whose name or email contains "alice" (case-insensitive)

#### Scenario: Sort by name ascending
- **WHEN** an authenticated Admin sends `GET /api/candidates?sortBy=name&sortDir=asc`
- **THEN** the response is sorted alphabetically by candidate name in ascending order

#### Scenario: Sort by registration date descending
- **WHEN** an authenticated Admin sends `GET /api/candidates?sortBy=createdAt&sortDir=desc`
- **THEN** the response is sorted by registration date newest-first

#### Scenario: Filter by assessment status
- **WHEN** an authenticated Admin sends `GET /api/candidates?status=PENDING`
- **THEN** the response contains only candidates who have at least one assessment with status `PENDING`

#### Scenario: Combined search and sort
- **WHEN** an authenticated Admin sends `GET /api/candidates?search=smith&sortBy=email&sortDir=asc`
- **THEN** the response contains only candidates matching "smith" sorted by email ascending

---

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
- **THEN** the browser navigates to the candidate detail page at `/user-management/candidates/{id}`

#### Scenario: No inline Edit or Delete on list rows
- **WHEN** the candidates list is displayed
- **THEN** there are no Edit or Delete buttons on individual rows; only a View button is shown per row

#### Scenario: Search input filters the list
- **WHEN** an Admin types in the search input on the candidates list page
- **THEN** after a short debounce the list is refreshed to show only candidates matching the search term

#### Scenario: Clicking a column header sorts the list
- **WHEN** an Admin clicks a sortable column header (Name, Email, or Registration Date)
- **THEN** the list is re-fetched sorted by that column; clicking again toggles between ascending and descending

#### Scenario: Pagination controls navigate pages
- **WHEN** the total number of candidates exceeds the page size
- **THEN** pagination controls are displayed showing the current page, total pages, and previous/next navigation

#### Scenario: Filter control narrows by assessment status
- **WHEN** an Admin selects an assessment status from the filter control
- **THEN** the list is refreshed to show only candidates who have at least one assessment with that status
