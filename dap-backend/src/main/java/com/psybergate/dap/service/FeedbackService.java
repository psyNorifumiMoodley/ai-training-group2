package com.psybergate.dap.service;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Feedback;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.dto.FeedbackItem;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.FeedbackRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final ResponseRepository responseRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           AssessmentRepository assessmentRepository,
                           AssessmentQuestionRepository assessmentQuestionRepository,
                           ResponseRepository responseRepository) {
        this.feedbackRepository = feedbackRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional
    public Feedback getOrCreateDraft(UUID assessmentId, UUID questionId) {
        Optional<Feedback> existing = feedbackRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        AssessmentQuestion question = assessmentQuestionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + questionId));

        String autoDraft = resolveAutoDraft(assessmentId, questionId, question);

        Feedback feedback = Feedback.builder()
                .assessment(assessment)
                .question(question)
                .draft(autoDraft)
                .finalised(false)
                .build();

        return feedbackRepository.save(feedback);
    }

    private String resolveAutoDraft(UUID assessmentId, UUID questionId, AssessmentQuestion question) {
        if (!(question instanceof McqQuestion)) {
            return "";
        }
        Optional<Response> responseOpt = responseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId);
        if (responseOpt.isEmpty()) {
            return "";
        }
        Response response = responseOpt.get();
        if (!(response instanceof McqResponse mcqResponse)) {
            return "";
        }
        Boolean correct = mcqResponse.getCorrect();
        if (correct == null) {
            return "";
        }
        return correct ? "Correct" : "Incorrect — please review this topic";
    }

    @Transactional
    public void updateFeedback(UUID assessmentId, UUID questionId, FeedbackUpdateRequest request) {
        Feedback feedback = getOrCreateDraft(assessmentId, questionId);
        feedback.setDraft(request.feedbackText());
        feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackItem> getCandidateFeedback(UUID assessmentId, UUID requestingCandidateId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (!assessment.getCandidate().getId().equals(requestingCandidateId)) {
            throw new AccessDeniedException("You are not the assigned candidate for this assessment");
        }

        if (assessment.getStatus() != AssessmentStatus.MARKED) {
            throw new AccessDeniedException("Feedback is not yet available");
        }

        return feedbackRepository.findWithQuestionByAssessmentId(assessmentId).stream()
                .map(f -> new FeedbackItem(
                        f.getQuestion().getId(),
                        f.getQuestion().getQuestion(),
                        f.getDraft()))
                .toList();
    }
}
