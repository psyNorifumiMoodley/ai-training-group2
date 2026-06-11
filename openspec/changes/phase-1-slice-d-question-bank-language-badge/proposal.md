## Why

CODING questions (added in Phase 1 Slice C) are indistinguishable from other question types in the question bank list — markers can't tell which language a coding question uses, how many test cases it has, or inspect those test cases without opening the edit form. Slice D closes that gap so markers can scan and review coding questions at a glance.

## What Changes

- Add a language badge (`Java` / `Python` / `C#`) to each `CODING` question row in the question list
- Add a test case count (`"3 test cases"` / `"0 test cases"`) to each `CODING` question row
- Add an inline "View test cases" toggle on `CODING` rows that expands to a read-only list of test cases (input, expected output, timeout, memory) — sourced from `CodingQuestionResponse.testCases`, already returned by the existing `GET /api/questions` response (no new API call)
- Add `'CODING'` to the question-list type filter dropdown (`question-list.component.ts`'s `filterTypes`), which currently omits it — without this, markers cannot filter to find coding questions at all

> **Deviation from original plan**: The spec doc describes new `QuestionCardComponent` and `QuestionDetailComponent` files under `features/question-bank/components/`. That folder doesn't exist — questions render as flat rows directly in `features/question-management/components/question-list/question-list.component.html`, with no separate detail view. This slice adds the badge, count, and inline expandable test case view directly to the existing `question-list` component, consistent with how Slice C extended the existing `question-form` rather than creating new form components.

## Capabilities

### New Capabilities
- `coding-question-display`: Display of language badge, test case count, and read-only inline test case list for `CODING` questions in the question bank list, plus inclusion of `CODING` in the type filter

### Modified Capabilities
(none — no existing spec covers question-list display behavior)

## Impact

- `dap-frontend/src/app/features/question-management/components/question-list/question-list.component.html` — add language badge, test case count, and expandable test case panel for `CODING` rows
- `dap-frontend/src/app/features/question-management/components/question-list/question-list.component.ts` — add `'CODING'` to `filterTypes`, add expand/collapse state and a language-label helper
- No backend changes — `CodingQuestionResponse.testCases` and `language` are already returned by `GET /api/questions`
