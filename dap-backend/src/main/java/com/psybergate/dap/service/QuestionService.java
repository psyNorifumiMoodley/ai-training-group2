package com.psybergate.dap.service;

import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.*;
import com.psybergate.dap.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final DocQuestionRepository docQuestionRepository;
    private final TextQuestionRepository textQuestionRepository;
    private final GroupQuestionRepository groupQuestionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final McqPlusQuestionRepository mcqPlusQuestionRepository;
    private final CodingQuestionRepository codingQuestionRepository;

    public QuestionService(AssessmentQuestionRepository assessmentQuestionRepository,
                           McqQuestionRepository mcqQuestionRepository,
                           DocQuestionRepository docQuestionRepository,
                           TextQuestionRepository textQuestionRepository,
                           GroupQuestionRepository groupQuestionRepository,
                           QuestionBankRepository questionBankRepository,
                           McqPlusQuestionRepository mcqPlusQuestionRepository,
                           CodingQuestionRepository codingQuestionRepository) {
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.docQuestionRepository = docQuestionRepository;
        this.textQuestionRepository = textQuestionRepository;
        this.groupQuestionRepository = groupQuestionRepository;
        this.questionBankRepository = questionBankRepository;
        this.mcqPlusQuestionRepository = mcqPlusQuestionRepository;
        this.codingQuestionRepository = codingQuestionRepository;
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        if (request instanceof McqPlusQuestionRequest r) {
            return toResponse(createMcqPlus(r));
        }
        if (request instanceof McqQuestionRequest r) {
            return toResponse(createMcq(r));
        }
        if (request instanceof DocQuestionRequest r) {
            return toResponse(createDoc(r));
        }
        if (request instanceof TextQuestionRequest r) {
            return toResponse(createText(r));
        }
        if (request instanceof GroupQuestionRequest r) {
            return toResponse(createGroup(r));
        }
        if (request instanceof CodingQuestionRequest r) {
            return toCodingQuestionResponse(createCodingQuestion(r));
        }
        throw new UnsupportedOperationException("Unsupported question type: " + request.getClass());
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> list(int page, int size, UUID questionBankId) {
        Page<AssessmentQuestion> questions;
        if (questionBankId != null) {
            questions = assessmentQuestionRepository.findAllByQuestionBanks_Id(questionBankId, PageRequest.of(page, size));
        } else {
            questions = assessmentQuestionRepository.findAll(PageRequest.of(page, size));
        }
        return new PageResponse<>(
                questions.getContent().stream().map(this::toResponse).toList(),
                questions.getTotalElements(),
                questions.getTotalPages(),
                questions.getSize(),
                questions.getNumber()
        );
    }

    @Transactional(readOnly = true)
    public QuestionResponse getById(UUID id) {
        AssessmentQuestion q = assessmentQuestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + id));
        return toResponse(q);
    }

    @Transactional
    public QuestionResponse update(UUID id, QuestionRequest request) {
        AssessmentQuestion existing = assessmentQuestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + id));

        if (request instanceof McqPlusQuestionRequest r && existing instanceof McqPlusQuestion mpq) {
            validateMcq(r.options(), r.correctAnswers());
            mpq.setQuestion(r.question());
            mpq.setOptions(r.options());
            mpq.setCorrectAnswers(r.correctAnswers());
            mpq.setFollowUpQuestion(r.followUpQuestion());
            mpq.setFollowUpKeywords(r.followUpKeywords() != null ? r.followUpKeywords() : List.of());
            mpq.setFollowUpMarks(r.followUpMarks());
            mpq.getQuestionBanks().clear();
            mpq.getQuestionBanks().addAll(resolveQuestionBanks(r.questionBankIds()));
            return toResponse(mcqPlusQuestionRepository.save(mpq));
        }

        if (request instanceof McqQuestionRequest r && existing instanceof McqQuestion mq) {
            validateMcq(r.options(), r.correctAnswers());
            mq.setQuestion(r.question());
            mq.setOptions(r.options());
            mq.setCorrectAnswers(r.correctAnswers());
            mq.getQuestionBanks().clear();
            mq.getQuestionBanks().addAll(resolveQuestionBanks(r.questionBankIds()));
            return toResponse(mcqQuestionRepository.save(mq));
        }

        if (request instanceof DocQuestionRequest r && existing instanceof DocQuestion dq) {
            dq.setQuestion(r.question());
            dq.setMarks(r.marks());
            dq.getQuestionBanks().clear();
            dq.getQuestionBanks().addAll(resolveQuestionBanks(r.questionBankIds()));
            return toResponse(docQuestionRepository.save(dq));
        }

        if (request instanceof TextQuestionRequest r && isExactTextQuestion(existing)) {
            TextQuestion tq = (TextQuestion) existing;
            tq.setQuestion(r.question());
            tq.setKeywords(r.keywords() != null ? r.keywords() : List.of());
            tq.setMarks(r.marks());
            tq.getQuestionBanks().clear();
            tq.getQuestionBanks().addAll(resolveQuestionBanks(r.questionBankIds()));
            return toResponse(textQuestionRepository.save(tq));
        }

        if (request instanceof GroupQuestionRequest r && existing instanceof GroupQuestion gq) {
            gq.setQuestion(r.question());
            gq.setOrdered(r.ordered());
            gq.getQuestionBanks().clear();
            gq.getQuestionBanks().addAll(resolveQuestionBanks(r.questionBankIds()));
            gq.getChildren().clear();
            gq.getChildren().addAll(buildChildren(r.children(), gq));
            return toResponse(groupQuestionRepository.save(gq));
        }

        if (request instanceof CodingQuestionRequest r && existing instanceof CodingQuestion cq) {
            cq.setQuestion(r.question());
            cq.setLanguage(r.language());
            cq.getQuestionBanks().clear();
            cq.getQuestionBanks().addAll(resolveQuestionBanks(r.questionBankIds()));
            cq.getTestCases().clear();
            if (r.testCases() != null) {
                int ordinal = 1;
                for (TestCaseRequest tcr : r.testCases()) {
                    TestCase tc = new TestCase();
                    tc.setCodingQuestion(cq);
                    tc.setInput(tcr.input());
                    tc.setExpectedOutput(tcr.expectedOutput());
                    tc.setTimeoutSeconds(tcr.timeoutSeconds());
                    tc.setMemoryMb(tcr.memoryMb());
                    tc.setOrdinal(ordinal++);
                    cq.getTestCases().add(tc);
                }
            }
            return toCodingQuestionResponse(codingQuestionRepository.save(cq));
        }

        throw new ValidationException("Request type does not match the existing question type");
    }

    @Transactional
    public void delete(UUID id) {
        AssessmentQuestion q = assessmentQuestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + id));
        assessmentQuestionRepository.delete(q);
    }

    McqQuestion createMcq(McqQuestionRequest request) {
        validateMcq(request.options(), request.correctAnswers());
        McqQuestion q = new McqQuestion();
        q.setQuestion(request.question());
        q.setOptions(request.options());
        q.setCorrectAnswers(request.correctAnswers());
        q.getQuestionBanks().addAll(resolveQuestionBanks(request.questionBankIds()));
        return mcqQuestionRepository.save(q);
    }

    DocQuestion createDoc(DocQuestionRequest request) {
        DocQuestion q = new DocQuestion();
        q.setQuestion(request.question());
        q.setMarks(request.marks());
        q.getQuestionBanks().addAll(resolveQuestionBanks(request.questionBankIds()));
        return docQuestionRepository.save(q);
    }

    private TextQuestion createText(TextQuestionRequest request) {
        TextQuestion q = new TextQuestion();
        q.setQuestion(request.question());
        q.setKeywords(request.keywords() != null ? request.keywords() : new ArrayList<>());
        q.setMarks(request.marks());
        q.getQuestionBanks().addAll(resolveQuestionBanks(request.questionBankIds()));
        return textQuestionRepository.save(q);
    }

    private GroupQuestion createGroup(GroupQuestionRequest request) {
        GroupQuestion q = new GroupQuestion();
        q.setQuestion(request.question());
        q.setOrdered(request.ordered());
        q.getQuestionBanks().addAll(resolveQuestionBanks(request.questionBankIds()));
        q.setChildren(buildChildren(request.children(), q));
        return groupQuestionRepository.save(q);
    }

    private McqPlusQuestion createMcqPlus(McqPlusQuestionRequest request) {
        validateMcq(request.options(), request.correctAnswers());
        McqPlusQuestion q = new McqPlusQuestion();
        q.setQuestion(request.question());
        q.setOptions(request.options());
        q.setCorrectAnswers(request.correctAnswers());
        q.setFollowUpQuestion(request.followUpQuestion());
        q.setFollowUpKeywords(request.followUpKeywords() != null ? request.followUpKeywords() : List.of());
        q.setFollowUpMarks(request.followUpMarks());
        q.getQuestionBanks().addAll(resolveQuestionBanks(request.questionBankIds()));
        return mcqPlusQuestionRepository.save(q);
    }

    private CodingQuestion createCodingQuestion(CodingQuestionRequest request) {
        CodingQuestion q = new CodingQuestion();
        q.getQuestionBanks().addAll(resolveQuestionBanks(request.questionBankIds()));
        q.setQuestion(request.question());
        q.setLanguage(request.language());
        if (request.testCases() != null) {
            int ordinal = 1;
            for (TestCaseRequest tcr : request.testCases()) {
                TestCase tc = new TestCase();
                tc.setCodingQuestion(q);
                tc.setInput(tcr.input());
                tc.setExpectedOutput(tcr.expectedOutput());
                tc.setTimeoutSeconds(tcr.timeoutSeconds());
                tc.setMemoryMb(tcr.memoryMb());
                tc.setOrdinal(ordinal++);
                q.getTestCases().add(tc);
            }
        }
        return codingQuestionRepository.save(q);
    }

    private Set<QuestionBank> resolveQuestionBanks(List<UUID> ids) {
        return ids.stream()
                .map(id -> questionBankRepository.findById(id)
                        .orElseThrow(() -> new ValidationException("Question bank not found: " + id)))
                .collect(Collectors.toSet());
    }

    private List<GroupQuestionChild> buildChildren(List<GroupChildRequest> childRequests, GroupQuestion parent) {
        List<GroupQuestionChild> children = new ArrayList<>();
        for (int i = 0; i < childRequests.size(); i++) {
            GroupChildRequest cr = childRequests.get(i);
            GroupQuestionChild child = new GroupQuestionChild();
            child.setGroupQuestion(parent);
            child.setQuestionText(cr.questionText());
            child.setKeywords(cr.keywords() != null ? cr.keywords() : List.of());
            child.setMarks(cr.marks());
            child.setDisplayOrder(i);
            children.add(child);
        }
        return children;
    }

    private void validateMcq(List<String> options, List<String> correctAnswers) {
        if (options == null || options.isEmpty()) {
            throw new ValidationException("MCQ must have at least one option");
        }
        if (correctAnswers == null || correctAnswers.isEmpty()) {
            throw new ValidationException("MCQ must have at least one correct answer");
        }
        if (!new HashSet<>(options).containsAll(correctAnswers)) {
            throw new ValidationException("All correct answers must be present in the options list");
        }
    }

    private QuestionResponse toResponse(AssessmentQuestion q) {
        if (q instanceof McqPlusQuestion mpq) {
            return toMcqPlusResponse(mpq);
        }
        if (q instanceof McqQuestion mq) {
            return toMcqResponse(mq);
        }
        if (q instanceof DocQuestion dq) {
            return toDocResponse(dq);
        }
        if (q instanceof GroupQuestion gq) {
            return toGroupResponse(gq);
        }
        if (q instanceof TextQuestion tq) {
            return toTextResponse(tq);
        }
        if (q instanceof CodingQuestion cq) {
            return toCodingQuestionResponse(cq);
        }
        throw new UnsupportedOperationException("Unmapped question type: " + q.getClass());
    }

    private McqQuestionResponse toMcqResponse(McqQuestion q) {
        List<QuestionBankResponse> banks = q.getQuestionBanks().stream()
                .map(b -> new QuestionBankResponse(b.getId(), b.getName(), 0L))
                .toList();
        return new McqQuestionResponse(q.getId(), banks, q.getQuestion(), q.getOptions(),
                q.getCorrectAnswers(), q.getCorrectAnswers().size() > 1);
    }

    private McqPlusQuestionResponse toMcqPlusResponse(McqPlusQuestion q) {
        List<QuestionBankResponse> banks = q.getQuestionBanks().stream()
                .map(b -> new QuestionBankResponse(b.getId(), b.getName(), 0L))
                .toList();
        return new McqPlusQuestionResponse(
                q.getId(),
                banks,
                q.getQuestion(),
                q.getOptions(),
                q.getCorrectAnswers(),
                q.getCorrectAnswers().size() > 1,
                q.getFollowUpQuestion(),
                q.getFollowUpKeywords() != null ? q.getFollowUpKeywords() : List.of(),
                q.getFollowUpMarks(),
                1 + q.getFollowUpMarks()
        );
    }

    private DocQuestionResponse toDocResponse(DocQuestion q) {
        List<QuestionBankResponse> banks = q.getQuestionBanks().stream()
                .map(b -> new QuestionBankResponse(b.getId(), b.getName(), 0L))
                .toList();
        return new DocQuestionResponse(q.getId(), banks, q.getQuestion(), q.getMarks());
    }

    private TextQuestionResponse toTextResponse(TextQuestion q) {
        List<QuestionBankResponse> banks = q.getQuestionBanks().stream()
                .map(b -> new QuestionBankResponse(b.getId(), b.getName(), 0L))
                .toList();
        return new TextQuestionResponse(q.getId(), banks, q.getQuestion(),
                q.getKeywords() != null ? q.getKeywords() : List.of(), q.getMarks());
    }

    private GroupQuestionResponse toGroupResponse(GroupQuestion q) {
        List<QuestionBankResponse> banks = q.getQuestionBanks().stream()
                .map(b -> new QuestionBankResponse(b.getId(), b.getName(), 0L))
                .toList();
        List<GroupChildResponse> children = q.getChildren().stream()
                .filter(c -> c != null)
                .map(c -> new GroupChildResponse(c.getId(), c.getQuestionText(),
                        c.getKeywords() != null ? c.getKeywords() : List.of(), c.getMarks()))
                .toList();
        int totalMarks = children.stream().mapToInt(GroupChildResponse::marks).sum();
        return new GroupQuestionResponse(q.getId(), banks, q.getQuestion(), q.isOrdered(), children, totalMarks);
    }

    private CodingQuestionResponse toCodingQuestionResponse(CodingQuestion q) {
        List<QuestionBankResponse> banks = q.getQuestionBanks().stream()
                .map(b -> new QuestionBankResponse(b.getId(), b.getName(), 0L))
                .toList();
        List<TestCaseResponse> testCases = q.getTestCases().stream()
                .map(tc -> new TestCaseResponse(tc.getId(), tc.getInput(), tc.getExpectedOutput(),
                        tc.getTimeoutSeconds(), tc.getMemoryMb(), tc.getOrdinal()))
                .toList();
        return new CodingQuestionResponse(q.getId(), banks, q.getQuestion(), q.getLanguage(), testCases);
    }

    private boolean isExactTextQuestion(AssessmentQuestion q) {
        return q.getClass() == TextQuestion.class;
    }
}
