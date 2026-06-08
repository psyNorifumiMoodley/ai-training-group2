## Why

The settings page for markers and admins currently exists as a placeholder with no functional options. Users cannot update their own credentials, personal information, or UI preferences, forcing admin intervention for routine self-service tasks.

## What Changes

- **Change password**: Markers and admins can update their own password directly from settings, with current-password verification and confirmation.
- **Profile info editing**: Markers and admins can edit their display name, surname, and email address from settings.
- **Theme / appearance**: A light/dark mode toggle stored per user, applied globally across the app on next load and persisted in the backend.

## Capabilities

### New Capabilities

- `user-settings`: Settings page functionality covering password change, profile info editing, and theme preference for marker and admin roles.

### Modified Capabilities

- `user-management`: The existing user management spec may need to reflect that a user can now self-update certain fields (name, email, password) — previously admin-only.

## Impact

- **Backend**: New endpoint(s) under `/api/users/me` for self-update (profile info, password change, theme preference). Spring Security must ensure users can only update their own record.
- **Frontend**: Settings feature module — new components for each tab/section (profile, security, appearance). Theme toggle wires into a global theme service.
- **Database**: A new `theme_preference` column on `app_user` (via Liquibase changeset). No schema changes needed for name/email/password — those columns already exist.
- **Auth**: Password change requires the current password to be validated server-side before updating.
