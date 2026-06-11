package com.psybergate.dap.service;

import com.psybergate.dap.config.InvitationTokenUtil;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Feedback;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.CandidateRepository;
import com.psybergate.dap.repository.DocQuestionRepository;
import com.psybergate.dap.repository.FeedbackRepository;
import com.psybergate.dap.repository.GroupQuestionRepository;
import com.psybergate.dap.repository.McqPlusQuestionRepository;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.TextQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackEmailTest {

    @Mock private CandidateRepository candidateRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private McqQuestionRepository mcqQuestionRepository;
    @Mock private McqPlusQuestionRepository mcqPlusQuestionRepository;
    @Mock private TextQuestionRepository textQuestionRepository;
    @Mock private DocQuestionRepository docQuestionRepository;
    @Mock private GroupQuestionRepository groupQuestionRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private InvitationTokenUtil invitationTokenUtil;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private ResponseService responseService;

    private AssessmentService assessmentService;

    private static final String CANDIDATE_EMAIL = "candidate@test.com";
    private static final String CANDIDATE_NAME = "Jane Doe";

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentService(
                candidateRepository, assessmentRepository, assessmentQuestionRepository,
                mcqQuestionRepository, mcqPlusQuestionRepository, textQuestionRepository, docQuestionRepository,
                groupQuestionRepository, invitationTokenUtil, jwtUtil, emailService,
                responseService, feedbackRepository);
    }

    private Assessment submittedAssessmentFor(String email, String name) {
        AppUser user = AppUser.builder()
                .email(email).name(name).passwordHash("x").role(Role.CANDIDATE).build();
        user.setId(UUID.randomUUID());

        Candidate candidate = Candidate.builder().user(user).build();
        candidate.setId(user.getId());

        Assessment assessment = Assessment.builder()
                .candidate(candidate)
                .status(AssessmentStatus.SUBMITTED)
                .timeLimitMinutes(60)
                .invitationToken("stub-token")
                .build();
        assessment.setId(UUID.randomUUID());
        return assessment;
    }

    private Feedback feedbackFor(Assessment assessment, String questionBody, String draft) {
        TextQuestion question = new TextQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestion(questionBody);

        return Feedback.builder()
                .assessment(assessment)
                .question(question)
                .draft(draft)
                .finalised(false)
                .build();
    }

    @Test
    void finalise_sendsEmailToCorrectRecipient() {
        Assessment assessment = submittedAssessmentFor(CANDIDATE_EMAIL, CANDIDATE_NAME);
        UUID assessmentId = assessment.getId();
        Feedback feedback = feedbackFor(assessment, "Explain polymorphism", "Good explanation.");

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(feedbackRepository.findQuestionsWithEmptyFeedback(assessmentId)).thenReturn(List.of());
        when(feedbackRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(feedback));
        when(assessmentRepository.save(any())).thenReturn(assessment);

        assessmentService.finalise(assessmentId, "Strong overall performance.");

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendFeedback(emailCaptor.capture(), nameCaptor.capture(), any(String.class));

        assertThat(emailCaptor.getValue()).isEqualTo(CANDIDATE_EMAIL);
        assertThat(nameCaptor.getValue()).isEqualTo(CANDIDATE_NAME);
    }

    @Test
    void finalise_passesOverallFeedbackToEmail() {
        Assessment assessment = submittedAssessmentFor(CANDIDATE_EMAIL, CANDIDATE_NAME);
        UUID assessmentId = assessment.getId();
        Feedback feedback = feedbackFor(assessment, "Explain polymorphism", "Good explanation.");

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(feedbackRepository.findQuestionsWithEmptyFeedback(assessmentId)).thenReturn(List.of());
        when(feedbackRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(feedback));
        when(assessmentRepository.save(any())).thenReturn(assessment);

        assessmentService.finalise(assessmentId, "Strong overall performance.");

        ArgumentCaptor<String> feedbackCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendFeedback(any(), any(), feedbackCaptor.capture());

        assertThat(feedbackCaptor.getValue()).isEqualTo("Strong overall performance.");
    }

    @Test
    void finalise_nullOverallFeedback_sendsEmptyString() {
        Assessment assessment = submittedAssessmentFor(CANDIDATE_EMAIL, CANDIDATE_NAME);
        UUID assessmentId = assessment.getId();
        Feedback feedback = feedbackFor(assessment, "Explain polymorphism", "Good explanation.");

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(feedbackRepository.findQuestionsWithEmptyFeedback(assessmentId)).thenReturn(List.of());
        when(feedbackRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(feedback));
        when(assessmentRepository.save(any())).thenReturn(assessment);

        assessmentService.finalise(assessmentId, null);

        ArgumentCaptor<String> feedbackCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendFeedback(any(), any(), feedbackCaptor.capture());

        assertThat(feedbackCaptor.getValue()).isEqualTo("");
    }

    @Test
    void finalise_notSubmitted_throwsConflictException_andNoEmailSent() {
        Assessment assessment = submittedAssessmentFor(CANDIDATE_EMAIL, CANDIDATE_NAME);
        assessment.setStatus(AssessmentStatus.MARKED);
        UUID assessmentId = assessment.getId();

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> assessmentService.finalise(assessmentId, ""))
                .isInstanceOf(ConflictException.class);

        verify(emailService, never()).sendFeedback(any(), any(), any(String.class));
    }
}
