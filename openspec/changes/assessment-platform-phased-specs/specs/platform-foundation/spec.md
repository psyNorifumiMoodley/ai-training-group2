## ADDED Requirements

### Requirement: Project Scaffolding
The system SHALL provide a runnable local development environment with a Spring Boot 3.x backend, Angular 20 frontend, and PostgreSQL database orchestrated via Docker Compose.

#### Scenario: Developer starts local environment
- **WHEN** a developer runs `docker compose up` from the project root
- **THEN** PostgreSQL starts on port 5432, the backend starts on port 8080, and the frontend dev server starts on port 4200

#### Scenario: Backend application starts cleanly
- **WHEN** the Spring Boot application starts
- **THEN** it connects to PostgreSQL, runs all Liquibase migrations, and logs no errors

#### Scenario: Frontend application starts cleanly
- **WHEN** the Angular dev server starts
- **THEN** the application loads at `http://localhost:4200` with no console errors

---

### Requirement: Database Baseline Migration
The system SHALL establish the initial PostgreSQL schema via Liquibase with all tables defined in the domain model present from the first migration.

#### Scenario: Liquibase runs on first startup
- **WHEN** the backend starts against a clean PostgreSQL database
- **THEN** Liquibase creates the `DATABASECHANGELOG` table and applies all baseline changesets without error

#### Scenario: Schema matches domain model
- **WHEN** the baseline migration is applied
- **THEN** tables `app_user`, `candidate`, `question_bank`, `assessment`, `submission`, `response` (and subtables) exist with all columns, PKs, FKs, and unique constraints as specified in the domain model

#### Scenario: Re-running migrations is idempotent
- **WHEN** the backend is restarted against a database where migrations have already run
- **THEN** Liquibase applies no new changesets and the application starts successfully

---

### Requirement: JWT Authentication Endpoint
The system SHALL expose a public login endpoint that returns a signed JWT on successful authentication.

#### Scenario: Valid credentials return JWT
- **WHEN** a POST request is sent to `/api/auth/login` with a valid email and password
- **THEN** the response is HTTP 200 with a JSON body containing a `token` field (signed JWT) and the authenticated user's `role`

#### Scenario: Invalid credentials are rejected
- **WHEN** a POST request is sent to `/api/auth/login` with an incorrect password
- **THEN** the response is HTTP 401 with the standard error shape `{ status, error, message, timestamp }`

#### Scenario: Protected endpoints reject unauthenticated requests
- **WHEN** a request is sent to any endpoint other than `/api/auth/login` or `GET /api/assessments/access/{token}` without a valid `Authorization: Bearer <jwt>` header
- **THEN** the response is HTTP 401

---

### Requirement: Shared API Error Shape
The system SHALL return all error responses in a consistent JSON structure.

#### Scenario: Validation error returns standard shape
- **WHEN** a request fails bean validation (e.g., missing required field)
- **THEN** the response body matches `{ "status": <http-code>, "error": <reason>, "message": <detail>, "timestamp": <ISO-8601 UTC> }`

#### Scenario: Not-found error returns standard shape
- **WHEN** a requested resource does not exist
- **THEN** the response is HTTP 404 with a body matching the standard error shape

---

### Requirement: Angular HTTP Client Bootstrap
The Angular application SHALL include a configured HTTP client with a JWT interceptor that attaches the stored token to every outgoing API request.

#### Scenario: JWT interceptor attaches token
- **WHEN** a stored JWT exists in the application's token store and an HTTP request is made
- **THEN** the request includes an `Authorization: Bearer <token>` header

#### Scenario: 401 response triggers logout
- **WHEN** the API returns HTTP 401
- **THEN** the Angular interceptor clears the stored token and redirects the user to the login page