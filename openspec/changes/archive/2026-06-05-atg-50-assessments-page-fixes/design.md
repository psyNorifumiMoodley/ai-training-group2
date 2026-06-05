## Context

The Assessments list page (`/assessments`) is used by Admins and Markers to oversee all assessments. Currently it shows Role and Bank columns that are low-value at the list level, the date is not ISO-formatted, the table doesn't refresh after creation, and the action buttons are incomplete (no copy-link, no remind, Mark not visible to Admins). Additionally, the manual question selection flow ends with an inline confirmation that is inconsistent with the auto-generate modal confirmation.

The backend `AssessmentSummaryResponse` DTO does not include the invitation link, so "Copy link" requires a small additive backend change. The "Remind" action requires a new endpoint and email re-send.

## Goals / Non-Goals

**Goals:**
- Simplify the table by removing Role and Bank columns
- Format `assignedDate` as ISO-8601 date (`YYYY-MM-DD`) in the UI
- Auto-refresh the table after assessment creation (both flows)
- Add candidate name search and status filter above the table
- Complete the Actions column: Mark (for Admins too), Copy link, Remind
- Add `invitationLink` to `AssessmentSummaryResponse` (backend)
- Add `POST /api/assessments/{id}/remind` endpoint (backend)
- After manual selection, show a confirmation modal consistent with auto-generate

**Non-Goals:**
- Pagination changes (keep current 10-per-page)
- Backend filtering/search (client-side filter on the loaded page is sufficient for now)
- Any changes to the marking or submission flows
- Changing question count configuration

## Decisions

**Client-side search/filter vs. server-side**
The list is paginated to 10 items. For v1, apply search and status filter client-side on the current page's data rather than adding query params to the backend. This avoids backend changes and is sufficient for current data volumes. If the dataset grows large this should be revisited.

**Rationale for adding `invitationLink` to `AssessmentSummaryResponse`**
Rather than a separate endpoint per assessment (which would require N+1 calls), we add `invitationLink` directly to the list DTO. The link is already computed during assessment generation and stored as `invitationToken` on the entity. The backend simply composes `frontendBaseUrl + "/assessment/access/" + token` at list time.

**Remind endpoint re-uses existing `EmailService.sendInvitation`**
The remind action is strictly for PENDING assessments. It re-sends the same invitation email using the existing token (no new token generation). The endpoint validates status = PENDING before sending.

**Modal confirmation after manual selection**
After the user completes manual selection on `/assessments/generate` and the backend responds, navigate back to `/assessments` and emit an event (or use a query param `?confirmed=true`) to show a modal overlay. Alternatively, show the confirmation inline on the question-selection page using the existing `AssessmentConfirmationComponent`. Given the existing page already renders the component when `done()` is true, we enhance it to look more like a modal (centered card overlay) rather than inline. This avoids routing complexity.

**Admin mark access**
The backend already has `@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")` on the marking endpoints. The frontend currently shows "Mark" only when the user's role is MARKER. We remove the role gate on the frontend for the Mark button — it should appear for both ADMIN and MARKER when status is SUBMITTED or MARKED.

## Risks / Trade-offs

- **Client-side filter only covers the current page**: If the user is on page 2, the search only filters page 2 items. → Acceptable for v1; document the limitation. If needed, add server-side `?candidateName=` param later.
- **Invitation link in list DTO**: Adds a small payload overhead. → Negligible for typical list sizes.
- **Remind could spam candidates**: No rate-limiting in v1. → Accept; Markers are trusted actors.
