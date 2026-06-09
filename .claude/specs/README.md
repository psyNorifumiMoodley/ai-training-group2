# Developer Assessment Platform — Specs

Specifications for the Developer Assessment Platform, organised by version and delivery phase.
Each file maps to a Jira Epic. Stories within each Epic are vertical slices owned by individual developers.

## Delivery Model

| Rule                            | Detail                                                                                            |
|---------------------------------|---------------------------------------------------------------------------------------------------|
| **Epic = Phase**                | Each phase is a Jira Epic                                                                         |
| **Story = Vertical Slice**      | Each story spans backend + frontend + tests for one user-facing capability                        |
| **Subtask = Individual Ticket** | Day-to-day work items within a slice                                                              |
| **Slice 0 = Contract Ticket**   | First story in each Phase Epic; must merge to `main` before parallel slices begin                 |
| **Board Rule**                  | All stories in a phase are In Progress simultaneously — never all 4 devs in the same story column |

---

## v1 — Assessment Platform

Full spec: [v1-assessment-platform/](v1-assessment-platform/README.md)

| File                                                                                          | Phase   | Key Capabilities                                                                                       |
|-----------------------------------------------------------------------------------------------|---------|--------------------------------------------------------------------------------------------------------|
| [phase-0-platform-foundation.md](v1-assessment-platform/phase-0-platform-foundation.md)       | Phase 0 | Project scaffolding, DB baseline, JWT auth, shared error shape, Angular HTTP bootstrap                 |
| [phase-1-user-management.md](v1-assessment-platform/phase-1-user-management.md)               | Phase 1 | Admin registers candidates & markers, role-based access, Angular user management UI                    |
| [phase-2-question-bank.md](v1-assessment-platform/phase-2-question-bank.md)                   | Phase 2 | Question banks, MCQ / text / doc / group questions, Angular question bank UI                           |
| [phase-3-assessment-generation.md](v1-assessment-platform/phase-3-assessment-generation.md)   | Phase 3 | Assessment generation, no-repeat rule (per year), configurable doc limit, invitation token, email      |
| [phase-4-assessment-experience.md](v1-assessment-platform/phase-4-assessment-experience.md)   | Phase 4 | Token-gated access, response saving (incl. groups), server-side timer, MCQ auto-marking                |
| [phase-5-marking-and-results.md](v1-assessment-platform/phase-5-marking-and-results.md)       | Phase 5 | Marking queue, auto-generated editable feedback, finalisation, feedback email, candidate feedback view |

---

## v2 — Automated Testing

Full spec: [v2-automated-testing/](v2-automated-testing/)
OpenSpec change: `openspec/changes/v2-automated-testing/`

| File                                                                                            | Phase   | Key Capabilities                                                                            |
|-------------------------------------------------------------------------------------------------|---------|---------------------------------------------------------------------------------------------|
| [phase-6-coding-question.md](v2-automated-testing/phase-6-coding-question.md)                  | Phase 6 | New `coding_question` subtype, test case management, doc_question soft deprecation, UI      |
| [phase-7-execution-engine.md](v2-automated-testing/phase-7-execution-engine.md)                | Phase 7 | Docker execution engine, Java / Python / C# language runners, container lifecycle           |
| [phase-8-grading-and-results.md](v2-automated-testing/phase-8-grading-and-results.md)          | Phase 8 | Async auto-grading on submission, result query endpoints, candidate and marker result views |

---

## Key Business Rules (quick reference)

- **No self-registration** — candidates are always registered by an Admin or Marker
- **No-repeat questions** — scoped to the current calendar year; prior-year questions are eligible again
- **Doc/coding question limit** — configurable via `assessment.doc-question-limit`; default 1; applies to both `doc_question` (legacy) and `coding_question` rows
- **Invitation token expiry** — tied to the assessment session (`start_time + time_limit_minutes`), not a fixed TTL
- **Server-side timer** — the server is the authority; the frontend countdown is informational only
- **One submission only** — enforced by DB UNIQUE constraint on `submission(assessment_id)` and service-layer status check
- **Assessment status machine** — `PENDING → IN_PROGRESS → SUBMITTED → MARKED`
- **Feedback email, not results** — the candidate receives per-question written feedback only; no scores or marks
- **Finalisation gate** — Marker cannot finalise until all text/doc feedback entries are non-empty
- **doc_question is deprecated** — existing rows remain valid; new creation returns HTTP 410

## Source of Truth

Full change artifacts (proposal, design, tasks) live in:
- v1: `openspec/changes/assessment-platform-phased-specs/`
- v2: `openspec/changes/v2-automated-testing/`
