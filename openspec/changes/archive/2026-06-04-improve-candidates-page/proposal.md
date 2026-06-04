## Why

The current candidates list page provides only basic row-level Edit and Delete buttons with no way to view a candidate's full profile or their assessments. As the platform grows, admins need efficient navigation — search, sort, filter, and pagination — to manage candidates at scale, and a dedicated detail view to access all candidate information and actions in one place.

## What Changes

- Replace per-row Edit and Delete buttons with a single **View** button on the candidates list
- Add a **candidate detail view page** accessible via the View button, which surfaces the candidate's profile, Edit and Delete actions, and a list of all their assessments
- Add **search** to the candidates list (by name or email)
- Add **column sorting** to the candidates list (name, email, registration date)
- Add **filtering** to the candidates list (e.g., by assessment status or presence of assessments)
- Add **pagination** to the candidates list (page/size controls with total count display)
- Backend: expose a `GET /api/candidates/{id}/assessments` endpoint to retrieve all assessments for a given candidate

## Capabilities

### New Capabilities
- `candidate-detail-view`: Candidate detail page displaying profile info, Edit and Delete actions, and a list of all assessments assigned to that candidate

### Modified Capabilities
- `user-management`: Candidate list UI requirements updated to include search, column sorting, filtering, pagination, and replacement of inline Edit/Delete with a View button; backend `GET /api/candidates` updated to support search/sort/filter query parameters

## Impact

- **Frontend**: `features/candidates/` — candidates list component, new candidate detail component, candidates service (new API calls), candidates routes
- **Backend**: `CandidateController` — update `GET /api/candidates` to accept `search`, `sort`, `filter` query params; add `GET /api/candidates/{id}/assessments`; `CandidateService` and `AssessmentRepository` for the new query
- **API**: No breaking changes — new query parameters are additive; new endpoint is additive
