package com.psybergate.dap.service;

import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.McqQuestionRequest;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.DocQuestionRepository;
import com.psybergate.dap.repository.GroupQuestionRepository;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.TextQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McqQuestionTest {

    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Mock
    private McqQuestionRepository mcqQuestionRepository;

    @Mock
    private DocQuestionRepository docQuestionRepository;

    @Mock
    private TextQuestionRepository textQuestionRepository;

    @Mock
    private GroupQuestionRepository groupQuestionRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void createMcq_withEmptyOptions_throwsValidationException() {
        McqQuestionRequest request = new McqQuestionRequest(List.of(), "Which is correct?", List.of(), List.of("A"));

        assertThatThrownBy(() -> questionService.createMcq(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one option");
    }

    @Test
    void createMcq_withEmptyCorrectAnswers_throwsValidationException() {
        McqQuestionRequest request = new McqQuestionRequest(List.of(), "Which is correct?", List.of("A", "B"), List.of());

        assertThatThrownBy(() -> questionService.createMcq(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one correct answer");
    }

    @Test
    void createMcq_withCorrectAnswerNotInOptions_throwsValidationException() {
        McqQuestionRequest request = new McqQuestionRequest(List.of(), "Which is correct?", List.of("A", "B"), List.of("C"));

        assertThatThrownBy(() -> questionService.createMcq(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("correct answers must be present in the options");
    }

    @Test
    void createMcq_withValidMultiCorrectRequest_persistsQuestion() {
        McqQuestionRequest request = new McqQuestionRequest(List.of(), "Which is correct?", List.of("A", "B", "C"), List.of("A", "C"));

        McqQuestion saved = new McqQuestion();
        saved.setId(UUID.randomUUID());
        saved.setCategory("Java");
        saved.setQuestion("Which is correct?");
        saved.setOptions(List.of("A", "B", "C"));
        saved.setCorrectAnswers(List.of("A", "C"));
        when(mcqQuestionRepository.save(any(McqQuestion.class))).thenReturn(saved);

        McqQuestion result = questionService.createMcq(request);

        org.assertj.core.api.Assertions.assertThat(result.getOptions()).containsExactly("A", "B", "C");
        org.assertj.core.api.Assertions.assertThat(result.getCorrectAnswers()).containsExactly("A", "C");
    }
}
