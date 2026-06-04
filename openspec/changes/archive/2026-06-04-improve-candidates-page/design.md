## Context

The candidates list lives in `dap-frontend/src/app/features/user-management/components/candidate-list/`. It renders a paginated table with Name and Email columns and currently has no search, sort, or filter. Each row shows no action buttons (Edit/Delete were removed in a prior pass; the form is opened via a top-level "Register candidate" button). The backend `GET /api/candidates` accepts only `page` and `size`; the `CandidateController` and `CandidateService` have no search or sort logic. There is no route or component for viewing a single candidate, and no endpoint that returns a candidate's assessments.

## Goals / Non-Goals

**Goals:**
- Add a View button per candidate row; move Edit and Delete into the detail view
- Add server-side search (name or email), column sorting, and filtering to the candidates list
- Add client-side pagination controls (already partially in place; complete with total-count display)
- Add a candidate detail page that shows profile info, Edit/Delete actions, and a list of all assessments assigned to that candidate
- Add `GET /api/candidates/{id}/assessments` backend endpoint

**Non-Goals:**
- Inline editing within the list table
- Bulk operations (delete many, export)
- Assessment creation from the candidate detail page (that belongs to the assessments feature)
- Full-text search across assessment content

## Decisions

### 1. Server-side search/sort/filter via query parameters
`GET /api/candidates` will accept `search` (partial match on name or email), `sortBy` (field name, default `name`), `sortDir` (`asc`/`desc`, default `asc`), and `status` (filter by whether the candidate has at least one assessment in a given status). Implementation uses a custom `@Query` JPQL method in `CandidateRepository` with `LIKE` for search and `LOWER()` for case-insensitive matching. Spring Data `Sort` is passed in from `CandidateService`.

**Alternative considered:** Spring Data `Specification` (JPA Criteria API). Rejected — the query is simple enough that a parameterised JPQL is more readable and avoids the boilerplate of a `Specification` class.

### 2. Candidate detail route at `/user-management/candidates/:id`
A new standalone `CandidateDetailComponent` is added to the `user-management` feature routes. The list's View button navigates to this route. Edit opens the existing `CandidateFormComponent` as an inline modal within the detail page (reusing the existing form). Delete triggers a confirmation dialog then navigates back to the list.

**Alternative considered:** A slide-out drawer/panel within the list page. Rejected — a dedicated route gives a stable URL, allows deep-linking, and keeps the list component focused.

### 3. Assessments list on the detail page via `GET /api/candidates/{id}/assessments`
A new endpoint returns all `AssessmentResponse` records for a given candidate. The `AssessmentService` already has list logic; a new `findByCandidateId` repository method is sufficient. The frontend calls this from the detail component on load via a new method on `UserService` (or a dedicated `CandidateService` if the service grows large enough — use `UserService` for now to avoid premature fragmentation).

### 4. Search debounce on the frontend
The search input is a reactive form control with a `debounceTime(300)` pipe before triggering `loadCandidates()`. This avoids a request per keystroke.

### 5. Column sort via clickable headers
The list component tracks `sortBy` and `sortDir` signals. Clicking a sortable column header toggles direction if already sorted by that column, or sets a new column. These feed into the `getCandidates()` service call as query params.

### 6. CandidateResponse extended with `createdAt`
The detail view and list sort-by-date both need the registration date. `CandidateResponse` gains a `createdAt: string` (ISO-8601) field sourced from `BaseEntity.createdAt`. No schema migration needed — the column already exists.

## Risks / Trade-offs

- **LIKE query performance on large datasets** → Mitigation: the candidate table is expected to stay in the thousands range for v1; a GIN index or full-text search can be added later if needed. An index on `app_user.name` and `app_user.email` covers the LIKE prefix case adequately.
- **Reusing CandidateFormComponent for edit in detail view** → The form currently only handles creation (POST). It needs a mode switch (create vs. edit) with a PUT call. This is contained within the form component and does not affect the list. Risk is low — the form is small.
- **UserService growing** → Adding a `getCandidateAssessments` method keeps it in UserService for now. If the service exceeds ~5 methods per entity, split into `CandidateService` / `MarkerService` in a follow-up.

## Migration Plan

1. Deploy backend changes first (additive: new query params are optional, new endpoint is new).
2. Deploy frontend — the list falls back gracefully if query params are omitted (defaults apply server-side).
3. No data migration or Liquibase changeset required.
4. Rollback: revert backend to previous jar; frontend reverts to previous build. No DB state change.

## Open Questions

- Should the candidate detail page show only assessments or also allow sending/resending invitation emails? (Out of scope for this change — defer.)
- Should Markers be able to edit/delete candidates from the detail page, or only Admins? (Current RBAC grants both roles access to `GET /api/candidates`; Edit/Delete should remain Admin-only — enforce with `@PreAuthorize` on the update/delete endpoints and hide the buttons on the frontend via role check.)
