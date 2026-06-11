package com.psybergate.dap.service;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.DocQuestion;
import com.psybergate.dap.domain.DocResponse;
import com.psybergate.dap.domain.GroupQuestion;
import com.psybergate.dap.domain.GroupQuestionChild;
import com.psybergate.dap.domain.McqPlusQuestion;
import com.psybergate.dap.domain.McqPlusResponse;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.QuestionGroupResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.AssessmentSummaryResponse;
import com.psybergate.dap.dto.DocAnswerPayload;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.dto.McqAnswerPayload;
import com.psybergate.dap.dto.ScoreUpdateRequest;
import com.psybergate.dap.dto.ResponseReviewItem;
import com.psybergate.dap.dto.TextAnswerPayload;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

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
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Assessment> assessments = (status != null && !status.isBlank())
                ? assessmentRepository.findByStatus(AssessmentStatus.valueOf(status.toUpperCase()), pageable)
                : assessmentRepository.findAll(pageable);
        return assessments.map(this::toSummaryResponse);
    }

    private AssessmentSummaryResponse toSummaryResponse(Assessment assessment) {
        String bankName = assessment.getQuestions().stream()
                .findFirst()
                .flatMap(q -> q.getQuestionBanks().stream().findFirst())
                .map(bank -> bank.getName())
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
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        List<Response> responses = responseRepository.findTopLevelByAssessmentId(assessmentId);
        Map<UUID, Response> responseByQuestionId = responses.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        return assessment.getQuestions().stream()
                .map(question -> {
                    AssessmentQuestion q = (AssessmentQuestion) Hibernate.unproxy(question);
                    Response response = responseByQuestionId.get(q.getId());
                    return response != null
                            ? mapToReviewItem(assessmentId, response)
                            : mapUnansweredQuestion(assessmentId, q);
                })
                .toList();
    }

    private ResponseReviewItem mapToReviewItem(UUID assessmentId, Response response) {
        UUID questionId = response.getQuestion().getId();
        String questionBody = response.getQuestion().getQuestion();
        var feedback = feedbackService.getOrCreateDraft(assessmentId, questionId);
        String feedbackDraft = feedback.getDraft();

        if (response instanceof McqPlusResponse mcqPlusResponse) {
            McqPlusQuestion mcqPlusQuestion = (McqPlusQuestion) Hibernate.unproxy(response.getQuestion());
            boolean correct = Boolean.TRUE.equals(mcqPlusResponse.getCorrect());
            int marks = 1 + mcqPlusQuestion.getFollowUpMarks();
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "MCQ",
                    new McqAnswerPayload(
                            mcqPlusResponse.getSelectedAnswers(),
                            mcqPlusQuestion.getOptions(),
                            mcqPlusQuestion.getCorrectAnswers()),
                    mcqPlusResponse.getCorrect(),
                    feedbackDraft,
                    marks,
                    correct ? 1 : 0,
                    null
            );
        } else if (response instanceof McqResponse mcqResponse) {
            boolean correct = Boolean.TRUE.equals(mcqResponse.getCorrect());
            McqQuestion mcqQuestion = (McqQuestion) Hibernate.unproxy(response.getQuestion());
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "MCQ",
                    new McqAnswerPayload(
                            mcqResponse.getSelectedAnswers(),
                            mcqQuestion.getOptions(),
                            mcqQuestion.getCorrectAnswers()),
                    mcqResponse.getCorrect(),
                    feedbackDraft,
                    1,
                    correct ? 1 : 0,
                    null
            );
        } else if (response instanceof TextResponse textResponse) {
            TextQuestion textQuestion = (TextQuestion) Hibernate.unproxy(response.getQuestion());
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "TEXT",
                    new TextAnswerPayload(textResponse.getAnswer()),
                    null,
                    feedbackDraft,
                    textQuestion.getMarks(),
                    response.getScore(),
                    null
            );
        } else if (response instanceof DocResponse docResponse) {
            DocQuestion docQuestion = (DocQuestion) Hibernate.unproxy(response.getQuestion());
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
                    docQuestion.getMarks(),
                    response.getScore(),
                    null
            );
        } else {
            var groupResponse = (QuestionGroupResponse) response;
            GroupQuestion gq = (GroupQuestion) Hibernate.unproxy(groupResponse.getQuestion());
            List<GroupQuestionChild> childDefs = gq.getChildren();
            int totalGroupMarks = childDefs.stream().mapToInt(GroupQuestionChild::getMarks).sum();

            List<Response> childResponseList = groupResponse.getChildResponses();
            List<ResponseReviewItem> childItems = new ArrayList<>();
            for (int i = 0; i < childResponseList.size(); i++) {
                Response child = childResponseList.get(i);
                String childBody = (i < childDefs.size()) ? childDefs.get(i).getQuestionText() : "";
                String childAnswer = child instanceof TextResponse textChild ? textChild.getAnswer() : null;
                int childMarks = (i < childDefs.size()) ? childDefs.get(i).getMarks() : 1;
                childItems.add(new ResponseReviewItem(
                        child.getId(),
                        child.getQuestion() != null ? child.getQuestion().getId() : null,
                        childBody,
                        "TEXT",
                        new TextAnswerPayload(childAnswer),
                        null,
                        null,
                        childMarks,
                        child.getScore(),
                        null
                ));
            }
            return new ResponseReviewItem(
                    response.getId(),
                    questionId,
                    questionBody,
                    "GROUP",
                    null,
                    null,
                    feedbackDraft,
                    totalGroupMarks,
                    response.getScore(),
                    childItems
            );
        }
    }

    private ResponseReviewItem mapUnansweredQuestion(UUID assessmentId, AssessmentQuestion question) {
        var feedback = feedbackService.getOrCreateDraft(assessmentId, question.getId());
        String feedbackDraft = feedback.getDraft();

        if (question instanceof McqPlusQuestion mq) {
            int marks = 1 + mq.getFollowUpMarks();
            return new ResponseReviewItem(null, question.getId(), question.getQuestion(),
                    "MCQ", new McqAnswerPayload(List.of(), mq.getOptions(), mq.getCorrectAnswers()),
                    null, feedbackDraft, marks, null, null);
        } else if (question instanceof McqQuestion mq) {
            return new ResponseReviewItem(null, question.getId(), question.getQuestion(),
                    "MCQ", new McqAnswerPayload(List.of(), mq.getOptions(), mq.getCorrectAnswers()),
                    null, feedbackDraft, 1, null, null);
        } else if (question instanceof TextQuestion tq) {
            return new ResponseReviewItem(null, question.getId(), question.getQuestion(),
                    "TEXT", new TextAnswerPayload(null), null, feedbackDraft, tq.getMarks(), null, null);
        } else if (question instanceof DocQuestion dq) {
            return new ResponseReviewItem(null, question.getId(), question.getQuestion(),
                    "DOC", new DocAnswerPayload(null), null, feedbackDraft, dq.getMarks(), null, null);
        } else if (question instanceof GroupQuestion gq) {
            List<GroupQuestionChild> childDefs = gq.getChildren();
            int totalMarks = childDefs.stream().mapToInt(GroupQuestionChild::getMarks).sum();
            List<ResponseReviewItem> childItems = childDefs.stream()
                    .map(child -> new ResponseReviewItem(null, null, child.getQuestionText(),
                            "TEXT", new TextAnswerPayload(null), null, null, child.getMarks(), null, null))
                    .toList();
            return new ResponseReviewItem(null, question.getId(), question.getQuestion(),
                    "GROUP", null, null, feedbackDraft, totalMarks, null, childItems);
        }
        throw new UnsupportedOperationException("Unmapped question type: " + question.getClass());
    }

    @Transactional
    public void updateResponseScore(UUID assessmentId, UUID responseId, ScoreUpdateRequest request) {
        Response response = responseRepository.findById(responseId)
                .orElseThrow(() -> new NoSuchElementException("Response not found: " + responseId));

        if (!response.getAssessment().getId().equals(assessmentId)) {
            throw new ValidationException(
                    "Response " + responseId + " does not belong to assessment " + assessmentId);
        }

        if (response instanceof McqResponse && !(response instanceof McqPlusResponse)) {
            throw new ValidationException("MCQ scores are set automatically and cannot be updated manually");
        }

        int marks;
        if (response instanceof QuestionGroupResponse g) {
            GroupQuestion gq = (GroupQuestion) Hibernate.unproxy(g.getQuestion());
            marks = gq.getChildren().stream().mapToInt(GroupQuestionChild::getMarks).sum();
        } else if (response instanceof TextResponse) {
            TextQuestion tq = (TextQuestion) Hibernate.unproxy(response.getQuestion());
            marks = tq.getMarks();
        } else if (response instanceof DocResponse) {
            DocQuestion dq = (DocQuestion) Hibernate.unproxy(response.getQuestion());
            marks = dq.getMarks();
        } else {
            marks = 1;
        }
        if (request.score() > marks) {
            throw new ValidationException("Score " + request.score() + " exceeds maximum marks of " + marks);
        }

        response.setScore(request.score());
        responseRepository.save(response);
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
