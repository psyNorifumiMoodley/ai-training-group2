package com.psybergate.dap.service;

import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.GroupQuestion;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.*;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.GroupQuestionRepository;
import com.psybergate.dap.repository.TextQuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class QuestionService {

    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final TextQuestionRepository textQuestionRepository;
    private final GroupQuestionRepository groupQuestionRepository;

    public QuestionService(AssessmentQuestionRepository assessmentQuestionRepository,
                           TextQuestionRepository textQuestionRepository,
                           GroupQuestionRepository groupQuestionRepository) {
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.textQuestionRepository = textQuestionRepository;
        this.groupQuestionRepository = groupQuestionRepository;
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        if (request instanceof TextQuestionRequest r) {
            return toResponse(createText(r));
        }
        if (request instanceof GroupQuestionRequest r) {
            return toResponse(createGroup(r));
        }
        throw new UnsupportedOperationException("Unsupported question type: " + request.getClass());
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> list(int page, int size, String category) {
        Page<AssessmentQuestion> questions = (category != null && !category.isBlank())
                ? assessmentQuestionRepository.findByCategory(category, PageRequest.of(page, size))
                : assessmentQuestionRepository.findAll(PageRequest.of(page, size));

        return new PageResponse<>(
                questions.getContent().stream().map(this::toResponse).toList(),
                questions.getTotalElements(),
                questions.getTotalPages()
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

        if (request instanceof TextQuestionRequest r && isExactTextQuestion(existing)) {
            TextQuestion tq = (TextQuestion) existing;
            tq.setCategory(r.category());
            tq.setQuestion(r.question());
            tq.setKeywords(r.keywords());
            return toResponse(textQuestionRepository.save(tq));
        }

        if (request instanceof GroupQuestionRequest r && existing instanceof GroupQuestion gq) {
            gq.setCategory(r.category());
            gq.setQuestion(r.question());
            gq.setOrdered(r.ordered());
            List<TextQuestion> followUps = resolveFollowUpQuestions(r.followUpQuestionIds());
            gq.getFollowUpQuestions().clear();
            gq.getFollowUpQuestions().addAll(followUps);
            return toResponse(groupQuestionRepository.save(gq));
        }

        throw new ValidationException("Request type does not match the existing question type");
    }

    @Transactional
    public void delete(UUID id) {
        AssessmentQuestion q = assessmentQuestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + id));
        assessmentQuestionRepository.delete(q);
    }

    private TextQuestion createText(TextQuestionRequest request) {
        TextQuestion q = new TextQuestion();
        q.setCategory(request.category());
        q.setQuestion(request.question());
        q.setKeywords(request.keywords() != null ? request.keywords() : new ArrayList<>());
        return textQuestionRepository.save(q);
    }

    private GroupQuestion createGroup(GroupQuestionRequest request) {
        List<TextQuestion> followUps = resolveFollowUpQuestions(request.followUpQuestionIds());

        GroupQuestion q = new GroupQuestion();
        q.setCategory(request.category());
        q.setQuestion(request.question());
        q.setOrdered(request.ordered());
        q.setFollowUpQuestions(new ArrayList<>(followUps));
        return groupQuestionRepository.save(q);
    }

    private List<TextQuestion> resolveFollowUpQuestions(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<TextQuestion> found = assessmentQuestionRepository.findTextQuestionsByIds(ids);
        if (found.size() != ids.size()) {
            throw new ValidationException("One or more follow-up question IDs are invalid or not text questions");
        }
        if (found.stream().anyMatch(GroupQuestion.class::isInstance)) {
            throw new ValidationException("Group questions cannot be used as follow-up questions");
        }
        return found;
    }

    private QuestionResponse toResponse(AssessmentQuestion q) {
        if (q instanceof GroupQuestion gq) {
            return toGroupResponse(gq);
        }
        if (q instanceof TextQuestion tq) {
            return toTextResponse(tq);
        }
        throw new UnsupportedOperationException("Unmapped question type: " + q.getClass());
    }

    private TextQuestionResponse toTextResponse(TextQuestion q) {
        return new TextQuestionResponse(q.getId(), q.getCategory(), q.getQuestion(), q.getKeywords());
    }

    private GroupQuestionResponse toGroupResponse(GroupQuestion q) {
        List<TextQuestionResponse> followUps = q.getFollowUpQuestions().stream()
                .map(this::toTextResponse)
                .toList();
        return new GroupQuestionResponse(q.getId(), q.getCategory(), q.getQuestion(), q.isOrdered(), followUps);
    }

    private boolean isExactTextQuestion(AssessmentQuestion q) {
        return q.getClass() == TextQuestion.class;
    }
}
