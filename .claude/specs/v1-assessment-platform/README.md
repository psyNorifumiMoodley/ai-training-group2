# Developer Assessment Platform — Specs

Specifications for the Developer Assessment Platform, organised by delivery phase.
Each file maps to a Jira Epic. Stories within each Epic are vertical slices owned by individual developers.

## Delivery Model

| Rule                            | Detail                                                                                            |
|---------------------------------|---------------------------------------------------------------------------------------------------|
| **Epic = Phase**                | Each phase is a Jira Epic                                                                         |
| **Story = Vertical Slice**      | Each story spans backend + frontend + tests for one user-facing capability                        |
| **Subtask = Individual Ticket** | Day-to-day work items within a slice                                                              |
| **Slice 0 = Contract Ticket**   | First story in each Phase 1–5 Epic; must merge to `main` before parallel slices begin             |
| **Board Rule**                  | All stories in a phase are In Progress simultaneously — never all 4 devs in the same story column |

## Phases

| File                                                                 | Phase   | Key Capabilities                                                                                       |
|----------------------------------------------------------------------|---------|--------------------------------------------------------------------------------------------------------|
| [phase-0-platform-foundation.md](phase-0-platform-foundation.md)     | Phase 0 | Project scaffolding, DB baseline, JWT auth, shared error shape, Angular HTTP bootstrap                 |
| [phase-1-user-management.md](phase-1-user-management.md)             | Phase 1 | Admin registers candidates & markers, role-based access, Angular user management UI                    |
| [phase-2-question-bank.md](phase-2-question-bank.md)                 | Phase 2 | Question banks, MCQ / text / doc / group questions, Angular question bank UI                           |
| [phase-3-assessment-generation.md](phase-3-assessment-generation.md) | Phase 3 | Assessment generation, no-repeat rule (per year), configurable doc limit, invitation token, email      |
| [phase-4-assessment-experience.md](phase-4-assessment-experience.md) | Phase 4 | Token-gated access, response saving (incl. groups), server-side timer, MCQ auto-marking                |
| [phase-5-marking-and-results.md](phase-5-marking-and-results.md)     | Phase 5 | Marking queue, auto-generated editable feedback, finalisation, feedback email, candidate feedback view |

## Key Business Rules (quick reference)

- **No self-registration** — candidates are always registered by an Admin or Marker
- **No-repeat questions** — scoped to the current calendar year; prior-year questions are eligible again
- **Doc question limit** — configurable via `assessment.doc-question-limit`; default 1
- **Invitation token expiry** — tied to the assessment session (`start_time + time_limit_minutes`), not a fixed TTL
- **Server-side timer** — the server is the authority; the frontend countdown is informational only
- **One submission only** — enforced by DB UNIQUE constraint on `submission(assessment_id)` and service-layer status check
- **Assessment status machine** — `PENDING → IN_PROGRESS → SUBMITTED → MARKED`
- **Feedback email, not results** — the candidate receives per-question written feedback only; no scores or marks
- **Finalisation gate** — Marker cannot finalise until all text/doc feedback entries are non-empty

## Source of Truth

Full change artifacts (proposal, design, tasks) live in:
`openspec/changes/assessment-platform-phased-specs/`
