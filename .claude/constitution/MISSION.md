# Mission

## What We're Building

The Developer Assessment Platform enables companies to design, distribute, and evaluate technical assessments for candidates — providing a clean, scalable foundation for modern technical hiring workflows.

## Core Goals

- **Assess effectively** — Support multiple question types (MCQ, text, document/code submission) that can be grouped, structured, and drawn from reusable question banks
- **Distribute securely** — Deliver assessments to candidates via signed email invitations with time-limited, token-gated access
- **Evaluate fairly** — Combine automated MCQ marking with structured manual review workflows for text and submission-based answers
- **Scale responsibly** — Build a domain model and architecture extensible enough to support automated code execution, multi-language submissions, and cheating detection in future iterations

## Who It Serves

| Role | Purpose |
|---|---|
| **Admin** | Registers candidates and markers, manages assessments, oversees results |
| **Marker / Reviewer** | Builds question banks, assigns assessments, reviews and marks submissions |
| **Candidate** | Receives invitations, completes assessments, views results after marking |

## Guiding Principles

- **No repeat questions** — The system tracks a candidate's full assessment history and ensures previously seen questions are excluded when generating new assessments
- **Server is the authority** — Time limits are enforced server-side; the frontend timer is a convenience only
- **One submission, one chance** — Candidates may not retake an assessment; integrity of results depends on this constraint
- **Markers control results** — Result notifications are only sent once a marker explicitly finalises marking, preventing premature disclosure
- **Liquibase owns the schema** — No direct database modifications; all changes go through versioned changesets

## Out of Scope (v1)

The following are intentionally deferred and should not be built in the initial release, but the architecture must remain extensible to support them:

- Candidate self-registration
- Automated code execution / sandboxed testing
- AI-generated content detection and cheating detection
- Multi-language coding execution (Java, Python, C# runners)
- OAuth2 / SSO authentication
