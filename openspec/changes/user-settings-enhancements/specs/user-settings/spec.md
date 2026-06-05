## ADDED Requirements

### Requirement: Authenticated User Can View Their Own Profile
The system SHALL provide a `GET /api/users/me` endpoint that returns the authenticated user's current profile information and preferences.

#### Scenario: Successful profile fetch
- **WHEN** an authenticated marker or admin sends `GET /api/users/me`
- **THEN** the response is HTTP 200 with `{ id, name, surname, email, role, themePreference }`

#### Scenario: Unauthenticated request is rejected
- **WHEN** `GET /api/users/me` is called without a valid JWT
- **THEN** the response is HTTP 401

---

### Requirement: Authenticated User Can Update Their Profile Info
The system SHALL allow an authenticated marker or admin to update their own `name`, `surname`, and `email` via `PATCH /api/users/me/profile`.

#### Scenario: Successful profile update
- **WHEN** an authenticated user sends `PATCH /api/users/me/profile` with valid `name`, `surname`, and `email`
- **THEN** the response is HTTP 200 with the updated profile fields; the `app_user` record is persisted with the new values

#### Scenario: Email already in use is rejected
- **WHEN** an authenticated user sends `PATCH /api/users/me/profile` with an `email` already belonging to a different `app_user`
- **THEN** the response is HTTP 409 with a message indicating the email is already in use

#### Scenario: Missing required field is rejected
- **WHEN** an authenticated user sends `PATCH /api/users/me/profile` with a blank `name`, `surname`, or `email`
- **THEN** the response is HTTP 400 with a validation error identifying the missing field

#### Scenario: Unauthenticated request is rejected
- **WHEN** `PATCH /api/users/me/profile` is called without a valid JWT
- **THEN** the response is HTTP 401

---

### Requirement: Authenticated User Can Change Their Password
The system SHALL allow an authenticated marker or admin to change their own password via `PATCH /api/users/me/password`, requiring verification of the current password.

#### Scenario: Successful password change
- **WHEN** an authenticated user sends `PATCH /api/users/me/password` with a correct `currentPassword`, a valid `newPassword`, and a matching `confirmPassword`
- **THEN** the response is HTTP 204; the stored password hash is updated to the new password

#### Scenario: Incorrect current password is rejected
- **WHEN** an authenticated user sends `PATCH /api/users/me/password` with an incorrect `currentPassword`
- **THEN** the response is HTTP 400 with a message indicating the current password is wrong

#### Scenario: New password and confirm password mismatch is rejected
- **WHEN** `newPassword` and `confirmPassword` do not match
- **THEN** the response is HTTP 400 with a message indicating the values do not match

#### Scenario: New password too short is rejected
- **WHEN** `newPassword` is fewer than 8 characters
- **THEN** the response is HTTP 400 with a validation error on the `newPassword` field

#### Scenario: Unauthenticated request is rejected
- **WHEN** `PATCH /api/users/me/password` is called without a valid JWT
- **THEN** the response is HTTP 401

---

### Requirement: Authenticated User Can Set Their Theme Preference
The system SHALL allow an authenticated marker or admin to set their theme preference (`LIGHT` or `DARK`) via `PATCH /api/users/me/theme`, persisted in the database.

#### Scenario: Successful theme update to dark
- **WHEN** an authenticated user sends `PATCH /api/users/me/theme` with `{ "theme": "DARK" }`
- **THEN** the response is HTTP 200; the `theme_preference` column for the user is updated to `DARK`

#### Scenario: Successful theme update to light
- **WHEN** an authenticated user sends `PATCH /api/users/me/theme` with `{ "theme": "LIGHT" }`
- **THEN** the response is HTTP 200; the `theme_preference` column for the user is updated to `LIGHT`

#### Scenario: Invalid theme value is rejected
- **WHEN** `PATCH /api/users/me/theme` is sent with a value outside `LIGHT` or `DARK`
- **THEN** the response is HTTP 400 with a validation error

#### Scenario: Unauthenticated request is rejected
- **WHEN** `PATCH /api/users/me/theme` is called without a valid JWT
- **THEN** the response is HTTP 401

---

### Requirement: Angular Settings Page Displays Three Panels
The Angular settings page SHALL display three panels accessible to markers and admins: Profile, Security, and Appearance.

#### Scenario: Settings page renders all three panels
- **WHEN** an authenticated marker or admin navigates to the settings page
- **THEN** three distinct sections are shown: Profile (name, surname, email), Security (change password), and Appearance (theme toggle)

#### Scenario: Candidate cannot access settings
- **WHEN** a user with role `CANDIDATE` navigates to the settings route
- **THEN** they are redirected away (403 or redirect to their own area)

---

### Requirement: Angular Profile Panel Saves Changes
The Angular profile panel SHALL submit updated name, surname, and email to `PATCH /api/users/me/profile` and display feedback.

#### Scenario: Successful profile save shows confirmation
- **WHEN** an authenticated user edits profile fields and clicks Save
- **THEN** the form is submitted to the API; on success a confirmation message is shown and the displayed name/email is updated

#### Scenario: Duplicate email shows inline error
- **WHEN** the API returns HTTP 409 on profile save
- **THEN** an inline error message is displayed on the email field indicating the email is already in use

#### Scenario: Validation errors shown before submission
- **WHEN** required fields are empty when the user attempts to save
- **THEN** inline validation errors appear and no API call is made

---

### Requirement: Angular Security Panel Changes Password
The Angular security panel SHALL submit the password change form to `PATCH /api/users/me/password` and clear the fields on success.

#### Scenario: Successful password change clears form
- **WHEN** an authenticated user submits a valid password change form
- **THEN** the API call succeeds with HTTP 204; all password fields are cleared and a success message is shown

#### Scenario: Wrong current password shows error
- **WHEN** the API returns HTTP 400 with a wrong-current-password message
- **THEN** an error is displayed on the current password field

#### Scenario: Mismatched confirm password shown before submission
- **WHEN** `newPassword` and `confirmPassword` differ at submission time
- **THEN** a validation error appears on the confirm field and no API call is made

---

### Requirement: Angular Appearance Panel Applies and Persists Theme
The Angular appearance panel SHALL toggle between light and dark themes, persist the preference via `PATCH /api/users/me/theme`, and apply the theme globally.

#### Scenario: Toggling theme applies immediately
- **WHEN** an authenticated user toggles the theme switch in settings
- **THEN** the dark or light CSS class is applied to the document body immediately without a page reload

#### Scenario: Theme preference persisted to backend
- **WHEN** an authenticated user changes the theme toggle
- **THEN** `PATCH /api/users/me/theme` is called with the new value; a success or silent failure message is shown

#### Scenario: Theme restored on next login
- **WHEN** an authenticated user logs in after previously saving a theme preference
- **THEN** the `GET /api/users/me` response is used to apply the saved theme on load
