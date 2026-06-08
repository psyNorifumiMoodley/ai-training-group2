package com.psybergate.dap.service;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.DocResponse;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.QuestionGroupResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.AssessmentSummaryResponse;
import com.psybergate.dap.dto.DocAnswerPayload;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.dto.McqAnswerPayload;
import com.psybergate.dap.dto.ResponseReviewItem;
import com.psybergate.dap.dto.TextAnswerPayload;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MarkingService {

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

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
    public Page<AssessmentSummaryResponse> listAssessments(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Assessment> assessments = (status != null && !status.isBlank())
                ? assessmentRepository.findByStatus(AssessmentStatus.valueOf(status.toUpperCase()), pageable)
                : assessmentRepository.findAll(pageable);
        return assessments.map(this::toSummaryResponse);
    }

    private AssessmentSummaryResponse toSummaryResponse(Assessment assessment) {
        String bankName = assessment.getQuestions().stream()
                .findFirst()
                .map(AssessmentQuestion::getCategory)
                .orElse(null);
        String submittedAt = assessment.getUpdatedAt() != null ? assessment.getUpdatedAt().toString() : null;
        String assignedDate = assessment.getCreatedAt() != null ? assessment.getCreatedAt().toString() : null;
        String invitationLink = assessment.getInvitationToken() != null
                ? frontendBaseUrl + "/assessment/access/" + assessment.getInvitationToken()
                : null;
        return new AssessmentSummaryResponse(
                assessment.getId(),
                assessment.getCandidate().getUser().getName(),
                null,
                bankName,
                assessment.getStatus().name(),
                assignedDate,
                submittedAt,
                assessment.getTimeLimitMinutes(),
                invitationLink
        );
    }

    @Transactional
    public List<ResponseReviewItem> getResponsesForReview(UUID assessmentId) {
        List<Response> responses = responseRepository.findTopLevelByAssessmentId(assessmentId);
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
            boolean correct = Boolean.TRUE.equals(mcqResponse.getCorrect());
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "MCQ",
                    new McqAnswerPayload(mcqResponse.getSelectedAnswers()),
                    mcqResponse.getCorrect(),
                    feedbackDraft,
                    1,
                    correct ? 1 : 0,
                    null
            );
        } else if (response instanceof TextResponse textResponse) {
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "TEXT",
                    new TextAnswerPayload(textResponse.getAnswer()),
                    null,
                    feedbackDraft,
                    1,
                    null,
                    null
            );
        } else if (response instanceof DocResponse docResponse) {
            String rawPath = docResponse.getFilePath();
            String fileName = rawPath != null
                    ? rawPath.substring(Math.max(rawPath.lastIndexOf('/'), rawPath.lastIndexOf('\\')) + 1)
                    : null;
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "DOC",
                    new DocAnswerPayload(fileName),
                    null,
                    feedbackDraft,
                    1,
                    null,
                    null
            );
        } else {
            var groupResponse = (QuestionGroupResponse) response;
            List<ResponseReviewItem> childItems = groupResponse.getChildResponses().stream()
                    .map(child -> {
                        String childBody = child.getQuestion().getQuestion();
                        String childAnswer = child instanceof TextResponse textChild ? textChild.getAnswer() : null;
                        return new ResponseReviewItem(
                                child.getId(),
                                child.getQuestion().getId(),
                                childBody,
                                "TEXT",
                                new TextAnswerPayload(childAnswer),
                                null,
                                null,
                                1,
                                null,
                                null
                        );
                    })
                    .toList();
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "GROUP",
                    null,
                    null,
                    feedbackDraft,
                    1,
                    null,
                    childItems
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
