## 1. Database

- [ ] 1.1 Add Liquibase changeset to add `theme_preference` column (`VARCHAR NOT NULL DEFAULT 'LIGHT'`) to `app_user`

## 2. Backend — Domain & DTOs

- [ ] 2.1 Add `ThemePreference` enum (`LIGHT`, `DARK`) to the domain layer
- [ ] 2.2 Add `themePreference` field to `AppUser` entity with `@Enumerated(EnumType.STRING)`
- [ ] 2.3 Create `UserProfileResponse` record: `id`, `name`, `surname`, `email`, `role`, `themePreference`
- [ ] 2.4 Create `UpdateProfileRequest` record: `name`, `surname`, `email` with `@NotBlank` / `@Email` validation
- [ ] 2.5 Create `ChangePasswordRequest` record: `currentPassword`, `newPassword`, `confirmPassword` with `@NotBlank` and `@Size(min=8)` on `newPassword`
- [ ] 2.6 Create `UpdateThemeRequest` record: `theme` (`ThemePreference`) with `@NotNull`

## 3. Backend — Repository & Service

- [ ] 3.1 Add `existsByEmailAndIdNot(String email, UUID id)` query method to `AppUserRepository` (for duplicate-email check on profile update)
- [ ] 3.2 Create `UserSettingsService` with methods: `getProfile`, `updateProfile`, `changePassword`, `updateTheme` — all resolve the target user from the JWT principal
- [ ] 3.3 Implement `getProfile`: load `AppUser` by ID from principal, map to `UserProfileResponse`
- [ ] 3.4 Implement `updateProfile`: check email uniqueness (409 if taken by another user), persist updated fields
- [ ] 3.5 Implement `changePassword`: verify `currentPassword` against stored hash (400 if wrong), verify `newPassword == confirmPassword` (400 if mismatch), encode and persist new password
- [ ] 3.6 Implement `updateTheme`: set `themePreference` on `AppUser`, persist

## 4. Backend — Controller & Security

- [ ] 4.1 Create `UserSettingsController` mapped to `/api/users/me` with `GET`, `PATCH /profile`, `PATCH /password`, `PATCH /theme` endpoints
- [ ] 4.2 Restrict all `/api/users/me/**` endpoints to roles `ADMIN` and `MARKER` in Spring Security config (deny `CANDIDATE`)
- [ ] 4.3 Inject the authenticated principal (user UUID) via `@AuthenticationPrincipal` — never accept user ID from path/query params

## 5. Frontend — Models & Service

- [ ] 5.1 Create `UserProfile` TypeScript interface: `id`, `name`, `surname`, `email`, `role`, `themePreference`
- [ ] 5.2 Create `UserSettingsService` in `core/` with methods: `getProfile()`, `updateProfile()`, `changePassword()`, `updateTheme()` — each calling the appropriate `/api/users/me/*` endpoint
- [ ] 5.3 Create `ThemeService` in `core/` that reads `themePreference` from the profile response, applies `dark-theme` / `light-theme` class to `document.body`, and caches the last-known theme in `localStorage` to prevent flicker on load

## 6. Frontend — Settings Page Components

- [ ] 6.1 Update the existing settings component to display three sections: Profile, Security, and Appearance (use tabs or card sections)
- [ ] 6.2 Build the **Profile panel**: reactive form with `name`, `surname`, `email` fields; `OnPush`; calls `UserSettingsService.updateProfile()` on submit; displays success/error feedback
- [ ] 6.3 Build the **Security panel**: reactive form with `currentPassword`, `newPassword`, `confirmPassword` fields; cross-field validator for password match; `OnPush`; calls `UserSettingsService.changePassword()` on submit; clears fields on success
- [ ] 6.4 Build the **Appearance panel**: toggle component (light/dark); `OnPush`; on toggle calls `UserSettingsService.updateTheme()` and `ThemeService` to apply immediately
- [ ] 6.5 Load the current user profile via `UserSettingsService.getProfile()` on settings page init and pre-populate the profile form and theme toggle with current values
- [ ] 6.6 Apply the saved theme on app bootstrap: `ThemeService` reads `localStorage` fallback first, then syncs with `GET /api/users/me` after auth

## 7. Frontend — Route Guard

- [ ] 7.1 Ensure the settings route is guarded so only `ADMIN` and `MARKER` roles can access it; redirect candidates elsewhere

## 8. Tailwind Dark Mode Config

- [ ] 8.1 Set `darkMode: 'class'` in `tailwind.config.js` if not already set, so `dark:` variants respond to the body class applied by `ThemeService`
