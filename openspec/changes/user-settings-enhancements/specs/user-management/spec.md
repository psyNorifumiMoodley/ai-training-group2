## ADDED Requirements

### Requirement: Role-Based Access Enforcement for Self-Update Endpoints
The system SHALL ensure that self-update endpoints (`/api/users/me/*`) are accessible only to the currently authenticated user and cannot be used to update another user's record.

#### Scenario: User can only update their own record
- **WHEN** an authenticated marker or admin calls any `/api/users/me/*` endpoint
- **THEN** the system resolves the target user from the JWT `sub` claim, never from a path parameter

#### Scenario: Candidate cannot access self-update endpoints
- **WHEN** a user with role `CANDIDATE` calls any `/api/users/me/*` endpoint
- **THEN** the response is HTTP 403
