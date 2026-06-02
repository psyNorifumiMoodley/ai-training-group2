package com.psybergate.dap.service;

import com.psybergate.dap.config.InvitationTokenUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

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
    private InvitationTokenUtil invitationTokenUtil;
    @Mock
    private EmailService emailService;

    private AssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentService(
                candidateRepository, assessmentRepository, assessmentQuestionRepository,
                mcqQuestionRepository, textQuestionRepository, docQuestionRepository,
                groupQuestionRepository, invitationTokenUtil, emailService);
        setCompositionDefaults();
        ReflectionTestUtils.setField(assessmentService, "frontendBaseUrl", "http://localhost:4200");
        // Default stub so token generation does not return null in existing tests
        lenient().when(invitationTokenUtil.generateToken(any())).thenReturn("stub-invitation-token");
    }

    private void setCompositionDefaults() {
        ReflectionTestUtils.setField(assessmentService, "requiredMcq", 5);
        ReflectionTestUtils.setField(assessmentService, "requiredText", 3);
        ReflectionTestUtils.setField(assessmentService, "requiredDoc", 1);
        ReflectionTestUtils.setField(assessmentService, "requiredGroup", 1);
        ReflectionTestUtils.setField(assessmentService, "docQuestionLimit", 1);
    }

    private Candidate candidateWithId(UUID id) {
        AppUser user = AppUser.builder().email("c@test.com").name("Candidate").passwordHash("x").role(Role.CANDIDATE).build();
        user.setId(id);
        Candidate candidate = Candidate.builder().user(user).build();
        candidate.setId(id);
        return candidate;
    }

    private McqQuestion mcqWithId(UUID id) {
        McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
        q.setId(id);
        q.setCategory("Java");
        q.setQuestion("What is Java?");
        return q;
    }

    private TextQuestion textWithId(UUID id) {
        TextQuestion q = new TextQuestion(null);
        q.setId(id);
        q.setCategory("Java");
        q.setQuestion("Explain OOP.");
        return q;
    }

    private DocQuestion docWithId(UUID id) {
        DocQuestion q = new DocQuestion();
        q.setId(id);
        q.setCategory("Java");
        q.setQuestion("Submit your code.");
        return q;
    }

    private GroupQuestion groupWithId(UUID id) {
        GroupQuestion q = new GroupQuestion(false, new ArrayList<>());
        q.setId(id);
        q.setCategory("Java");
        q.setQuestion("Group scenario.");
        return q;
    }

    private Assessment savedAssessmentFor(Candidate candidate) {
        Assessment a = Assessment.builder()
                .candidate(candidate)
                .status(AssessmentStatus.PENDING)
                .timeLimitMinutes(60)
                .questions(new ArrayList<>())
                .build();
        a.setId(UUID.randomUUID());
        a.setCreatedAt(Instant.now());
        return a;
    }

    private List<McqQuestion> fiveMcq() {
        return List.of(mcqWithId(UUID.randomUUID()), mcqWithId(UUID.randomUUID()),
                mcqWithId(UUID.randomUUID()), mcqWithId(UUID.randomUUID()), mcqWithId(UUID.randomUUID()));
    }

    private List<TextQuestion> threeText() {
        return List.of(textWithId(UUID.randomUUID()), textWithId(UUID.randomUUID()), textWithId(UUID.randomUUID()));
    }

    // --- Random mode tests ---

    @Test
    void generate_randomMode_selectsCorrectComposition() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(fiveMcq());
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        AssessmentResponse response = assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60));

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("PENDING");

        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository, times(2)).save(captor.capture());
        List<AssessmentQuestion> questions = captor.getAllValues().get(0).getQuestions();
        assertThat(questions).hasSize(10);
        assertThat(questions.stream().filter(q -> q instanceof McqQuestion).count()).isEqualTo(5);
        assertThat(questions.stream().filter(q -> q instanceof TextQuestion && !(q instanceof GroupQuestion)).count()).isEqualTo(3);
        assertThat(questions.stream().filter(q -> q instanceof DocQuestion).count()).isEqualTo(1);
        assertThat(questions.stream().filter(q -> q instanceof GroupQuestion).count()).isEqualTo(1);
    }

    @Test
    void generate_randomMode_seenQuestionSameYear_excluded() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);

        McqQuestion seenMcq = mcqWithId(UUID.randomUUID());
        McqQuestion unseenMcq1 = mcqWithId(UUID.randomUUID());
        McqQuestion unseenMcq2 = mcqWithId(UUID.randomUUID());
        McqQuestion unseenMcq3 = mcqWithId(UUID.randomUUID());
        McqQuestion unseenMcq4 = mcqWithId(UUID.randomUUID());
        McqQuestion unseenMcq5 = mcqWithId(UUID.randomUUID());

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of(seenMcq.getId()));
        when(mcqQuestionRepository.findAll()).thenReturn(
                List.of(seenMcq, unseenMcq1, unseenMcq2, unseenMcq3, unseenMcq4, unseenMcq5));
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60));

        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository, times(2)).save(captor.capture());
        List<AssessmentQuestion> questions = captor.getAllValues().get(0).getQuestions();
        assertThat(questions).doesNotContain(seenMcq);
    }

    @Test
    void generate_randomMode_seenQuestionLastYear_included() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);

        McqQuestion lastYearMcq = mcqWithId(UUID.randomUUID());

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        // last year's question is NOT in seen IDs (filtered at DB level by year range)
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        List<McqQuestion> mcqs = new ArrayList<>();
        mcqs.add(lastYearMcq);
        for (int i = 0; i < 4; i++) mcqs.add(mcqWithId(UUID.randomUUID()));
        when(mcqQuestionRepository.findAll()).thenReturn(mcqs);
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60));

        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository, times(2)).save(captor.capture());
        // lastYearMcq was available (not excluded) — it may or may not be chosen but 5 MCQ were picked
        assertThat(captor.getAllValues().get(0).getQuestions().stream().filter(q -> q instanceof McqQuestion).count()).isEqualTo(5);
    }

    @Test
    void generate_randomMode_notEnoughQuestionsAfterFiltering_throwsUnprocessableException() {
        UUID candidateId = UUID.randomUUID();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidateWithId(candidateId)));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        // only 3 MCQ available, need 5
        when(mcqQuestionRepository.findAll()).thenReturn(
                List.of(mcqWithId(UUID.randomUUID()), mcqWithId(UUID.randomUUID()), mcqWithId(UUID.randomUUID())));

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60)))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("MCQ");
    }

    // --- Marker-picked mode tests ---

    @Test
    void generate_markerPicked_validComposition_persists() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);

        List<UUID> ids = new ArrayList<>();
        List<AssessmentQuestion> questions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            McqQuestion q = mcqWithId(UUID.randomUUID());
            ids.add(q.getId());
            questions.add(q);
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        for (int i = 0; i < 3; i++) {
            TextQuestion q = textWithId(UUID.randomUUID());
            ids.add(q.getId());
            questions.add(q);
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        DocQuestion doc = docWithId(UUID.randomUUID());
        ids.add(doc.getId());
        questions.add(doc);
        when(assessmentQuestionRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        GroupQuestion group = groupWithId(UUID.randomUUID());
        ids.add(group.getId());
        questions.add(group);
        when(assessmentQuestionRepository.findById(group.getId())).thenReturn(Optional.of(group));

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        AssessmentResponse response = assessmentService.generate(new AssessmentRequest(candidateId, ids, 60));

        assertThat(response.status()).isEqualTo("PENDING");
        verify(assessmentRepository, times(2)).save(any(Assessment.class));
    }

    @Test
    void generate_markerPicked_wrongComposition_throwsValidationException() {
        UUID candidateId = UUID.randomUUID();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidateWithId(candidateId)));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());

        // Only 3 MCQ (wrong — need 5)
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            McqQuestion q = mcqWithId(UUID.randomUUID());
            ids.add(q.getId());
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        for (int i = 0; i < 3; i++) {
            TextQuestion q = textWithId(UUID.randomUUID());
            ids.add(q.getId());
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        DocQuestion doc = docWithId(UUID.randomUUID());
        ids.add(doc.getId());
        when(assessmentQuestionRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        GroupQuestion group = groupWithId(UUID.randomUUID());
        ids.add(group.getId());
        when(assessmentQuestionRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, ids, 60)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void generate_markerPicked_allSeenThisYear_throwsUnprocessableException() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);

        List<UUID> ids = new ArrayList<>();
        List<UUID> seenIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            McqQuestion q = mcqWithId(UUID.randomUUID());
            ids.add(q.getId());
            seenIds.add(q.getId());
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        for (int i = 0; i < 3; i++) {
            TextQuestion q = textWithId(UUID.randomUUID());
            ids.add(q.getId());
            seenIds.add(q.getId());
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        DocQuestion doc = docWithId(UUID.randomUUID());
        ids.add(doc.getId());
        seenIds.add(doc.getId());
        when(assessmentQuestionRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        GroupQuestion group = groupWithId(UUID.randomUUID());
        ids.add(group.getId());
        seenIds.add(group.getId());
        when(assessmentQuestionRepository.findById(group.getId())).thenReturn(Optional.of(group));

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(seenIds);

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, ids, 60)))
                .isInstanceOf(UnprocessableException.class);
    }

    @Test
    void generate_docLimitExceeded_throwsValidationException() {
        // Lower limit to 0 to force the check
        ReflectionTestUtils.setField(assessmentService, "docQuestionLimit", 0);

        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(fiveMcq());
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("document");
    }

    @Test
    void generate_compositionCountsConfigurable_alternateValues() {
        // Reconfigure to require only 1 MCQ, 1 Text, 1 Doc, 1 Group
        ReflectionTestUtils.setField(assessmentService, "requiredMcq", 1);
        ReflectionTestUtils.setField(assessmentService, "requiredText", 1);
        ReflectionTestUtils.setField(assessmentService, "requiredDoc", 1);
        ReflectionTestUtils.setField(assessmentService, "requiredGroup", 1);

        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(List.of(mcqWithId(UUID.randomUUID())));
        when(textQuestionRepository.findAll()).thenReturn(List.of(textWithId(UUID.randomUUID())));
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60));

        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getQuestions()).hasSize(4);
    }

    @Test
    void generate_nullQuestionIds_treatedAsRandomMode() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(fiveMcq());
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        AssessmentResponse response = assessmentService.generate(new AssessmentRequest(candidateId, null, 60));

        assertThat(response).isNotNull();
        verify(mcqQuestionRepository).findAll();
    }

    @Test
    void generate_markerPicked_compositionBrokenAfterSeenFilter_throwsValidationException() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);

        // Valid initial composition: 5 MCQ, 3 text, 1 doc, 1 group
        List<UUID> ids = new ArrayList<>();
        List<UUID> seenIds = new ArrayList<>();

        // 4 of 5 MCQ are already seen — post-filter composition will be 1 MCQ (invalid)
        for (int i = 0; i < 5; i++) {
            McqQuestion q = mcqWithId(UUID.randomUUID());
            ids.add(q.getId());
            if (i < 4) seenIds.add(q.getId());
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        for (int i = 0; i < 3; i++) {
            TextQuestion q = textWithId(UUID.randomUUID());
            ids.add(q.getId());
            when(assessmentQuestionRepository.findById(q.getId())).thenReturn(Optional.of(q));
        }
        DocQuestion doc = docWithId(UUID.randomUUID());
        ids.add(doc.getId());
        when(assessmentQuestionRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        GroupQuestion group = groupWithId(UUID.randomUUID());
        ids.add(group.getId());
        when(assessmentQuestionRepository.findById(group.getId())).thenReturn(Optional.of(group));

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(seenIds);

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, ids, 60)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("MCQ");
    }

    @Test
    void generate_candidateNotFound_throwsNoSuchElementException() {
        UUID candidateId = UUID.randomUUID();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(candidateId.toString());
    }

    @Test
    void generate_missingQuestion_throwsNoSuchElementException() {
        UUID candidateId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidateWithId(candidateId)));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(assessmentQuestionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.generate(new AssessmentRequest(candidateId, List.of(missingId), 60)))
                .isInstanceOf(NoSuchElementException.class);
    }

    // --- Invitation token & email integration ---

    @Test
    void generate_emailServiceThrows_assessmentStillPersistedWithNonNullToken() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(fiveMcq());
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        Assessment firstSave = savedAssessmentFor(candidate);
        String generatedToken = "generated-test-token";
        Assessment secondSave = savedAssessmentFor(candidate);
        secondSave.setInvitationToken(generatedToken);

        when(assessmentRepository.save(any()))
                .thenReturn(firstSave)
                .thenReturn(secondSave);
        when(invitationTokenUtil.generateToken(any())).thenReturn(generatedToken);
        // emailService.sendInvitation is @Async and swallows exceptions —
        // we simulate a failure to verify the assessment is still returned correctly.
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(emailService).sendInvitation(any(), any(), any());

        AssessmentResponse response = assessmentService.generate(
                new AssessmentRequest(candidateId, List.of(), 60));

        assertThat(response).isNotNull();
        assertThat(response.invitationLink()).isNotNull();
        // Assessment was saved twice (once initially, once with the token)
        verify(assessmentRepository, times(2)).save(any(Assessment.class));
    }

    @Test
    void generate_invitationTokenSetOnAssessment() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(fiveMcq());
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        String expectedToken = "test-invitation-token";
        when(invitationTokenUtil.generateToken(any())).thenReturn(expectedToken);

        Assessment firstSave = savedAssessmentFor(candidate);
        Assessment secondSave = savedAssessmentFor(candidate);
        secondSave.setInvitationToken(expectedToken);
        when(assessmentRepository.save(any()))
                .thenReturn(firstSave)
                .thenReturn(secondSave);

        AssessmentResponse response = assessmentService.generate(
                new AssessmentRequest(candidateId, List.of(), 60));

        assertThat(response.invitationLink()).isEqualTo(expectedToken);

        // Verify the second save captured an assessment with the token set
        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository, times(2)).save(captor.capture());
        Assessment savedWithToken = captor.getAllValues().get(1);
        assertThat(savedWithToken.getInvitationToken()).isEqualTo(expectedToken);
    }

    @Test
    void generate_sendInvitationCalledWithCandidateEmailAndLink() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = candidateWithId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentRepository.findSeenQuestionIdsByCandidateAndYear(eq(candidateId), any(), any(), any()))
                .thenReturn(List.of());
        when(mcqQuestionRepository.findAll()).thenReturn(fiveMcq());
        when(textQuestionRepository.findAll()).thenReturn(threeText());
        when(docQuestionRepository.findAll()).thenReturn(List.of(docWithId(UUID.randomUUID())));
        when(groupQuestionRepository.findAll()).thenReturn(List.of(groupWithId(UUID.randomUUID())));

        String generatedToken = "my-unique-token";
        when(invitationTokenUtil.generateToken(any())).thenReturn(generatedToken);

        Assessment saved = savedAssessmentFor(candidate);
        when(assessmentRepository.save(any())).thenReturn(saved);

        assessmentService.generate(new AssessmentRequest(candidateId, List.of(), 60));

        verify(emailService).sendInvitation(
                eq("c@test.com"),
                eq("Candidate"),
                eq("http://localhost:4200/assessment/" + generatedToken));
    }
}
