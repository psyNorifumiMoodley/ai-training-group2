## Why

Phase 6 Slice A delivered all 11 DDL changesets and the schema is now in the state that Hibernate `ddl-auto=validate` expects. Slice B converts that schema into live backend logic: updating entities to match the new tables, implementing real `QuestionBank` and `QuestionBankService` behind the stub controller, wiring `QuestionService` to persist question-bank associations, and introducing the `GroupQuestionChild` and `McqPlusQuestion`/`McqPlusResponse` entity types that exist in the DB but have no JPA representations yet.

## What Changes

- **BREAKING** — `AssessmentQuestion.category: String` removed; replaced with `questionBanks: Set<QuestionBank>` (`@ManyToMany` via `question_question_bank` join table)
- **BREAKING** — `GroupQuestion.followUpQuestions: List<TextQuestion>` removed (the `group_question_follow_up` join table was dropped in Slice A); replaced with `children: List<GroupQuestionChild>` (`@OneToMany` via `group_question_child`)
- **New entity** — `QuestionBank`: `@Entity`, `@Table(name = "question_bank")`, `id (UUID)`, `name (String, UNIQUE NOT NULL)`, `questions (ManyToMany inverse side, mappedBy = "questionBanks")`
- **New entity** — `GroupQuestionChild`: `@Entity`, `@Table(name = "group_question_child")`, owned by `GroupQuestion`; fields: `id`, `groupQuestion (ManyToOne)`, `questionText (TEXT NOT NULL)`, `keywords (JSONB)`, `marks (INTEGER NOT NULL)`, `displayOrder (INTEGER NOT NULL)`
- **New entity** — `McqPlusQuestion`: `@Entity`, `@Table(name = "mcq_plus_question")`, `TABLE_PER_CLASS` extending `McqQuestion`; fields: `followUpQuestion (TEXT NOT NULL)`, `followUpKeywords (JSONB)`, `followUpMarks (INTEGER NOT NULL)`
- **New entity** — `McqPlusResponse`: `@Entity`, `@Table(name = "mcq_plus_response")`, JOINED hierarchy extending `McqResponse`; fields: `followUpAnswer (TEXT)`, `followUpScore (INTEGER)`
- **Updated entity** — `TextQuestion`: add `marks: int` field (`@Column(name = "marks", nullable = false)`)
- **Updated entity** — `DocQuestion`: add `marks: int` field (`@Column(name = "marks", nullable = false)`)
- **New repository** — `QuestionBankRepository`: `JpaRepository<QuestionBank, UUID>`; custom query: `findByName(String name)` for duplicate-name check
- **New repository** — `McqPlusQuestionRepository`: `JpaRepository<McqPlusQuestion, UUID>`
- **New service** — `QuestionBankService`: real CRUD (create with duplicate-name validation, list paginated, rename with duplicate-name validation, delete)
- **Updated controller** — `QuestionBankController`: wired to `QuestionBankService` (replace hardcoded stubs)
- **Updated service** — `QuestionService`: resolve and attach `QuestionBank` entities from `questionBankIds` on create/update; persist `GroupQuestionChild` rows inline; persist `McqPlusQuestion` via `McqPlusQuestionRepository`; filter `list()` by `questionBankId` when provided; `toResponse()` maps `questionBanks`, `children`, and `marks` from real entity state

## Capabilities

### New Capabilities

- `question-bank-management`: Real CRUD for named `QuestionBank` entities — create with unique-name validation, list (paginated), rename, delete; question-to-bank many-to-many association persisted via `question_question_bank` join table on every create/update
- `question-marks`: `marks` field persisted on `TextQuestion` and `DocQuestion`; `GroupQuestion` total marks derived from sum of `GroupQuestionChild.marks`; `McqPlusQuestion` total = `1 + followUpMarks`
- `group-question-inline-children`: `GroupQuestionChild` entity owned by `GroupQuestion`; children created, updated, and deleted inline with the parent; absent from all standalone question queries
- `mcq-plus-question`: `McqPlusQuestion` entity persisted via `McqPlusQuestionRepository`; `McqPlusResponse` entity for candidate responses; MCQ part auto-marked on submission (1 mark); text follow-up part manually marked

### Modified Capabilities

- `question-bank`: `AssessmentQuestion` entities now require at least one `questionBankId` in create/update requests; `GET /api/questions?questionBankId=` filter now queries the `question_question_bank` join table

## Impact

- **Domain entities** — `AssessmentQuestion`, `TextQuestion`, `DocQuestion`, `GroupQuestion` all modified; four new entities (`QuestionBank`, `GroupQuestionChild`, `McqPlusQuestion`, `McqPlusResponse`) added to `com.psybergate.dap.domain`
- **Repositories** — `QuestionBankRepository` and `McqPlusQuestionRepository` added; `AssessmentQuestionRepository` may need a `findAllByQuestionBanks_Id(UUID)` query for bank-scoped listing
- **Services** — `QuestionBankService` (new), `QuestionService` (updated); `QuestionBankController` wired to real service
- **Tests** — `QuestionBankControllerTest` stub tests replaced with real service-backed tests; new repository tests for `QuestionBank`; `QuestionControllerTest` updated to supply `questionBankIds`
- **Unblocks Slice C** — Angular `QuestionBankService` and `QuestionService` can now call real endpoints
