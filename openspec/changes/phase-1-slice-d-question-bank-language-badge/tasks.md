## 1. QuestionListComponent — type filter & helpers

- [x] 1.1 Add `'CODING'` to `filterTypes: Array<QuestionType | 'ALL'>` in `question-list.component.ts`
- [x] 1.2 Add `languageLabel(language: CodingQuestionLanguage): string` helper mapping `'JAVA' → 'Java'`, `'PYTHON' → 'Python'`, `'CSHARP' → 'C#'`
- [x] 1.3 Add `expandedQuestionId = signal<string | null>(null)` and `toggleExpand(id: string)` (sets to `null` if already expanded, otherwise sets to `id`)
- [x] 1.4 Import `CodingQuestionResponse` and `CodingQuestionLanguage` from `question.model`; add a typed helper `asCoding(q: QuestionResponse): CodingQuestionResponse` (cast, used only behind `resolveType(q) === 'CODING'` guards)

## 2. question-list.component.html — badge & count

- [x] 2.1 In the row metadata line (next to the existing `<dap-tag>` type tag and bank name), add `@if (resolveType(q) === 'CODING')` block rendering `<dap-tag [label]="languageLabel(asCoding(q).language)" variant="coding" />`
- [x] 2.2 In the same `@if` block, render the test case count as `{{ asCoding(q).testCases.length }} test case(s)` (singular/plural), styled like the existing bank-name text

## 3. question-list.component.html — inline read-only test case expansion

- [x] 3.1 Add a "View test cases" toggle button on `CODING` rows (`@if (resolveType(q) === 'CODING')`) that calls `toggleExpand(q.id)`; label/icon reflects expanded vs collapsed state (e.g., chevron direction or "Hide test cases")
- [x] 3.2 Below the row, add `@if (expandedQuestionId() === q.id)` block rendering one card per entry in `asCoding(q).testCases`, each showing input, expected output, timeout (seconds), and memory (MB) as read-only text
- [x] 3.3 When `asCoding(q).testCases` is empty, show a "No test cases" message in the expanded area instead of an empty list

## 4. Verification

- [x] 4.1 Run `npx tsc --noEmit -p tsconfig.app.json` — no type errors
- [x] 4.2 Run `npx ng build --configuration development` — build succeeds
- [ ] 4.3 Manually verify in the browser: CODING rows show language badge + test case count, non-CODING rows show neither, `CODING` filter option works, expand/collapse shows correct read-only test case data including the zero-test-case case
