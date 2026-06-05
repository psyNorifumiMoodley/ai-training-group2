## Why

The markers list page is significantly behind the candidates list in capability — it has no search, no column sorting, and no way to edit or delete a marker once registered. Admins currently have no recourse if a marker's name or email needs correcting, and the list becomes harder to navigate as the number of markers grows.

## What Changes

- `GET /api/markers` gains `search`, `sortBy`, and `sortDir` query parameters so the backend filters and sorts results server-side
- `PUT /api/markers/{id}` is added to allow an Admin to update a marker's name and email
- `DELETE /api/markers/{id}` is added to allow an Admin to remove a marker (guarded against deletion if the marker has submitted markings)
- `MarkerResponse` gains a `createdAt` field for use as a sortable column
- The marker list UI gains a debounced search input, clickable sortable column headers (Name, Email, Registration Date), and inline Edit / Delete actions per row

## Capabilities

### New Capabilities
- *(none)*

### Modified Capabilities
- `user-management`: The "Admin Lists Markers" requirement expands to include search/sort parameters, and new requirements cover updating and deleting markers. The "Angular User Management UI" requirement gains marker-specific scenarios for search, sorting, inline edit, and delete.

## Impact

- **Backend**: `MarkerController`, `MarkerService`, `MarkerResponse` DTO; new custom JPQL query on `AppUserRepository` (or a new `MarkerRepository`)
- **Frontend**: `MarkerListComponent`, `UserService` (`getMarkers`, `updateMarker`, `deleteMarker`), `MarkerFormComponent` (edit mode), `user.model.ts`
- **No breaking changes** — existing `GET /api/markers` callers are unaffected; new params are all optional with existing defaults
