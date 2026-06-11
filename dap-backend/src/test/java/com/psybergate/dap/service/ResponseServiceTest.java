package com.psybergate.dap.service;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.QuestionGroupResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.dto.GroupResponseRequest;
import com.psybergate.dap.dto.McqResponseRequest;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private ResponseRepository responseRepository;
    @Mock
    private McqQuestionRepository mcqQuestionRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;

    private ResponseService responseService;

    @BeforeEach
    void setUp() {
        responseService = new ResponseService(responseRepository, mcqQuestionRepository,
                assessmentRepository, assessmentQuestionRepository);
    }

    private Assessment assessmentWithStatus(UUID id, AssessmentStatus status) {
        Assessment a = Assessment.builder()
                .status(status)
                .timeLimitMinutes(60)
                .build();
        a.setId(id);
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

    // --- saveResponse: MCQ creates McqResponse with correct selectedAnswers ---

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

    // --- saveResponse: SUBMITTED assessment -> 409 ---

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

    // --- saveResponse: MARKED assessment -> 409 ---

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

    // --- saveResponse: PENDING assessment -> 409 ---

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

    // --- saveResponse: upsert updates existing response ---

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

    // --- saveResponse: GroupResponse children persisted correctly ---

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
        assertThat(savedGroup.getChildResponses().get(0)).isInstanceOf(TextResponse.class);
        TextResponse childText = (TextResponse) savedGroup.getChildResponses().get(0);
        assertThat(childText.getAnswer()).isEqualTo("My answer");
    }

    // --- saveResponse: assessment not found -> 404 ---

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

    // --- getResponsesForAssessment delegates to repository ---

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
