## Context

The markers list page was built to the same initial spec as the candidates list but has not received the same enhancements. Candidates now have search, sort, and CRUD from the detail page; markers have only paginated listing and registration. Admins need parity: find markers quickly and correct mistakes without deleting and re-registering.

Markers are stored directly in `app_user` (no separate `marker` entity or table), so all queries run against `app_user WHERE role = 'MARKER'`. `MarkerService` currently delegates to `AppUserRepository.findAllByRole()`.

## Goals / Non-Goals

**Goals:**
- `GET /api/markers` supports `search` (name/email LIKE), `sortBy`, `sortDir`
- `PUT /api/markers/{id}` allows updating a marker's name and email
- `DELETE /api/markers/{id}` removes a marker (blocked if they have marked assessments)
- `MarkerResponse` exposes `createdAt` for use as a sortable column
- Frontend gains debounced search, sortable headers, and inline Edit / Delete per row

**Non-Goals:**
- A separate marker detail page (edit/delete stay inline on the list, no navigation away)
- Password reset or credential management
- Marker activity audit history

## Decisions

### Search/sort query placement — `AppUserRepository` vs new interface
Because there is no `Marker` JPA entity, a `MarkerRepository` extending `JpaRepository<AppUser, UUID>` would duplicate `AppUserRepository`. The clean choice is to add a marker-specific `@Query` method directly to `AppUserRepository`, namespaced by convention (`searchMarkers`). This keeps the layer boundary intact (service → repository) without introducing a phantom entity.

The same `CAST(:search AS String)` pattern used on `CandidateRepository.search()` is required to avoid the PostgreSQL `lower(bytea)` error on null parameters.

### Edit mode — inline modal vs dedicated page
Candidates have a detail page; markers do not and do not need one (no assessments list or complex profile). Reusing the existing `MarkerFormComponent` in edit mode (accept an optional existing marker input, call `PUT` on submit) keeps the UX consistent with the candidate form pattern and avoids a new route.

### Delete safety check
A marker may have marked assessments (`Feedback` records referencing their `AppUser`). Deleting their `app_user` row would orphan those records. The service checks for existing feedback before deletion and throws `ConflictException` if any exist, returning HTTP 409 — same pattern as candidate deletion guarded by assessment FK.

### `createdAt` on `MarkerResponse`
`AppUser extends BaseEntity` which already has `createdAt`. Adding `createdAt` to `MarkerResponse` is a non-breaking additive change; existing API callers that ignore unknown fields are unaffected.

## Risks / Trade-offs

- **`AppUserRepository` growing large** → Acceptable for now; if marker-specific queries multiply in the future, a `MarkerRepository` view or a JPA filter can be introduced then.
- **Inline delete with no undo** → Mitigation: frontend confirmation dialog before calling the API (same pattern as candidate delete).
- **Sort on `createdAt` may be slow without index** → `BaseEntity` `createdAt` column should have an index; if not, one can be added via Liquibase changeset in a follow-up.
