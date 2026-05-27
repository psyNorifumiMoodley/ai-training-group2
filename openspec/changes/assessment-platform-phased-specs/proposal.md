## Why

The Developer Assessment Platform needs to be built collaboratively by a team of 4 developers working in parallel. Without a phased spec structure, work gets split by layer (frontend / backend) rather than by vertical slice — resulting in blocked dependencies, serialised delivery, and no meaningful collaboration across the stack.

## What Changes

- Introduce a phased delivery structure (Phase 0 through Phase 5) with each phase as a Jira Epic
- Each phase is decomposed into vertical slices (Stories) that 4 developers can work on simultaneously
- A contract ticket (Slice 0 in each phase) establishes shared stubs/interfaces that unblock all parallel slices before real implementations land
- Specs are created per-capability so individual Jira tickets can reference a single source of truth

## Capabilities

### New Capabilities

- `platform-foundation`: Phase 0 — project scaffolding, Spring Boot + Angular setup, Docker, CI pipeline, PostgreSQL + Liquibase baseline, JWT auth wiring, shared API contracts (error shape, base DTOs, HTTP client)
- `user-management`: Phase 1 — Admin registers candidates and markers; role-based access (ADMIN, MARKER, CANDIDATE); no self-registration
- `question-bank`: Phase 2 — Markers create and manage question banks; question types: MCQ (single/multi), text, doc, question groups; question bank ownership
- `assessment-generation`: Phase 3 — Generate tailored assessments per candidate; no-repeat question rule; doc question limit (max 1); invitation token (short-lived JWT); email distribution
- `assessment-experience`: Phase 4 — Token-gated assessment access; candidate completes assessment; auto-save responses; server-side timer; status machine (PENDING → IN_PROGRESS → SUBMITTED); MCQ auto-marking on submission
- `marking-and-results`: Phase 5 — Marker reviews text/doc responses; finalises marking; status transitions to MARKED; result email sent to candidate (no scores, feedback only)

### Modified Capabilities

## Impact

- New backend modules: `controller/`, `service/`, `repository/`, `domain/`, `dto/` per feature area
- New Angular feature modules under `src/app/features/` per phase
- Liquibase migrations for all schema changes — no direct DDL
- JWT security config affects all protected endpoints
- Email service integration required for Phase 3 (invitations) and Phase 5 (results)
- All 4 developers affected — phased structure defines how work is picked up and parallelised