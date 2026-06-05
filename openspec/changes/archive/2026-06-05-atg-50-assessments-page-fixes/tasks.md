## 1. Backend — DTO and Endpoint Changes

- [x] 1.1 Add `invitationLink` field to `AssessmentSummaryResponse` record (compute as `frontendBaseUrl + "/assessment/access/" + invitationToken`)
- [x] 1.2 Update `MarkingService.listAssessments` to populate `invitationLink` on each `AssessmentSummaryResponse`
- [x] 1.3 Add `sendReminder(String toEmail, String candidateName, String invitationLink)` to the `EmailService` interface and implement it in `EmailServiceImpl` with `@Async`, its own subject ("Reminder: You have a pending technical assessment"), and reminder-specific body text
- [x] 1.4 Add `remind(UUID assessmentId)` method to `AssessmentService` — validates status is PENDING, sends reminder email via `EmailService.sendReminder`
- [x] 1.5 Add `POST /api/assessments/{id}/remind` endpoint to `AssessmentController` with `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")`

## 2. Frontend — Model and Service Updates

- [x] 2.1 Add `invitationLink: string` to the `Assessment` interface in `assessment.model.ts`
- [x] 2.2 Add `remindCandidate(assessmentId: string): Observable<void>` to `AssessmentService` calling `POST /api/assessments/{id}/remind`
- [x] 2.3 Update `AssessmentService.getAssessments` to pass through `invitationLink` from the API response

## 3. Frontend — Assessment List Table

- [x] 3.1 Remove the Role and Bank `<th>` and `<td>` columns from `assessment-list.component.html`
- [x] 3.2 Format `assignedDate` as ISO-8601 (`YYYY-MM-DD`) — use a `DatePipe` or string slice in the template
- [x] 3.3 Remove the `role` and `bankName` fields from the `Assessment` interface (if no longer used)
- [x] 3.4 Add search input and status filter dropdown above the table in `assessment-list.component.html`
- [x] 3.5 Add `searchText` signal and `statusFilter` signal to `assessment-list.component.ts`
- [x] 3.6 Add `filteredAssessments` computed signal that filters `assessments()` by `searchText` and `statusFilter`
- [x] 3.7 Update template `@for` loop to iterate over `filteredAssessments()` instead of `assessments()`

## 4. Frontend — Actions Column

- [x] 4.1 Remove any MARKER-only role guard from the Mark button so both ADMIN and MARKER see it for SUBMITTED/MARKED assessments
- [x] 4.2 Add "Copy link" button to the actions cell — visible when status is PENDING or IN_PROGRESS; copies `a.invitationLink` to clipboard and shows brief "Copied!" state
- [x] 4.3 Add "Remind" button to the actions cell — visible when status is PENDING; calls `assessmentService.remindCandidate(a.id)` and shows success/error toast

## 5. Frontend — Table Auto-Refresh

- [x] 5.1 In `AssessmentGenerateComponent`, emit a new `assessmentCreated` output event after a successful auto-generate response
- [x] 5.2 In `AssessmentListComponent`, listen to `(assessmentCreated)` on `<dap-assessment-generate>` and call `loadPage(0)` to refresh the table
- [x] 5.3 In `QuestionSelectionComponent`, after successful submission navigate to `/assessments` — the list component's `ngOnInit` will reload the table

## 6. Frontend — Manual Selection Confirmation Modal

- [x] 6.1 In `question-selection.component.html`, wrap the `AssessmentConfirmationComponent` in a full-screen overlay (`fixed inset-0 z-50 flex items-center justify-center bg-black/50`) that is shown when `done()` is true
- [x] 6.2 Pass `questionCount` as `10` (constant) to `AssessmentConfirmationComponent` in the question-selection template
- [x] 6.3 Add a "Done" button below the confirmation card that navigates to `/assessments`
