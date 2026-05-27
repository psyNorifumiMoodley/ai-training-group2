## Context

Four developers are building a Developer Assessment Platform from scratch. The risk without a structured delivery model is layer-splitting — all backend work lands in one PR, all frontend in another, and no single developer owns an end-to-end feature. This design establishes the collaborative delivery model: how phases are structured, how slices are assigned, and what rules govern the board.

The platform itself is defined in CLAUDE.md (Spring Boot 3.x / Angular 20 / PostgreSQL / JWT). This design doc is about *how the work is organised and delivered*, not the technical internals of each feature.

## Goals / Non-Goals

**Goals:**
- Define a phase structure (Phase 0–5) that maps directly to Jira Epics
- Define vertical slice structure within each phase (Stories) that 4 devs can own simultaneously
- Establish the contract ticket pattern so slices can run in parallel without hard blocking dependencies
- Provide board rules that detect when collaboration has broken down
- Produce specs per capability that become the source of truth for Jira ticket acceptance criteria

**Non-Goals:**
- Define the implementation details of each feature (those live in the per-capability specs)
- Prescribe which developer owns which slice
- Define CI/CD pipeline specifics (those belong in Phase 0 tasks)

## Decisions

### Decision 1: Phase = Epic, Story = Vertical Slice, Subtask = Individual Ticket

**Rationale:** Jira's three-level hierarchy maps cleanly to delivery phases. Epics are large enough to represent a phase of work (1–3 sprints). Stories within an epic represent vertical slices — a thin slice of the user-facing capability that spans backend API + frontend UI + tests. Subtasks are the day-to-day tickets within a slice.

**Alternative considered:** Feature-team model (backend epic, frontend epic). Rejected — it creates integration bottlenecks and prevents developers from owning a full user journey.

---

### Decision 2: Contract Ticket (Slice 0) in Every Phase 1–5 Epic

**Rationale:** Parallel slice work requires shared interfaces. Without a contract ticket, Slice B can't start until Slice A's API is merged. The contract ticket is the first story in each phase epic. It produces:
- Backend: Controller stubs with hardcoded/empty responses (compiles and returns 200)
- Angular: Service stubs with Observable<never> or mock data
- Shared DTOs / request-response record types

Once the contract ticket is merged to `main`, all other slices in the phase can branch off it and build in parallel.

**Rule:** A slice story MUST NOT be moved to Done if the contract ticket for its phase has not been merged.

---

### Decision 3: All Stories In Progress Simultaneously Within a Phase

**Rationale:** If any story in a phase is still in To Do while another is Done, work is being serialised. This signals either a missing contract ticket, a layer dependency (backend waited for, frontend blocked), or a coordination failure.

**Board rule:** At the start of each sprint within a phase, all stories for that phase MUST be moved to In Progress together. If a story is blocked, it stays In Progress with a blocker label — it does not revert to To Do.

**Anti-pattern to watch:** All 4 devs in the same story column → layer-splitting is happening → raise a coordination issue immediately.

---

### Decision 4: Phase 0 Must Merge Before Phase 1+ Begins

**Rationale:** Phase 0 produces the shared foundation: project scaffolding, Docker Compose, Liquibase baseline, JWT security wiring, and Angular HTTP client setup. Without it, every subsequent slice would need to set up its own environment, causing diverging configs and merge conflicts.

**Phase 0 stories run sequentially** (exception to the parallel rule) because each story builds directly on the previous one. Phase 0 is the only phase where sequential delivery is acceptable.

---

### Decision 5: Feature-Name Slice Naming

**Rationale:** Slice names describe the user-facing capability rather than the layer (not "Backend" or "Frontend"). Example: "Candidate Registration", "Question Bank CRUD", "Assessment Invitation Flow". This keeps the focus on the outcome, not the implementation.

---

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Phase 0 delays block all Phase 1+ work | Phase 0 stories are minimal viable scaffolding — no polish, no full error handling. Goal is a compiling, deployable skeleton. |
| Contract ticket drifts from real implementation | The developer who authors the contract ticket is also the reviewer for all slice PRs in that phase. |
| Slice boundary unclear — devs duplicate work or leave gaps | Each slice spec defines its acceptance criteria. Gaps found during implementation become new subtasks, not scope creep. |
| Board rule not enforced — slices pile up in Done while others linger | Facilitator checks board at sprint start and mid-sprint. Any story in To Do after day 1 of a sprint is a blocker conversation. |
| 4 devs insufficient for large phases | Phases 3–5 have more slices than 4 devs. Priority slices are assigned; remaining slices become the next sprint's In Progress batch. |

## Open Questions

- **Email provider**: Which SMTP/transactional email service will be used for invitation and result emails? (Needed for Phase 3 and Phase 5 tasks)
- **File storage**: Where are `doc_question` file uploads stored? Local disk (dev only) vs S3/object storage? (Needed for Phase 2 and Phase 4 tasks)
- **Invitation token TTL**: How long should the invitation token remain valid? 7 days? 30 days? (Needed for Phase 3 spec)