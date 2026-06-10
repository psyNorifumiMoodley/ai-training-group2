## 1. New Domain Entities

- [ ] 1.1 Create `QuestionBank` entity: `@Entity`, `@Table(name = "question_bank")`, fields `id` (UUID, from `BaseEntity`), `name` (`@Column(unique = true, nullable = false)`); `@ManyToMany(mappedBy = "questionBanks", fetch = LAZY)` `@ToString.Exclude` `questions` field (inverse side only)
- [ ] 1.2 Create `GroupQuestionChild` entity: `@Entity`, `@Table(name = "group_question_child")`, fields `id`, `questionText` (`@Column(name = "question_text", columnDefinition = "TEXT", nullable = false)`), `keywords` (`@JdbcTypeCode(SqlTypes.JSON)`, `@Column(columnDefinition = "jsonb")`), `marks` (`@Column(nullable = false)`), `displayOrder` (`@Column(name = "display_order", nullable = false)`); `@ManyToOne(fetch = LAZY)` `@JoinColumn(name = "group_id")` `@ToString.Exclude` back to `GroupQuestion`
- [ ] 1.3 Create `McqPlusQuestion` entity: `@Entity`, `@Table(name = "mcq_plus_question")` extending `McqQuestion`; fields `followUpQuestion` (`@Column(name = "follow_up_question", columnDefinition = "TEXT", nullable = false)`), `followUpKeywords` (`@JdbcTypeCode(SqlTypes.JSON)`, `@Column(name = "follow_up_keywords", columnDefinition = "jsonb")`), `followUpMarks` (`@Column(name = "follow_up_marks", nullable = false)`)
- [ ] 1.4 Create `McqPlusResponse` entity: `@Entity`, `@Table(name = "mcq_plus_response")`, `@DiscriminatorValue("McqPlusResponse")` extending `McqResponse`; fields `followUpAnswer` (`@Column(name = "follow_up_answer", columnDefinition = "TEXT")`), `followUpScore` (`@Column(name = "follow_up_score")`)

## 2. Update Existing Entities

- [ ] 2.1 Update `AssessmentQuestion`: remove `category` field; add `questionBanks` (`@ManyToMany`, `@JoinTable(name = "question_question_bank", joinColumns = @JoinColumn(name = "question_id"), inverseJoinColumns = @JoinColumn(name = "question_bank_id"))`, `fetch = LAZY`, `@ToString.Exclude`) of type `Set<QuestionBank>`
- [ ] 2.2 Update `TextQuestion`: add `marks` field (`@Column(name = "marks", nullable = false)`)
- [ ] 2.3 Update `DocQuestion`: add `marks` field (`@Column(name = "marks", nullable = false)`)
- [ ] 2.4 Update `GroupQuestion`: remove `followUpQuestions` ManyToMany and `@JoinTable(name = "group_question_follow_up")`; add `children` (`@OneToMany(mappedBy = "groupQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = LAZY)`, `@OrderColumn(name = "display_order")`, `@ToString.Exclude`) of type `List<GroupQuestionChild>`

## 3. New Repositories

- [ ] 3.1 Create `QuestionBankRepository extends JpaRepository<QuestionBank, UUID>`: add `Optional<QuestionBank> findByName(String name)`; add `boolean existsByQuestionsId(UUID questionId)` for delete guard
- [ ] 3.2 Create `McqPlusQuestionRepository extends JpaRepository<McqPlusQuestion, UUID>`
- [ ] 3.3 Update `AssessmentQuestionRepository`: add `Page<AssessmentQuestion> findAllByQuestionBanks_Id(UUID bankId, Pageable pageable)` annotated with `@EntityGraph(attributePaths = {"questionBanks"})`

## 4. QuestionBankService (Real Implementation)

- [ ] 4.1 Create `QuestionBankService`: constructor-inject `QuestionBankRepository`
- [ ] 4.2 Implement `create(QuestionBankRequest)`: check for duplicate name (throw `ConflictException` if exists), save, return `QuestionBankResponse`
- [ ] 4.3 Implement `list(int page, int size)`: return `PageResponse<QuestionBankResponse>` from paginated `findAll()`
- [ ] 4.4 Implement `rename(UUID id, QuestionBankRequest)`: load by id (throw `NoSuchElementException` if missing), check for duplicate name when name changes, update `name`, save, return `QuestionBankResponse`
- [ ] 4.5 Implement `delete(UUID id)`: load by id (throw `NoSuchElementException` if missing), check `existsByQuestionsId(id)` (throw `ValidationException` with 409 if in use), delete

## 5. Wire QuestionBankController to Real Service

- [ ] 5.1 Constructor-inject `QuestionBankService` into `QuestionBankController`
- [ ] 5.2 Replace `listQuestionBanks()` stub with `questionBankService.list(page, size)` (add `@RequestParam` pagination params with defaults 0 / 20)
- [ ] 5.3 Replace `createQuestionBank()` stub with `questionBankService.create(request)`
- [ ] 5.4 Replace `updateQuestionBank()` stub with `questionBankService.rename(id, request)`
- [ ] 5.5 Replace `deleteQuestionBank()` stub with `questionBankService.delete(id)`

## 6. Update QuestionService

- [ ] 6.1 Constructor-inject `QuestionBankRepository` and `McqPlusQuestionRepository` into `QuestionService`
- [ ] 6.2 Add private helper `resolveQuestionBanks(List<UUID> ids)`: loads each `QuestionBank` by id, throws `NoSuchElementException` if any is missing, returns `Set<QuestionBank>`
- [ ] 6.3 Update `createMcq()`: call `resolveQuestionBanks()`, set `questionBanks` on `McqQuestion`, save
- [ ] 6.4 Update `createText()`: call `resolveQuestionBanks()`, set `marks` and `questionBanks` on `TextQuestion`, save
- [ ] 6.5 Update `createDoc()`: call `resolveQuestionBanks()`, set `marks` and `questionBanks` on `DocQuestion`, save
- [ ] 6.6 Update `createGroup()`: call `resolveQuestionBanks()`, set `questionBanks`, map `GroupChildRequest` list to `GroupQuestionChild` entities (set `groupQuestion`, `questionText`, `keywords`, `marks`, `displayOrder`), set on `children`, save
- [ ] 6.7 Replace `createMcqPlusStub()` with real `createMcqPlus()`: call `resolveQuestionBanks()`, build `McqPlusQuestion`, persist via `mcqPlusQuestionRepository.save()`, return `McqPlusQuestionResponse`
- [ ] 6.8 Update `list()`: when `questionBankId` is non-null call `findAllByQuestionBanks_Id(questionBankId, pageable)`; otherwise call `findAll(pageable)`; load `questionBanks` eagerly via entity graph (already baked in)
- [ ] 6.9 Update `update()` for `McqQuestion`: call `resolveQuestionBanks()`, update `questionBanks` (clear + addAll), save
- [ ] 6.10 Update `update()` for `TextQuestion`: call `resolveQuestionBanks()`, update `marks` and `questionBanks`, save
- [ ] 6.11 Update `update()` for `DocQuestion`: call `resolveQuestionBanks()`, update `marks` and `questionBanks`, save
- [ ] 6.12 Update `update()` for `GroupQuestion`: call `resolveQuestionBanks()`, update `questionBanks`, clear and replace `children` list with new `GroupQuestionChild` entities, save
- [ ] 6.13 Add `update()` branch for `McqPlusQuestionRequest` + `McqPlusQuestion`: update all MCQ fields, follow-up fields, and `questionBanks`, save via `mcqPlusQuestionRepository`
- [ ] 6.14 Update `toMcqResponse()`: populate `questionBanks` from `mq.getQuestionBanks()`
- [ ] 6.15 Update `toTextResponse()`: populate `questionBanks` from `tq.getQuestionBanks()`, populate `marks`
- [ ] 6.16 Update `toDocResponse()`: populate `questionBanks` from `dq.getQuestionBanks()`, populate `marks`
- [ ] 6.17 Update `toGroupResponse()`: populate `questionBanks`, map `children` to `GroupChildResponse` list, compute `totalMarks` as sum of child marks
- [ ] 6.18 Add `toMcqPlusResponse()` method: populate all fields including `totalMarks = 1 + followUpMarks`

## 7. Tests

- [ ] 7.1 Create `QuestionBankRepositoryTest` (`@DataJpaTest` with Testcontainers): test `findByName()` returns present/empty; test `existsByQuestionsId()` returns true when question is associated
- [ ] 7.2 Create `QuestionBankServiceTest` (Mockito unit test): test create success; test create duplicate name throws `ConflictException`; test rename to same name is idempotent; test rename to duplicate name throws; test delete with no questions succeeds; test delete with questions throws `ValidationException`; test delete non-existent throws `NoSuchElementException`
- [ ] 7.3 Update `QuestionBankControllerTest`: replace hardcoded stub assertions with service-backed tests — create returns real UUID and name; list returns paginated results; rename and delete call through to service; CANDIDATE JWT on any endpoint returns 403
- [ ] 7.4 Update `QuestionControllerTest` fixtures: create a `QuestionBank` row before each test and pass its ID in `questionBankIds`; verify `questionBanks` array in responses; test `questionBankId` filter on `GET /api/questions`
- [ ] 7.5 Add `QuestionControllerTest` cases for `McqPlusQuestion`: POST with valid `MCQ_PLUS` body returns 201 with persisted data; `followUpMarks: 0` returns 400; missing `followUpQuestion` returns 400; `GET /api/questions/{id}` returns correct `totalMarks`
- [ ] 7.6 Add `QuestionControllerTest` cases for `GroupQuestion` with inline children: POST with valid children returns 201; each child has a non-null UUID in the response; `children: []` returns 400; PUT replaces all children
- [ ] 7.7 Add `QuestionControllerTest` cases for marks on TEXT and DOC: marks persisted and returned; marks `< 1` returns 400; GET after create returns the submitted marks value
- [ ] 7.8 Verify application context starts with `ddl-auto=validate` against Testcontainers with all Slice A changesets applied (`DapApplicationTests.contextLoads()`)
