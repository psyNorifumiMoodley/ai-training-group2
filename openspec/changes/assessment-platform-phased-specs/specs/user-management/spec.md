## ADDED Requirements

### Requirement: Admin Registers a Candidate
An Admin SHALL be able to register a new Candidate by providing their details. Candidates cannot self-register.

#### Scenario: Successful candidate registration
- **WHEN** an authenticated Admin sends a POST to `/api/candidates` with a valid name and email
- **THEN** the response is HTTP 201 with the created candidate's UUID, name, and email; a corresponding `app_user` record with role `CANDIDATE` is persisted

#### Scenario: Duplicate email is rejected
- **WHEN** an Admin attempts to register a candidate with an email already in use by any `app_user`
- **THEN** the response is HTTP 409 with a message indicating the email is already registered

#### Scenario: Unauthenticated registration attempt is rejected
- **WHEN** a POST to `/api/candidates` is made without a valid JWT
- **THEN** the response is HTTP 401

#### Scenario: Non-admin registration attempt is rejected
- **WHEN** a user with role `CANDIDATE` or `MARKER` sends a POST to `/api/candidates`
- **THEN** the response is HTTP 403

---

### Requirement: Admin Registers a Marker
An Admin SHALL be able to register a new Marker by providing their details.

#### Scenario: Successful marker registration
- **WHEN** an authenticated Admin sends a POST to `/api/markers` with a valid name, email, and password
- **THEN** the response is HTTP 201 with the created marker's UUID, name, and email; a corresponding `app_user` record with role `MARKER` is persisted

#### Scenario: Non-admin registration attempt is rejected
- **WHEN** a user without role `ADMIN` attempts to POST to `/api/markers`
- **THEN** the response is HTTP 403

---

### Requirement: Admin Lists Candidates
An Admin SHALL be able to retrieve a paginated list of all registered candidates.

#### Scenario: Paginated candidate list
- **WHEN** an authenticated Admin sends GET `/api/candidates?page=0&size=20`
- **THEN** the response is HTTP 200 with a paginated body containing candidate records and total count

---

### Requirement: Admin Lists Markers
An Admin SHALL be able to retrieve a paginated list of all registered markers.

#### Scenario: Paginated marker list
- **WHEN** an authenticated Admin sends GET `/api/markers?page=0&size=20`
- **THEN** the response is HTTP 200 with a paginated body containing marker records and total count

---

### Requirement: Role-Based Access Enforcement
The system SHALL enforce that each role can only access endpoints appropriate to their role.

#### Scenario: Candidate cannot access admin endpoints
- **WHEN** a user with role `CANDIDATE` requests an admin-only endpoint
- **THEN** the response is HTTP 403

#### Scenario: Marker cannot register users
- **WHEN** a user with role `MARKER` sends a POST to `/api/candidates` or `/api/markers`
- **THEN** the response is HTTP 403

---

### Requirement: Angular User Management UI
The Angular application SHALL provide an admin UI for registering and listing candidates and markers.

#### Scenario: Admin registers candidate via form
- **WHEN** an Admin submits the candidate registration form with valid data
- **THEN** the candidate is created via the API and the list view is refreshed to include the new candidate

#### Scenario: Validation errors displayed inline
- **WHEN** the Admin submits the registration form with missing required fields
- **THEN** inline validation errors are shown next to each invalid field without making an API call