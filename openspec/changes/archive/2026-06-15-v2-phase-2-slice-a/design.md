## Context

The `Response` hierarchy uses `InheritanceType.JOINED` with a discriminator column (`dtype`) on the `response` base table. Every concrete response type (MCQ, Text, Doc, Group, McqPlus) follows the same pattern: a dedicated table with a single `id` column that is both the PK and a FK referencing `response(id)`, plus the type-specific columns. The discriminator value matches the Java class name.

`CodingResponse` is the sixth member of this hierarchy. Slice 0 created the `CodingResponseRequest` DTO and wired it into the `ResponseRequest` sealed interface, so the controller compiles and can receive a CODING payload — but the service has no entity to persist it. Slice B will add the service logic; this slice delivers only the schema and JPA layer.

`TestCaseResult` is a new standalone table (not part of the response hierarchy) that stores the per-test-case output of one code execution run against a `CodingResponse`. It references both `coding_response(id)` and `test_case(id)`, so execution results are always traceable back to both the candidate's answer and the question's expected test case definition.

## Goals / Non-Goals

**Goals:**
- `CodingResponse` JPA entity compiles, is loadable by Hibernate, and persists via the existing `ResponseRepository`
- `TestCaseResult` JPA entity is loadable by Hibernate and links correctly to both `CodingResponse` and `TestCase`
- Two Liquibase changesets produce the DDL (with rollback blocks)
- `CodingResponseRepository` and `TestCaseResultRepository` interfaces exist for Slice B to call without modification
- No breaking changes to any existing entity or changeset

**Non-Goals:**
- No service logic — saving, loading, or mapping `CodingResponse` rows (Slice B)
- No Judge0 integration or execution trigger (Slice B)
- No controller changes (Slice B)
- No Angular changes (Slice C)
- No composite unique constraint on `test_case_result` — Slice B owns the delete-and-reinsert pattern; the schema does not enforce single-result-per-test-case at the DB level (avoids complexity in partial failure scenarios)

## Decisions

### Decision 1: `executed_at` lives on `coding_response`, not on `test_case_result`

`CodeExecuteResponse` (the DTO returned by the execute endpoint) has a top-level `executedAt` field alongside the results list. Storing it on `coding_response` as a nullable `executed_at` column avoids deriving it from `test_case_result.created_at`, which would break if results are partially inserted or if the clock skews between rows. It also gives Slice B a single column to update atomically on each execution run.

**Alternative considered:** Derive `executedAt` from `MAX(test_case_result.created_at)`. Rejected — fragile if partial inserts occur and adds a GROUP BY query for a value that's logically singular per execution run.

---

### Decision 2: `CodingResponse` extends `Response` directly (JOINED, `@DiscriminatorValue("CodingResponse")`)

All existing response subtypes follow this pattern. Consistency with `DocResponse`, `TextResponse`, etc. is more important than any marginal gain from a different strategy. `TABLE_PER_CLASS` was considered to avoid the join, but was rejected because it would require duplicating all base columns (`assessment_id`, `question_id`, `score`, `dtype`, `created_at`, `updated_at`) in the DDL and breaks the single `ResponseRepository` query pattern.

---

### Decision 3: `CodingResponseRepository` is a new interface (not folded into `ResponseRepository`)

`ResponseRepository` is generic (`Response`). Service code that loads `CodingResponse` by assessment + question will need to filter by type and return a typed result. A dedicated `CodingResponseRepository` with a `findByAssessmentIdAndQuestionId` method keeps the query readable and avoids casting. Slice B calls this directly.

---

### Decision 4: `TestCaseResultRepository` exposes `deleteAllByCodingResponseId` and `findAllByCodingResponseId`

Slice B's execute flow is: delete all prior results for this `CodingResponse`, then insert the new ones from Judge0. A `deleteAllByCodingResponseId(UUID)` derived query method is sufficient and keeps the service layer free of custom JPQL.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Slice B inserts partial `test_case_result` rows (Judge0 returns 3 of 5) then a second execute is called | `deleteAllByCodingResponseId` always clears prior state before inserting; no orphan rows remain |
| `dtype` discriminator collides with a future response type also named `CodingResponse` | Discriminator value matches the Java class name — this is the established project convention; unlikely to conflict |
| `executed_at` is non-null after first execution and null before — callers must null-check | DTO mapping in Slice B will pass `null` gracefully; `CodeExecuteResponse.executedAt` is `Instant` (nullable in practice) |

## Open Questions

- **Unique constraint on `(coding_response_id, test_case_id)`**: should the DB enforce one result row per test case per response? Decision deferred to Slice B — if the delete-before-insert pattern is solid, the constraint adds safety; if partial inserts are expected, the constraint causes failures on retry.
