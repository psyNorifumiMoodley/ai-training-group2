## Context

`question-list.component.html` renders each question as a flat row (text, type tag, bank name, edit/delete buttons) inside `features/question-management/components/question-list/`. `CODING` questions (added in Slice C) carry `language: CodingQuestionLanguage` (`'JAVA' | 'PYTHON' | 'CSHARP'`) and `testCases: TestCase[]` in `CodingQuestionResponse`, both already returned by `GET /api/questions` — nothing new to fetch.

The original Phase 1 plan assumed a `features/question-bank/` folder with separate `QuestionCardComponent` and `QuestionDetailComponent` files. That folder doesn't exist; Slice C already established the pattern of extending the existing `question-management` components in place rather than adding new component files. This slice follows the same pattern.

`TagComponent` (`shared/components/tag/tag.component.ts`) already has a `coding: 'bg-purple-100 text-purple-700'` variant used for the `CODING` type tag itself (`typeTagVariant()` in `question-list.component.ts`).

## Goals / Non-Goals

**Goals:**
- Show language (Java/Python/C#) and test case count on every `CODING` row in the question list
- Let a marker expand a `CODING` row to see a read-only list of its test cases (input, expected output, timeout, memory)
- Make `CODING` selectable in the existing type filter dropdown

**Non-Goals:**
- No new components (`QuestionCardComponent`, `QuestionDetailComponent`) — extend `question-list` in place
- No new API calls — `testCases` and `language` are already present on `CodingQuestionResponse`
- No editing of test cases from the list view — editing remains in `question-form` (Slice C)
- No changes to `question-form.component.*`

## Decisions

### 1. Language badge reuses `dap-tag` with the existing `coding` variant
Render `<dap-tag [label]="languageLabel(q)" variant="coding" />` next to the existing type tag. Reusing the `coding` variant (purple) keeps both badges visually grouped without adding a new color/variant to `TagComponent`. A small `languageLabel(language: CodingQuestionLanguage): string` helper in `question-list.component.ts` maps `'JAVA' → 'Java'`, `'PYTHON' → 'Python'`, `'CSHARP' → 'C#'`.

**Alternative considered**: add per-language variants (`java`, `python`, `csharp`) to `TagComponent`. Rejected — three new colors add visual noise for no functional benefit at this stage; can be revisited if language-specific styling is requested later.

### 2. Test case count is plain text, not a badge
Render `{{ q.testCases.length }} test case(s)` as small gray text next to the bank name (same row metadata line), matching how the bank name is currently displayed. A badge would overstate its importance relative to the bank name.

### 3. Read-only test cases via inline expand/collapse, not a new modal
Add an `expandedQuestionId = signal<string | null>(null)` to `QuestionListComponent`. A "View test cases" button on each `CODING` row toggles `expandedQuestionId` between `null` and that row's `q.id`. When expanded, render an `@if` block beneath the row's metadata line listing each `TestCase` (input, expected output, timeout, memory) read directly from `q.testCases` — no template/service changes beyond this component.

**Alternative considered**: a separate read-only "view" modal (closer to the original `QuestionDetailComponent`). Rejected for this slice — it's more net-new code (a new component + imports) for a feature that fits naturally as a row expansion, and keeps with Slice C's precedent of minimal new files. Revisit if the list of fields to show grows large enough that an expanded row becomes cluttered.

### 4. Add `'CODING'` to `filterTypes`
`filterTypes: Array<QuestionType | 'ALL'>` in `question-list.component.ts` currently omits `'CODING'`, so it's unreachable via the filter dropdown even though it's a valid `QuestionType` (added in Slice C). Add it to the array; `typeTagVariant()` and `resolveType()` already handle `'CODING'` correctly (added in Slice C), so no other changes are needed.

## Risks / Trade-offs

- **[Risk]** Expanding many rows at once could make the list visually noisy. → **Mitigation**: only one row expands at a time (`expandedQuestionId` holds a single id, not a set); expanding a new row collapses any previously expanded one.
- **[Risk]** `q.testCases` only exists on `CodingQuestionResponse` — accessing it on other types is a TypeScript error. → **Mitigation**: guard all CODING-specific rendering with `@if (resolveType(q) === 'CODING')` and cast via a small typed helper (e.g., `asCoding(q): CodingQuestionResponse`), mirroring how `question-form.component.ts` already narrows `QuestionResponse` by type.
