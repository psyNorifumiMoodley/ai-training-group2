## Why

Phase 6 Slice B delivered working backend endpoints for question banks, inline `GroupQuestionChild` persistence, `McqPlusQuestion`, and `marks` on text and doc questions. The Angular UI still operates on stale assumptions: `QuestionBankService` stubs out create/rename/delete; `QuestionFormComponent` still has a `category` text field and hardcodes `questionBankIds: []`; group questions are built by linking existing `TextQuestion` rows (a pattern eliminated in Slice A); and `MCQ_PLUS` is not surfaced as a createable question type. Slice C closes this gap — it connects the Angular layer to the real Phase 6 API contract so that markers can manage banks and create every supported question type correctly.

## What Changes

- **Wire `QuestionBankService`** — implement `createQuestionBank()`, `renameQuestionBank()`, and `deleteQuestionBank()` (all currently return `EMPTY`); `getQuestionBanks()` already calls the real endpoint
- **`BankListComponent` overhaul** — load banks directly from `QuestionBankService.getQuestionBanks()` (replace computed derivation from question responses); add inline Create Bank, Rename Bank, and Delete Bank flows with 409 conflict handling; load questions per bank via `?questionBankId=` filter instead of loading all 1000 and filtering client-side
- **`QuestionFormComponent` overhaul** — replace `category` form control with a `questionBankIds` multi-select populated from `QuestionBankService`; pre-select banks in edit mode from `existingQuestion.questionBanks`; add `marks` field for `TEXT` and `DOC`; replace old follow-up picker (`selectedFollowUpIds` + `loadTextQuestions`) with an inline `GroupChildrenBuilderComponent`; add `MCQ_PLUS` to the type selector with follow-up fields (`followUpQuestion`, `followUpKeywords`, `followUpMarks`)
- **New `GroupChildrenBuilderComponent`** — manages the ordered list of `GroupChildRequest` items (questionText, keywords via `KeywordListComponent`, marks); add/remove children; validates at least one child; used by `QuestionFormComponent` when type is `GROUP`
- **`QuestionPickerComponent` update** — load bank list from `QuestionBankService.getQuestionBanks()` rather than deriving it from a full question dump; fetch questions per selected bank via `?questionBankId=`

## Capabilities

### Modified Capabilities

- `question-bank-management`: Angular UI now performs real CRUD via `QuestionBankService` — create bank (unique-name validation feedback on 409), rename, delete (409 conflict feedback when bank has questions); bank list sourced from the API instead of derived from question responses
- `question-form`: Form sends a populated `questionBankIds[]` (required, min 1 bank); `marks` submitted for TEXT and DOC; GROUP questions built with inline children; MCQ_PLUS createable from the UI

### New Capabilities

- `group-question-inline-children` (Angular): `GroupChildrenBuilderComponent` lets markers add/remove/edit inline children on a GROUP question; replaces the obsolete follow-up link picker
- `mcq-plus-question` (Angular): MCQ_PLUS available in the question type selector; form sections for `followUpQuestion`, `followUpKeywords`, and `followUpMarks` render when type is `MCQ_PLUS`; `totalMarks` shown read-only in the response

## Impact

- **`src/app/core/services/question-bank.service.ts`** — three stub methods replaced with real HTTP calls
- **`src/app/features/question-banks/bank-list/`** — component and template substantially rewritten; now depends on `QuestionBankService` for bank CRUD and uses `?questionBankId=` filter for questions
- **`src/app/features/question-management/components/question-form/`** — `category` control removed; `questionBankIds` multi-select added; `marks`, MCQ_PLUS fields, and children builder wired in
- **New** `src/app/features/question-management/components/group-children-builder/` — standalone component for inline GROUP children
- **`src/app/features/assessment-generation/components/question-picker/`** — bank list loaded from service; questions fetched per bank
- **`src/app/features/question-banks/add-question-modal/`** — component deleted (obsolete stub that referenced stale `assessment.model.ts` types and never called the API)
- **Unblocks** end-to-end testing of the full Phase 6 question model against real data
