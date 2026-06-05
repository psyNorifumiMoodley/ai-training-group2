## Context

`ResponseRepository.findByAssessmentId(assessmentId)` issues a simple `SELECT * FROM response WHERE assessment_id = ?`. Child responses inside a `QuestionGroupResponse` are saved with `assessment_id` set (see `ResponseService.upsertResponse` — every child calls `response.setAssessment(assessment)`), so they appear in the flat result alongside their parent group response. `MarkingService.getResponsesForReview()` maps every row to a `ResponseReviewItem`, making a 2-follow-up group question produce 3 cards instead of 1.

The `question_group_response_children` join table records which `Response` rows are children (`child_response_id`). This is the authoritative source for exclusion.

## Goals / Non-Goals

**Goals:**
- `GET /api/assessments/{id}/responses` returns only top-level response items (no rows that are children in `question_group_response_children`).
- GROUP items include a nested `childItems` list carrying each follow-up question body and text answer.
- The frontend renders a GROUP card with the parent question stem and, below it, read-only rows for each child question + answer.
- Card layout in `FeedbackItemEditorComponent` is visually polished — cleaner spacing and hierarchy.

**Non-Goals:**
- Per-child feedback fields are not added in this change; GROUP feedback is at the parent card level only.
- No changes to how child responses are saved or auto-marked.
- No changes to the question count shown in the assessment list.

## Decisions

### Decision 1 — Exclude children via a NOT IN / NOT EXISTS subquery on `question_group_response_children`

**Choice:** Add `ResponseRepository.findTopLevelByAssessmentId(UUID assessmentId)` with JPQL:
```
SELECT r FROM Response r
WHERE r.assessment.id = :assessmentId
  AND r.id NOT IN (
    SELECT c.id FROM Response c
    JOIN QuestionGroupResponse g ON c MEMBER OF g.childResponses
    WHERE g.assessment.id = :assessmentId
  )
```

**Rationale:** `MEMBER OF` is standard JPQL and avoids native SQL, keeping the query portable. The subquery scope is bounded to the same assessment — safe and cheap for typical assessment sizes (< 20 responses).

**Alternative considered:** Filter in Java after fetching all rows. Rejected: materialises child rows unnecessarily and adds service-layer complexity.

### Decision 2 — Add `childItems` to `ResponseReviewItem` as a nullable list

**Choice:** `ResponseReviewItem` gains `List<ResponseReviewItem> childItems` (null for non-GROUP types, non-empty for GROUP types). The `questionType` for children exposed in `childItems` is always `TEXT` (follow-up questions are `TextQuestion`).

**Rationale:** Re-using `ResponseReviewItem` for children avoids a new DTO and lets the frontend re-use the same rendering primitives. The field being null for non-GROUP types avoids breaking any existing consumers.

### Decision 3 — Frontend renders children as read-only rows inside the GROUP card

**Choice:** `FeedbackItemEditorComponent` adds a GROUP branch: shows the group question body, then for each item in `childItems` renders a read-only block with the child question body and text answer. No feedback field per child.

**Rationale:** Feedback is given at the group level only (per product decision). Children are context, not independently markable items in this version.

## Risks / Trade-offs

- **Risk: MEMBER OF query performance** with TABLE_PER_CLASS inheritance → Mitigation: assessments have a bounded number of responses; query is not in a hot path.
- **Risk: childItems null vs empty** in the frontend — both should be treated as "no children". The TypeScript interface uses `childItems?: ResponseReviewItem[]`, so undefined and null are both handled by `?? []`.

## Migration Plan

No DB or Liquibase changes. Standard backend + frontend deploy. The API response shape gains a new nullable field — existing frontend code unaffected (Angular ignores unknown fields in JSON deserialization by default).
