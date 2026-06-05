## 1. Backend — Marker List Query Enhancements

- [x] 1.1 Add `search`, `sortBy`, and `sortDir` query parameters to `GET /api/markers` in `MarkerController`
- [x] 1.2 Add a `searchMarkers` custom JPQL query method to `AppUserRepository` that filters by `role = MARKER`, accepts a search string (LIKE on name/email), sort field/direction — use `CAST(:search AS String)` to avoid the PostgreSQL null-type issue
- [x] 1.3 Update `MarkerService.listMarkers()` to accept and pass the new search/sort parameters to the repository
- [x] 1.4 Add `createdAt` field to `MarkerResponse` DTO and populate it in `MarkerService`

## 2. Backend — Update and Delete Endpoints

- [x] 2.1 Add `PUT /api/markers/{id}` endpoint to `MarkerController` (Admin-only via `@PreAuthorize`)
- [x] 2.2 Implement `updateMarker(UUID id, MarkerRequest request)` in `MarkerService` — check email uniqueness, update name/email, return updated `MarkerResponse`
- [x] 2.3 Add `DELETE /api/markers/{id}` endpoint to `MarkerController` (Admin-only via `@PreAuthorize`)
- [x] 2.4 Implement `deleteMarker(UUID id)` in `MarkerService` — check for existing `Feedback` records referencing the marker and throw `ConflictException` if found, otherwise delete the `app_user` record
- [x] 2.5 Add `existsByMarkerId(UUID markerId)` or equivalent check to `FeedbackRepository` (or use `AppUserRepository`) to support the delete safety guard

## 3. Frontend — Markers List Improvements

- [x] 3.1 Update `UserService.getMarkers()` to accept `search`, `sortBy`, and `sortDir` parameters and pass them as query params
- [x] 3.2 Add `updateMarker(id: string, request: MarkerRequest)` and `deleteMarker(id: string)` methods to `UserService`
- [x] 3.3 Update `MarkerResponse` model in `user.model.ts` to include `createdAt: string`
- [x] 3.4 Add a debounced search input (300ms) to `MarkerListComponent` wired to reload the list on change
- [x] 3.5 Add clickable column headers (Name, Email, Registration Date) with sort indicators to the markers table; toggling direction on re-click
- [x] 3.6 Add Edit and Delete action buttons per marker row

## 4. Frontend — Inline Edit and Delete

- [x] 4.1 Update `MarkerFormComponent` to support an edit mode: accept an existing `MarkerResponse` as an optional input and call `PUT /api/markers/{id}` on submit
- [x] 4.2 Wire the Edit button in `MarkerListComponent` to open `MarkerFormComponent` in edit mode for the selected marker; refresh the list on save
- [x] 4.3 Wire the Delete button in `MarkerListComponent` to show an inline confirmation; on confirm call `DELETE /api/markers/{id}` and remove the marker from the list; display an error message if the API returns 409
