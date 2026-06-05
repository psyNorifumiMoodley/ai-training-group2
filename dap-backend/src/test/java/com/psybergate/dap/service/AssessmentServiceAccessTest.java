package com.psybergate.dap.service;

import com.psybergate.dap.config.InvitationTokenUtil;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.AssessmentAccessResponse;
import com.psybergate.dap.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceAccessTest {

    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock
    private McqQuestionRepository mcqQuestionRepository;
    @Mock
    private TextQuestionRepository textQuestionRepository;
    @Mock
    private DocQuestionRepository docQuestionRepository;
    @Mock
    private GroupQuestionRepository groupQuestionRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private InvitationTokenUtil invitationTokenUtil;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailService emailService;
    @Mock
    private ResponseService responseService;

    private AssessmentService assessmentService;

    private static final String VALID_TOKEN = "valid.invitation.token";

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentService(
                candidateRepository, assessmentRepository, assessmentQuestionRepository,
                mcqQuestionRepository, textQuestionRepository, docQuestionRepository,
                groupQuestionRepository, invitationTokenUtil, jwtUtil, emailService, responseService,
                feedbackRepository);
        lenient().when(jwtUtil.generateToken(any())).thenReturn("candidate.jwt.token");
        ReflectionTestUtils.setField(assessmentService, "requiredMcq", 5);
        ReflectionTestUtils.setField(assessmentService, "requiredText", 3);
        ReflectionTestUtils.setField(assessmentService, "requiredDoc", 1);
        ReflectionTestUtils.setField(assessmentService, "requiredGroup", 1);
        ReflectionTestUtils.setField(assessmentService, "docQuestionLimit", 1);
        ReflectionTestUtils.setField(assessmentService, "frontendBaseUrl", "http://localhost:4200");
    }

    private Assessment pendingAssessment() {
        Assessment a = Assessment.builder()
                .candidate(stubCandidate())
                .status(AssessmentStatus.PENDING)
                .timeLimitMinutes(60)
                .questions(new ArrayList<>())
                .build();
        a.setId(UUID.randomUUID());
        a.setCreatedAt(Instant.now());
        return a;
    }

    private Assessment inProgressAssessment() {
        Assessment a = Assessment.builder()
                .candidate(stubCandidate())
                .status(AssessmentStatus.IN_PROGRESS)
                .timeLimitMinutes(60)
                .questions(new ArrayList<>())
                .build();
        a.setId(UUID.randomUUID());
        a.setCreatedAt(Instant.now());
        a.setStartTime(Instant.now().minus(5, ChronoUnit.MINUTES));
        return a;
    }

    private Candidate stubCandidate() {
        AppUser user = AppUser.builder()
                .email("c@test.com").name("Candidate").passwordHash("x").role(Role.CANDIDATE).build();
        user.setId(UUID.randomUUID());
        Candidate candidate = Candidate.builder().user(user).build();
        candidate.setId(UUID.randomUUID());
        return candidate;
    }

    // --- Invalid token ---

    @Test
    void access_invalidSignature_throwsUnauthorizedException() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(false);

        assertThatThrownBy(() -> assessmentService.access(VALID_TOKEN))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid invitation token");

        verify(assessmentRepository, never()).findByInvitationToken(any());
    }

    // --- Token not found in DB ---

    @Test
    void access_tokenNotFound_throwsUnauthorizedException() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);
        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.access(VALID_TOKEN))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found");
    }

    // --- Already SUBMITTED ---

    @Test
    void access_submittedAssessment_throwsConflictException() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        Assessment submitted = pendingAssessment();
        submitted.setStatus(AssessmentStatus.SUBMITTED);
        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> assessmentService.access(VALID_TOKEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("submitted");
    }

    // --- Already MARKED ---

    @Test
    void access_markedAssessment_throwsConflictException() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        Assessment marked = pendingAssessment();
        marked.setStatus(AssessmentStatus.MARKED);
        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(marked));

        assertThatThrownBy(() -> assessmentService.access(VALID_TOKEN))
                .isInstanceOf(ConflictException.class);
    }

    // --- Session expired ---

    @Test
    void access_sessionExpired_throwsUnauthorizedException() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        Assessment expired = Assessment.builder()
                .candidate(stubCandidate())
                .status(AssessmentStatus.IN_PROGRESS)
                .timeLimitMinutes(60)
                .questions(new ArrayList<>())
                .build();
        expired.setId(UUID.randomUUID());
        expired.setCreatedAt(Instant.now());
        // startTime was 2 hours ago, limit is 60 minutes — clearly expired
        expired.setStartTime(Instant.now().minus(120, ChronoUnit.MINUTES));

        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> assessmentService.access(VALID_TOKEN))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    // --- PENDING → IN_PROGRESS transition ---

    @Test
    void access_pendingAssessment_transitionsToInProgressAndSetsStartTime() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        Assessment pending = pendingAssessment();
        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(pending));

        // After save, assessment must have startTime set (simulated by returning same object)
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment saved = inv.getArgument(0);
            // Ensure the startTime was set before save was called
            assertThat(saved.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
            assertThat(saved.getStartTime()).isNotNull();
            return saved;
        });

        AssessmentAccessResponse response = assessmentService.access(VALID_TOKEN);

        verify(assessmentRepository).save(any(Assessment.class));
        assertThat(response).isNotNull();
        assertThat(response.assessmentId()).isEqualTo(pending.getId());
        assertThat(response.remainingSeconds()).isGreaterThan(0);
    }

    // --- Already IN_PROGRESS (re-access) ---

    @Test
    void access_inProgressAssessment_returnsResponseWithoutSavingAgain() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        Assessment inProgress = inProgressAssessment();
        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(inProgress));

        AssessmentAccessResponse response = assessmentService.access(VALID_TOKEN);

        verify(assessmentRepository, never()).save(any());
        assertThat(response.assessmentId()).isEqualTo(inProgress.getId());
        assertThat(response.remainingSeconds()).isGreaterThan(0);
        assertThat(response.remainingSeconds()).isLessThanOrEqualTo(55 * 60); // ~55 minutes left
    }

    // --- remainingSeconds is correct ---

    @Test
    void access_inProgressAssessment_remainingSecondsReflectsElapsedTime() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        Assessment inProgress = Assessment.builder()
                .candidate(stubCandidate())
                .status(AssessmentStatus.IN_PROGRESS)
                .timeLimitMinutes(60)
                .questions(new ArrayList<>())
                .build();
        inProgress.setId(UUID.randomUUID());
        inProgress.setCreatedAt(Instant.now());
        // Started 30 minutes ago
        inProgress.setStartTime(Instant.now().minus(30, ChronoUnit.MINUTES));

        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(inProgress));

        AssessmentAccessResponse response = assessmentService.access(VALID_TOKEN);

        // ~30 minutes (1800 s) remaining — allow a 5-second tolerance for test execution time
        assertThat(response.remainingSeconds()).isGreaterThan(1795);
        assertThat(response.remainingSeconds()).isLessThanOrEqualTo(1800);
    }

    // --- Questions are mapped ---

    @Test
    void access_inProgressWithMcqQuestion_mapsQuestionsToResponse() {
        when(invitationTokenUtil.isSignatureValid(VALID_TOKEN)).thenReturn(true);

        McqQuestion mcq = new McqQuestion(List.of("A", "B"), List.of("A"));
        mcq.setId(UUID.randomUUID());
        mcq.setCategory("Java");
        mcq.setQuestion("What is Java?");

        Assessment inProgress = Assessment.builder()
                .candidate(stubCandidate())
                .status(AssessmentStatus.IN_PROGRESS)
                .timeLimitMinutes(60)
                .questions(new ArrayList<>(List.of(mcq)))
                .build();
        inProgress.setId(UUID.randomUUID());
        inProgress.setCreatedAt(Instant.now());
        inProgress.setStartTime(Instant.now().minus(5, ChronoUnit.MINUTES));

        when(assessmentRepository.findByInvitationToken(VALID_TOKEN)).thenReturn(Optional.of(inProgress));

        AssessmentAccessResponse response = assessmentService.access(VALID_TOKEN);

        assertThat(response.questions()).hasSize(1);
    }
}
