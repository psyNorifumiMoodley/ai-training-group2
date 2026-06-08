## Context

The platform has a settings route visible to markers and admins, but it renders no functional content. User self-service (password change, profile updates, appearance preferences) currently requires admin intervention or direct DB access. This design adds three self-contained settings panels to the existing settings page without altering the admin user-management flow.

Current state:
- `app_user` table holds `name`, `surname`, `email`, `password_hash`, `role`
- No theme preference column exists yet
- The settings page/component exists in the frontend but has no form or API calls
- No `/api/users/me` endpoints exist

## Goals / Non-Goals

**Goals:**
- Allow any authenticated marker or admin to change their own password (with current-password verification)
- Allow any authenticated marker or admin to update their own name, surname, and email
- Persist a light/dark theme preference per user and apply it globally on load
- Keep changes fully self-contained — no changes to other users' records

**Non-Goals:**
- Admin editing another user's password or profile (that remains in user management)
- Profile photo / avatar upload
- Two-factor authentication
- Role changes via settings
- Candidate access to settings

## Decisions

### 1. Single `/api/users/me` resource with targeted sub-actions

**Decision**: Expose three endpoints under the authenticated user's own context:
- `GET /api/users/me` — returns current profile info + theme preference
- `PATCH /api/users/me/profile` — updates name, surname, email
- `PATCH /api/users/me/password` — accepts `currentPassword` + `newPassword`
- `PATCH /api/users/me/theme` — accepts `theme` enum (`LIGHT` | `DARK`)

**Rationale**: Separate endpoints keep validation logic isolated (password change needs current-password check; profile update does not). A single `PUT /api/users/me` would require conditional logic on which fields changed. `PATCH` communicates partial update semantics correctly.

**Alternative considered**: PUT with all fields in one payload — rejected because password and profile updates have different security requirements and UI flows.

### 2. Theme preference stored in the database (not localStorage)

**Decision**: Add a `theme_preference` column (`VARCHAR`, default `LIGHT`) to `app_user` via Liquibase.

**Rationale**: Persisting in the DB means the theme follows the user across devices and browsers. localStorage is convenient but isolated per device — acceptable for a simple toggle but inconsistent with the platform's data model, where all user state lives in the DB.

**Alternative considered**: localStorage only — rejected because it does not roam and would be lost on browser data clear.

### 3. Angular theme service applies theme class on `<body>`

**Decision**: A singleton `ThemeService` in `core/` reads the user's preference from the `GET /api/users/me` response on login/page load and applies a CSS class (`dark-theme` or `light-theme`) on the document body. Tailwind's `dark:` variant or a custom CSS variable strategy handles styling.

**Rationale**: Centralising theme application in a service prevents component-level duplication. Body-class approach is compatible with Tailwind's `darkMode: 'class'` config.

### 4. Password change uses current-password re-verification (no email token)

**Decision**: The `PATCH /api/users/me/password` endpoint accepts `{ currentPassword, newPassword, confirmPassword }`. The service layer verifies `currentPassword` against the stored hash before updating.

**Rationale**: Prevents account takeover if a session token is stolen but the attacker doesn't know the current password. Email-link reset flow is out of scope for v1 self-service (it is a separate concern).

## Risks / Trade-offs

- **Email uniqueness on profile update** → If a user changes their email to one already in use, the DB unique constraint will throw. The service must check for duplicates before updating and return a clear 409 Conflict. Mitigation: explicit service-layer duplicate check.
- **JWT still carries old email after profile update** → The JWT `sub` is the user UUID, not email, so this is safe. The frontend should re-fetch the current user profile after a successful update to keep displayed info current.
- **Theme flicker on initial load** → If the theme class is applied after Angular bootstraps, there may be a brief flash of unstyled/wrong-theme content. Mitigation: store the last-known theme in localStorage as a cache; apply it immediately on load, then sync with the server response.
- **Concurrent password change sessions** → No session invalidation on password change in v1. Existing tokens remain valid until expiry. Acceptable for v1 given short JWT lifetimes.

## Migration Plan

1. Apply Liquibase changeset adding `theme_preference` column (default `LIGHT`, NOT NULL) — safe, no data migration needed
2. Deploy backend with new `/api/users/me/*` endpoints
3. Deploy frontend settings panels
4. No rollback complexity — new column has a default; removing endpoints is non-breaking to existing consumers
