package com.psybergate.dap.service;

import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.DocResponse;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.AssessmentSummaryResponse;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.dto.ResponseReviewItem;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MarkingService {

    private final AssessmentRepository assessmentRepository;
    private final ResponseRepository responseRepository;
    private final FeedbackService feedbackService;

    public MarkingService(AssessmentRepository assessmentRepository,
                          ResponseRepository responseRepository,
                          FeedbackService feedbackService) {
        this.assessmentRepository = assessmentRepository;
        this.responseRepository = responseRepository;
        this.feedbackService = feedbackService;
    }

    @Transactional(readOnly = true)
    public Page<AssessmentSummaryResponse> listSubmitted(int page, int size) {
        return assessmentRepository
                .findByStatus(AssessmentStatus.SUBMITTED, PageRequest.of(page, size))
                .map(assessment -> new AssessmentSummaryResponse(
                        assessment.getId(),
                        assessment.getCandidate().getUser().getName(),
                        assessment.getUpdatedAt() != null ? assessment.getUpdatedAt().toString() : null,
                        assessment.getStatus().name()
                ));
    }

    @Transactional
    public List<ResponseReviewItem> getResponsesForReview(UUID assessmentId) {
        List<Response> responses = responseRepository.findWithQuestionByAssessmentId(assessmentId);
        return responses.stream()
                .map(response -> mapToReviewItem(assessmentId, response))
                .toList();
    }

    private ResponseReviewItem mapToReviewItem(UUID assessmentId, Response response) {
        UUID questionId = response.getQuestion().getId();
        String questionBody = response.getQuestion().getQuestion();
        var feedback = feedbackService.getOrCreateDraft(assessmentId, questionId);
        String feedbackDraft = feedback.getDraft();

        if (response instanceof McqResponse mcqResponse) {
            Object answer = mcqResponse.getSelectedAnswers();
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "MCQ",
                    answer,
                    mcqResponse.getCorrect(),
                    feedbackDraft
            );
        } else if (response instanceof TextResponse textResponse) {
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "TEXT",
                    textResponse.getAnswer(),
                    null,
                    feedbackDraft
            );
        } else if (response instanceof DocResponse docResponse) {
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "DOC",
                    docResponse.getFilePath(),
                    null,
                    feedbackDraft
            );
        } else {
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "GROUP",
                    null,
                    null,
                    feedbackDraft
            );
        }
    }

    @Transactional
    public void updateResponseFeedback(UUID assessmentId, UUID responseId, FeedbackUpdateRequest request) {
        Response response = responseRepository.findById(responseId)
                .orElseThrow(() -> new NoSuchElementException("Response not found: " + responseId));

        if (!response.getAssessment().getId().equals(assessmentId)) {
            throw new ValidationException(
                    "Response " + responseId + " does not belong to assessment " + assessmentId);
        }

        UUID questionId = response.getQuestion().getId();
        feedbackService.updateFeedback(assessmentId, questionId, request);
    }
}
