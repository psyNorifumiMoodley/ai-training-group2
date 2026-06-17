package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.CodingQuestion;
import com.psybergate.dap.domain.CodingQuestionLanguage;
import com.psybergate.dap.domain.CodingResponse;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.QuestionGroupResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TestCase;
import com.psybergate.dap.domain.TestCaseResult;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.dto.CodeExecuteResponse;
import com.psybergate.dap.dto.CodingResponseRequest;
import com.psybergate.dap.dto.GroupResponseRequest;
import com.psybergate.dap.dto.McqResponseRequest;
import com.psybergate.dap.dto.TestCaseResultResponse;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.CodingQuestionRepository;
import com.psybergate.dap.repository.CodingResponseRepository;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.ResponseRepository;
import com.psybergate.dap.repository.TestCaseResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseServiceTest {

    @Mock private ResponseRepository responseRepository;
    @Mock private McqQuestionRepository mcqQuestionRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private CodingResponseRepository codingResponseRepository;
    @Mock private CodeExecutionService codeExecutionService;
    @Mock private TestCaseResultRepository testCaseResultRepository;
    @Mock private CodingQuestionRepository codingQuestionRepository;

    private ResponseService responseService;

    @BeforeEach
    void setUp() {
        responseService = new ResponseService(
                responseRepository, mcqQuestionRepository, assessmentRepository,
                assessmentQuestionRepository, codingResponseRepository, codeExecutionService,
                testCaseResultRepository, codingQuestionRepository);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Assessment assessmentWithStatus(UUID id, AssessmentStatus status) {
        Assessment a = Assessment.builder()
                .status(status)
                .timeLimitMinutes(60)
                .build();
        a.setId(id);
        return a;
    }

    private Assessment assessmentWithCandidate(UUID assessmentId, UUID candidateId, AssessmentStatus status) {
        AppUser user = new AppUser();
        user.setId(candidateId);
        Candidate candidate = Candidate.builder().user(user).build();
        candidate.setId(candidateId);
        Assessment a = Assessment.builder()
                .status(status)
                .timeLimitMinutes(60)
                .candidate(candidate)
                .build();
        a.setId(assessmentId);
        return a;
    }

    private McqQuestion mcqQuestion(UUID id) {
        McqQuestion q = new McqQuestion(List.of("A", "B", "C"), List.of("A"));
        q.setId(id);
        q.setQuestion("Which is correct?");
        return q;
    }

    private TextQuestion textQuestion(UUID id) {
        TextQuestion q = new TextQuestion();
        q.setId(id);
        q.setQuestion("Explain this.");
        return q;
    }

    private CodingQuestion codingQuestion(UUID id) {
        CodingQuestion q = CodingQuestion.builder()
                .language(CodingQuestionLanguage.JAVA)
                .build();
        q.setId(id);
        q.setQuestion("Write a solution.");
        return q;
    }

    // ── saveResponse: MCQ ─────────────────────────────────────────────────────

    @Test
    void saveResponse_mcq_createsNewMcqResponseWithSelectedAnswers() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        Assessment assessment = assessmentWithStatus(assessmentId, AssessmentStatus.IN_PROGRESS);
        McqQuestion question = mcqQuestion(questionId);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(responseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
                .thenReturn(Optional.empty());
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        McqResponseRequest request = new McqResponseRequest(List.of("A", "B"));
        responseService.saveResponse(assessmentId, questionId, request);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(McqResponse.class);
        McqResponse saved = (McqResponse) captor.getValue();
        assertThat(saved.getSelectedAnswers()).containsExactly("A", "B");
        assertThat(saved.getCorrect()).isNull();
    }

    @Test
    void saveResponse_submittedAssessment_throwsConflictException() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessmentWithStatus(assessmentId, AssessmentStatus.SUBMITTED)));

        assertThatThrownBy(() -> responseService.saveResponse(
                assessmentId, questionId, new McqResponseRequest(List.of("A"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("submitted");
    }

    @Test
    void saveResponse_markedAssessment_throwsConflictException() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessmentWithStatus(assessmentId, AssessmentStatus.MARKED)));

        assertThatThrownBy(() -> responseService.saveResponse(
                assessmentId, questionId, new McqResponseRequest(List.of("A"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("submitted");
    }

    @Test
    void saveResponse_pendingAssessment_throwsConflictException() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessmentWithStatus(assessmentId, AssessmentStatus.PENDING)));

        assertThatThrownBy(() -> responseService.saveResponse(
                assessmentId, questionId, new McqResponseRequest(List.of("A"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not in progress");
    }

    @Test
    void saveResponse_existingMcqResponse_updatesSelectedAnswers() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        Assessment assessment = assessmentWithStatus(assessmentId, AssessmentStatus.IN_PROGRESS);
        McqQuestion question = mcqQuestion(questionId);

        McqResponse existing = McqResponse.builder()
                .selectedAnswers(List.of("A"))
                .build();
        existing.setId(UUID.randomUUID());
        existing.setAssessment(assessment);
        existing.setQuestion(question);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(responseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
                .thenReturn(Optional.of(existing));
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        McqResponseRequest request = new McqResponseRequest(List.of("B", "C"));
        responseService.saveResponse(assessmentId, questionId, request);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());

        McqResponse updated = (McqResponse) captor.getValue();
        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getSelectedAnswers()).containsExactly("B", "C");
    }

    @Test
    void saveResponse_groupResponse_persistsChildResponses() {
        UUID assessmentId = UUID.randomUUID();
        UUID groupQuestionId = UUID.randomUUID();

        Assessment assessment = assessmentWithStatus(assessmentId, AssessmentStatus.IN_PROGRESS);
        McqQuestion groupQuestion = mcqQuestion(groupQuestionId);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentQuestionRepository.findById(groupQuestionId)).thenReturn(Optional.of(groupQuestion));
        when(responseRepository.findByAssessmentIdAndQuestionId(assessmentId, groupQuestionId))
                .thenReturn(Optional.empty());
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GroupResponseRequest request = new GroupResponseRequest(List.of("My answer"));
        responseService.saveResponse(assessmentId, groupQuestionId, request);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(QuestionGroupResponse.class);
        QuestionGroupResponse savedGroup = (QuestionGroupResponse) captor.getValue();
        assertThat(savedGroup.getChildResponses()).hasSize(1);
        TextResponse childText = (TextResponse) savedGroup.getChildResponses().get(0);
        assertThat(childText.getAnswer()).isEqualTo("My answer");
    }

    @Test
    void saveResponse_assessmentNotFound_throwsNoSuchElementException() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> responseService.saveResponse(
                assessmentId, questionId, new McqResponseRequest(List.of("A"))))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(assessmentId.toString());
    }

    // ── saveResponse: CodingResponseRequest ──────────────────────────────────

    @Test
    void saveResponse_coding_createsNewCodingResponse() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        Assessment assessment = assessmentWithStatus(assessmentId, AssessmentStatus.IN_PROGRESS);
        CodingQuestion question = codingQuestion(questionId);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(responseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
                .thenReturn(Optional.empty());
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseService.saveResponse(assessmentId, questionId, new CodingResponseRequest("class Main {}"));

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(CodingResponse.class);
        CodingResponse saved = (CodingResponse) captor.getValue();
        assertThat(saved.getCode()).isEqualTo("class Main {}");
    }

    @Test
    void saveResponse_coding_updatesExistingCodingResponse() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        Assessment assessment = assessmentWithStatus(assessmentId, AssessmentStatus.IN_PROGRESS);
        CodingQuestion question = codingQuestion(questionId);

        CodingResponse existing = CodingResponse.builder().code("old code").build();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(responseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
                .thenReturn(Optional.of(existing));
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseService.saveResponse(assessmentId, questionId, new CodingResponseRequest("new code"));

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());

        CodingResponse updated = (CodingResponse) captor.getValue();
        assertThat(updated.getId()).isEqualTo(existingId);
        assertThat(updated.getCode()).isEqualTo("new code");
    }

    @Test
    void saveResponse_coding_submittedAssessment_throwsConflict() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessmentWithStatus(assessmentId, AssessmentStatus.SUBMITTED)));

        assertThatThrownBy(() -> responseService.saveResponse(
                assessmentId, questionId, new CodingResponseRequest("code")))
                .isInstanceOf(ConflictException.class);
    }

    // ── executeCode ───────────────────────────────────────────────────────────

    @Test
    void executeCode_success_returnsCodeExecuteResponse() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID testCaseId = UUID.randomUUID();

        Assessment assessment = assessmentWithCandidate(assessmentId, candidateId, AssessmentStatus.IN_PROGRESS);

        CodingResponse codingResponse = CodingResponse.builder().code("class Main {}").build();
        codingResponse.setId(UUID.randomUUID());

        TestCase tc = TestCase.builder()
                .input("1")
                .expectedOutput("1")
                .timeoutSeconds(10)
                .memoryMb(256)
                .ordinal(1)
                .build();
        tc.setId(testCaseId);

        CodingQuestion codingQuestion = CodingQuestion.builder()
                .language(CodingQuestionLanguage.JAVA)
                .build();
        codingQuestion.setId(questionId);
        codingQuestion.getTestCases().add(tc);

        TestCaseResultResponse resultResponse = new TestCaseResultResponse(
                testCaseId, true, "1", 100L, 1L, null, 1);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(codingResponseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
                .thenReturn(Optional.of(codingResponse));
        when(codingQuestionRepository.findById(questionId)).thenReturn(Optional.of(codingQuestion));
        when(codeExecutionService.execute(codingQuestion, "class Main {}"))
                .thenReturn(List.of(resultResponse));
        when(codingResponseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeExecuteResponse response = responseService.executeCode(assessmentId, questionId, candidateId);

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).passed()).isTrue();
        assertThat(response.executedAt()).isNotNull();
        verify(testCaseResultRepository).deleteAllByCodingResponseId(codingResponse.getId());
        verify(testCaseResultRepository).saveAll(any());
    }

    @Test
    void executeCode_noCodingResponseSaved_throwsNoSuchElement() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        Assessment assessment = assessmentWithCandidate(assessmentId, candidateId, AssessmentStatus.IN_PROGRESS);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(codingResponseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> responseService.executeCode(assessmentId, questionId, candidateId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void executeCode_submittedAssessment_throwsConflict() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        Assessment assessment = assessmentWithCandidate(assessmentId, candidateId, AssessmentStatus.SUBMITTED);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> responseService.executeCode(assessmentId, questionId, candidateId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void executeCode_wrongCandidate_throwsAccessDenied() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID realCandidateId = UUID.randomUUID();
        UUID wrongUserId = UUID.randomUUID();

        Assessment assessment = assessmentWithCandidate(assessmentId, realCandidateId, AssessmentStatus.IN_PROGRESS);

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> responseService.executeCode(assessmentId, questionId, wrongUserId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── getResponsesForAssessment ─────────────────────────────────────────────

    @Test
    void getResponsesForAssessment_returnsRepositoryResults() {
        UUID assessmentId = UUID.randomUUID();
        McqResponse r = McqResponse.builder().selectedAnswers(List.of("A")).build();
        r.setId(UUID.randomUUID());

        when(responseRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(r));

        List<Response> result = responseService.getResponsesForAssessment(assessmentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(McqResponse.class);
    }
}
