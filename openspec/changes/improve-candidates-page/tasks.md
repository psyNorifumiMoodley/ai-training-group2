## 1. Backend — Candidate List Query Enhancements

- [ ] 1.1 Add `search`, `sortBy`, `sortDir`, and `status` query parameters to `GET /api/candidates` in `CandidateController`
- [ ] 1.2 Add a custom JPQL query method to `CandidateRepository` that accepts a search string (LIKE on name/email), sort field/direction, and optional assessment status filter
- [ ] 1.3 Update `CandidateService.listCandidates()` to accept and pass the new filter/sort parameters to the repository
- [ ] 1.4 Add `createdAt` field to `CandidateResponse` DTO

## 2. Backend — Candidate Detail and Assessments Endpoint

- [ ] 2.1 Add `GET /api/candidates/{id}` endpoint to `CandidateController` returning a single `CandidateResponse`
- [ ] 2.2 Add `getCandidateById(UUID id)` method to `CandidateService` (throws `NotFoundException` if not found)
- [ ] 2.3 Add `GET /api/candidates/{id}/assessments` endpoint to `CandidateController` returning a list of `AssessmentResponse`
- [ ] 2.4 Add `findByCandidateId(UUID candidateId)` query method to `AssessmentRepository`
- [ ] 2.5 Add `getCandidateAssessments(UUID candidateId)` method to `AssessmentService`
- [ ] 2.6 Add `PUT /api/candidates/{id}` and `DELETE /api/candidates/{id}` endpoints to `CandidateController` (Admin-only via `@PreAuthorize`)
- [ ] 2.7 Implement `updateCandidate` and `deleteCandidate` methods in `CandidateService`

## 3. Frontend — Candidates List Improvements

- [ ] 3.1 Update `UserService.getCandidates()` to accept `search`, `sortBy`, `sortDir`, and `status` parameters and pass them as query params
- [ ] 3.2 Add a search input (debounced 300ms) to `CandidateListComponent` wired to reload the list
- [ ] 3.3 Add clickable column headers (Name, Email, Registration Date) with sort indicators to the candidates table; toggling direction on re-click
- [ ] 3.4 Add an assessment status filter dropdown to the candidates list
- [ ] 3.5 Replace any inline Edit/Delete row buttons with a single View button per row
- [ ] 3.6 Update `CandidateResponse` model to include `createdAt: string`

## 4. Frontend — Candidate Detail Page

- [ ] 4.1 Create `CandidateDetailComponent` (standalone, OnPush) at `features/user-management/components/candidate-detail/`
- [ ] 4.2 Add route `/user-management/candidates/:id` to `user-management.routes.ts` pointing to `CandidateDetailComponent`
- [ ] 4.3 Display candidate profile fields (name, email, registration date) on the detail page
- [ ] 4.4 Add Edit button that opens the existing `CandidateFormComponent` in edit mode (PUT) — Admin only
- [ ] 4.5 Update `CandidateFormComponent` to support an edit mode: accept an existing `CandidateResponse` input and call `PUT /api/candidates/{id}` on submit
- [ ] 4.6 Add Delete button with a confirmation dialog that calls `DELETE /api/candidates/{id}` then navigates back to the list — Admin only
- [ ] 4.7 Add `getCandidateById(id: string)` and `getCandidateAssessments(id: string)` methods to `UserService`
- [ ] 4.8 Render the assessments list on the detail page showing status, time limit, and creation date; display an empty state when there are none
- [ ] 4.9 Hide Edit and Delete buttons for users with role `MARKER` (use auth service role check)
