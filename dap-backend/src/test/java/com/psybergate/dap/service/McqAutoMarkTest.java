package com.psybergate.dap.service;

import com.psybergate.dap.domain.*;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.ResponseRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McqAutoMarkTest {

    @Mock
    private ResponseRepository responseRepository;
    @Mock
    private McqQuestionRepository mcqQuestionRepository;

    private ResponseService responseService;

    @BeforeEach
    void setUp() {
        responseService = new ResponseService(responseRepository, mcqQuestionRepository);
    }

    private McqQuestion questionWithCorrectAnswers(List<String> correctAnswers) {
        McqQuestion q = new McqQuestion(List.of("A", "B", "C"), correctAnswers);
        q.setId(UUID.randomUUID());
        q.setCategory("Java");
        q.setQuestion("Pick the correct answer(s).");
        return q;
    }

    private McqResponse responseWithSelectedAnswers(McqQuestion question, List<String> selected) {
        McqResponse r = McqResponse.builder().selectedAnswers(selected).build();
        r.setId(UUID.randomUUID());
        AssessmentQuestion stub = new McqQuestion();
        stub.setId(question.getId());
        r.setQuestion(stub);
        return r;
    }

    @Test
    void singleCorrectAnswer_exactMatch_markedCorrect() {
        UUID assessmentId = UUID.randomUUID();
        McqQuestion question = questionWithCorrectAnswers(List.of("A"));
        McqResponse response = responseWithSelectedAnswers(question, List.of("A"));

        when(responseRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(response));
        when(mcqQuestionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseService.autoMarkMcqResponses(assessmentId);

        ArgumentCaptor<McqResponse> captor = ArgumentCaptor.forClass(McqResponse.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrect()).isTrue();
    }

    @Test
    void multiCorrect_allSelected_markedCorrect() {
        UUID assessmentId = UUID.randomUUID();
        McqQuestion question = questionWithCorrectAnswers(List.of("A", "B"));
        McqResponse response = responseWithSelectedAnswers(question, List.of("B", "A"));

        when(responseRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(response));
        when(mcqQuestionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseService.autoMarkMcqResponses(assessmentId);

        ArgumentCaptor<McqResponse> captor = ArgumentCaptor.forClass(McqResponse.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrect()).isTrue();
    }

    @Test
    void multiCorrect_partialSelection_markedIncorrect() {
        UUID assessmentId = UUID.randomUUID();
        McqQuestion question = questionWithCorrectAnswers(List.of("A", "B"));
        McqResponse response = responseWithSelectedAnswers(question, List.of("A"));

        when(responseRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(response));
        when(mcqQuestionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseService.autoMarkMcqResponses(assessmentId);

        ArgumentCaptor<McqResponse> captor = ArgumentCaptor.forClass(McqResponse.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrect()).isFalse();
    }

    @Test
    void multiCorrect_wrongAnswersAdded_markedIncorrect() {
        UUID assessmentId = UUID.randomUUID();
        McqQuestion question = questionWithCorrectAnswers(List.of("A"));
        McqResponse response = responseWithSelectedAnswers(question, List.of("A", "C"));

        when(responseRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(response));
        when(mcqQuestionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseService.autoMarkMcqResponses(assessmentId);

        ArgumentCaptor<McqResponse> captor = ArgumentCaptor.forClass(McqResponse.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrect()).isFalse();
    }
}
