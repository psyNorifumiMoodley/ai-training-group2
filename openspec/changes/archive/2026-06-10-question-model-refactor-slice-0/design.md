## Context

Phase 2 implemented the question domain with a `String category` field on every `AssessmentQuestion` subtype. Categories were free-text strings with no entity backing — discoverable only via a `SELECT DISTINCT category` query. This worked as a v1 shortcut but now needs to be replaced with proper `QuestionBank` entities that have identity, CRUD management, and a many-to-many relationship to questions.

Concurrently, three other capability gaps must be addressed before Phase 6 can continue:
- **Marks** — questions have no marks attached; assessment scoring cannot be completed without them
- **McqPlusQuestion** — a new question type (MCQ + embedded text follow-up) required for the marking and assessment experience features
- **GroupQuestion children** — currently a ManyToMany link to standalone `TextQuestion` entities; this leaks group children into standalone question lists and prevents them from being isolated to their parent

Slice 0 delivers only the **API contract layer** for all four capabilities: updated DTOs, new DTOs, sealed interface changes, stub controllers, and frontend type definitions. It contains no business logic, no DB changes, and no service implementations. Its sole purpose is to unblock parallel development on the three following slices (schema, backend logic, Angular UI).

## Goals / Non-Goals

**Goals:**
- Define all breaking DTO changes in a single merge so downstream slices compile against stable contracts
- Add `QuestionBankRequest`/`QuestionBankResponse`, `GroupChildRequest`/`GroupChildResponse`, `McqPlusQuestionRequest`/`McqPlusQuestionResponse` to the type system
- Remove `category` and add `questionBankIds`/`questionBanks` across all question request/response types
- Add `marks` to `TextQuestion` and `DocQuestion` DTOs; `totalMarks` (computed) to `GroupQuestionResponse` and `McqPlusQuestionResponse`
- Stub `QuestionBankController` so the Angular UI slice can develop against real HTTP paths
- Update `CodingQuestionRequest`/`CodingQuestionResponse` (merged Phase 6 Slice 0) to remove `category`
- Remove `GET /api/questions/categories`; update `GET /api/questions` query param from `?category=` to `?questionBankId=`
- Fully update Angular `question.model.ts` and add stub `QuestionBankService`

**Non-Goals:**
- No DB schema changes (handled in Slice A)
- No entity changes (handled in Slice B)
- No service logic — `QuestionBankController` returns hardcoded responses only
- No frontend components (handled in Slice C)
- No `AssessmentRequest` changes — QB-based generation scoping is a Phase 3 concern

## Decisions

### 1. All breaking changes land in one slice, not incrementally

**Decision:** Drop `category` and add `questionBankIds`/`questionBanks` in a single Slice 0 commit rather than keeping `category` temporarily alongside the new field.

**Rationale:** There is no production data to migrate and no live consumers. A dual-field transition period adds noise (which field is authoritative?) with no benefit. A clean break is safer and simpler.

**Alternative considered:** Add `questionBankIds` as an optional field alongside `category` in requests, deprecate `category` gradually. Rejected: creates ambiguity about which field takes precedence during the transition; doubles the validation surface.

---

### 2. `McqPlusQuestion` extends `McqQuestion`, not `AssessmentQuestion` directly

**Decision:** `McqPlusQuestionRequest`/`Response` carry all MCQ fields (`options`, `correctAnswers`, `multiCorrect`) plus `followUpQuestion`, `followUpKeywords`, and `followUpMarks`.

**Rationale:** An McqPlus IS an MCQ with an extra follow-up. Sharing the MCQ field set directly in the record avoids duplication and makes the type hierarchy clear at the DTO level. The same MCQ validation rules (correctAnswers ⊆ options, non-empty options) apply unchanged.

**Alternative considered:** A separate `followUp: TextQuestionRequest` nested object inside `McqPlusQuestionRequest`. Rejected: over-engineering for three flat fields; the follow-up is not a reusable TextQuestion entity, just embedded data.

---

### 3. `GroupChildRequest` carries inline question data, not IDs

**Decision:** `GroupChildRequest` contains `questionText`, `keywords`, and `marks` — not a `List<UUID>` pointing to existing `TextQuestion` rows.

**Rationale:** Group children must not be independently reusable standalone questions. Passing IDs would allow linking to arbitrary TextQuestions, re-introducing the coupling the separation requirement is trying to break. Inline data enforces that children are owned exclusively by their parent group.

**Alternative considered:** Keep `followUpQuestionIds: List<UUID>` and add a server-side "detach from standalone list" flag. Rejected: architecturally wrong — it creates two kinds of TextQuestion that differ only by a flag, which will cause confusion in every query and UI filter.

---

### 4. `totalMarks` on `GroupQuestionResponse` and `McqPlusQuestionResponse` is a response-only computed field

**Decision:** `totalMarks` is computed by the service at response mapping time (sum of `children[].marks` for Group; `1 + followUpMarks` for McqPlus) and returned in the response DTO. It is not stored in the DB.

**Rationale:** Derived data should not be persisted; it would require keeping the stored value in sync with any child mark change. Response-time computation is cheap and always correct.

---

### 5. `QuestionBankController` stubs follow the existing stub pattern

**Decision:** All four QB endpoints return hardcoded `200`/`201`/`204` in Slice 0, consistent with how `QuestionController` was stubbed in Phase 2 Slice 0.

**Rationale:** Allows Angular Slice C to code against real HTTP paths with known response shapes without waiting for the full backend implementation.

## Risks / Trade-offs

- **Phase 6 Slice 0 already merged with `category`** — `CodingQuestionRequest` and `CodingQuestionResponse` were merged to `main` with a `category` field. This slice breaks that contract. Any in-flight branch that references `category` on coding questions will need a rebase. → Mitigation: communicate the breaking change clearly at merge time; rebase is a one-line fix per occurrence.

- **All dependent branches uncompilable until Slice 0 merges** — Slices A, B, and C all depend on the new DTO shapes. If Slice 0 is delayed, the other three cannot start. → Mitigation: Slice 0 is a pure DTO/stub change with no logic; it should be fast to implement and review.

- **`sealed interface` permits list must stay in sync** — Adding a new `QuestionRequest`/`QuestionResponse` subtype requires updating both the `permits` clause and the `@JsonSubTypes` annotation. Missing either causes a runtime or compile error. → Mitigation: treat the sealed interface file as a checklist: every permits entry must have a matching `@JsonSubTypes.Type`.

## Open Questions

None — all decisions were resolved during the spec planning session on 2026-06-10.
