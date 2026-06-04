package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.Feedback;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.AssessmentSummaryResponse;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.dto.ResponseReviewItem;
import com.psybergate.dap.dto.TextAnswerPayload;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkingServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private ResponseRepository responseRepository;

    @Mock
    private FeedbackService feedbackService;

    private MarkingService markingService;

    @BeforeEach
    void setUp() {
        markingService = new MarkingService(assessmentRepository, responseRepository, feedbackService);
    }

    // ---------- helpers ----------

    private AppUser userWithName(String name) {
        AppUser user = AppUser.builder()
                .email(name.toLowerCase().replace(" ", ".") + "@test.com")
                .name(name)
                .passwordHash("x")
                .role(Role.CANDIDATE)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Candidate candidateFor(AppUser user) {
        Candidate candidate = Candidate.builder().user(user).build();
        candidate.setId(user.getId());
        return candidate;
    }

    private Assessment assessmentWith(Candidate candidate, AssessmentStatus status) {
        Assessment assessment = Assessment.builder()
                .candidate(candidate)
                .status(status)
                .timeLimitMinutes(60)
                .build();
        assessment.setId(UUID.randomUUID());
        assessment.setUpdatedAt(Instant.now());
        return assessment;
    }

    private McqQuestion mcqQuestion() {
        McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
        q.setId(UUID.randomUUID());
        q.setCategory("Java");
        q.setQuestion("What is Java?");
        return q;
    }

    private TextQuestion textQuestion() {
        TextQuestion q = new TextQuestion(null);
        q.setId(UUID.randomUUID());
        q.setCategory("Java");
        q.setQuestion("Explain OOP.");
        return q;
    }

    private Feedback feedbackWithDraft(Assessment assessment, String draft) {
        return Feedback.builder()
                .assessment(assessment)
                .draft(draft)
                .finalised(false)
                .build();
    }

    // ---------- listAssessments ----------

    @Test
    void listAssessments_withStatusFilter_mapsCandidateNameCorrectly() {
        AppUser user = userWithName("Alice Smith");
        Candidate candidate = candidateFor(user);
        Assessment assessment = assessmentWith(candidate, AssessmentStatus.SUBMITTED);

        Page<Assessment> page = new PageImpl<>(List.of(assessment), PageRequest.of(0, 20), 1);
        when(assessmentRepository.findByStatus(eq(AssessmentStatus.SUBMITTED), any()))
                .thenReturn(page);

        Page<AssessmentSummaryResponse> result = markingService.listAssessments("SUBMITTED", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        AssessmentSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.candidateName()).isEqualTo("Alice Smith");
        assertThat(summary.id()).isEqualTo(assessment.getId());
        assertThat(summary.status()).isEqualTo("SUBMITTED");
    }

    @Test
    void listAssessments_noStatusFilter_returnsAllAssessments() {
        Page<Assessment> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(assessmentRepository.findAll(any(PageRequest.class)))
                .thenReturn(empty);

        Page<AssessmentSummaryResponse> result = markingService.listAssessments(null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ---------- getResponsesForReview — MCQ correct ----------

    @Test
    void getResponsesForReview_mcqCorrect_feedbackDraftIsCorrect() {
        AppUser user = userWithName("Bob Jones");
        Candidate candidate = candidateFor(user);
        Assessment assessment = assessmentWith(candidate, AssessmentStatus.SUBMITTED);
        UUID assessmentId = assessment.getId();

        McqQuestion question = mcqQuestion();
        McqResponse response = McqResponse.builder()
                .selectedAnswers(List.of("A"))
                .correct(true)
                .build();
        response.setId(UUID.randomUUID());
        response.setAssessment(assessment);
        response.setQuestion(question);

        Feedback feedback = feedbackWithDraft(assessment, "Correct");

        when(responseRepository.findByAssessmentId(assessmentId))
                .thenReturn(List.of(response));
        when(feedbackService.getOrCreateDraft(assessmentId, question.getId()))
                .thenReturn(feedback);

        List<ResponseReviewItem> items = markingService.getResponsesForReview(assessmentId);

        assertThat(items).hasSize(1);
        ResponseReviewItem item = items.get(0);
        assertThat(item.questionType()).isEqualTo("MCQ");
        assertThat(item.correct()).isTrue();
        assertThat(item.feedbackDraft()).isEqualTo("Correct");
    }

    // ---------- getResponsesForReview — MCQ incorrect ----------

    @Test
    void getResponsesForReview_mcqIncorrect_feedbackDraftIsIncorrectMessage() {
        AppUser user = userWithName("Carol White");
        Candidate candidate = candidateFor(user);
        Assessment assessment = assessmentWith(candidate, AssessmentStatus.SUBMITTED);
        UUID assessmentId = assessment.getId();

        McqQuestion question = mcqQuestion();
        McqResponse response = McqResponse.builder()
                .selectedAnswers(List.of("B"))
                .correct(false)
                .build();
        response.setId(UUID.randomUUID());
        response.setAssessment(assessment);
        response.setQuestion(question);

        Feedback feedback = feedbackWithDraft(assessment, "Incorrect — please review this topic");

        when(responseRepository.findByAssessmentId(assessmentId))
                .thenReturn(List.of(response));
        when(feedbackService.getOrCreateDraft(assessmentId, question.getId()))
                .thenReturn(feedback);

        List<ResponseReviewItem> items = markingService.getResponsesForReview(assessmentId);

        assertThat(items).hasSize(1);
        ResponseReviewItem item = items.get(0);
        assertThat(item.questionType()).isEqualTo("MCQ");
        assertThat(item.correct()).isFalse();
        assertThat(item.feedbackDraft()).isEqualTo("Incorrect — please review this topic");
    }

    // ---------- getResponsesForReview — TEXT response ----------

    @Test
    void getResponsesForReview_textResponse_feedbackDraftIsEmpty() {
        AppUser user = userWithName("Dave Brown");
        Candidate candidate = candidateFor(user);
        Assessment assessment = assessmentWith(candidate, AssessmentStatus.SUBMITTED);
        UUID assessmentId = assessment.getId();

        TextQuestion question = textQuestion();
        TextResponse response = TextResponse.builder()
                .answer("OOP stands for...")
                .build();
        response.setId(UUID.randomUUID());
        response.setAssessment(assessment);
        response.setQuestion(question);

        Feedback feedback = feedbackWithDraft(assessment, "");

        when(responseRepository.findByAssessmentId(assessmentId))
                .thenReturn(List.of(response));
        when(feedbackService.getOrCreateDraft(assessmentId, question.getId()))
                .thenReturn(feedback);

        List<ResponseReviewItem> items = markingService.getResponsesForReview(assessmentId);

        assertThat(items).hasSize(1);
        ResponseReviewItem item = items.get(0);
        assertThat(item.questionType()).isEqualTo("TEXT");
        assertThat(item.correct()).isNull();
        assertThat(item.feedbackDraft()).isEmpty();
        assertThat(item.answer()).isEqualTo(new TextAnswerPayload("OOP stands for..."));
        assertThat(item.marks()).isEqualTo(1);
        assertThat(item.score()).isNull();
    }

    // ---------- updateResponseFeedback — response not belonging to assessment ----------

    @Test
    void updateResponseFeedback_responseNotBelongingToAssessment_throwsValidationException() {
        UUID assessmentId = UUID.randomUUID();
        UUID differentAssessmentId = UUID.randomUUID();

        AppUser user = userWithName("Eve Green");
        Candidate candidate = candidateFor(user);
        Assessment otherAssessment = assessmentWith(candidate, AssessmentStatus.SUBMITTED);
        otherAssessment.setId(differentAssessmentId);

        McqQuestion question = mcqQuestion();
        McqResponse response = McqResponse.builder().selectedAnswers(List.of("A")).build();
        UUID responseId = UUID.randomUUID();
        response.setId(responseId);
        response.setAssessment(otherAssessment);
        response.setQuestion(question);

        when(responseRepository.findById(responseId)).thenReturn(Optional.of(response));

        assertThatThrownBy(() -> markingService.updateResponseFeedback(
                assessmentId, responseId, new FeedbackUpdateRequest("Good answer")))
                .isInstanceOf(ValidationException.class);
    }

    // ---------- updateResponseFeedback — response not found ----------

    @Test
    void updateResponseFeedback_responseNotFound_throwsNoSuchElementException() {
        UUID assessmentId = UUID.randomUUID();
        UUID responseId = UUID.randomUUID();

        when(responseRepository.findById(responseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markingService.updateResponseFeedback(
                assessmentId, responseId, new FeedbackUpdateRequest("text")))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
