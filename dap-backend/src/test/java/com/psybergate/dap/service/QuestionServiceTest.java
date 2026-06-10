package com.psybergate.dap.service;

import com.psybergate.dap.domain.*;
import com.psybergate.dap.domain.GroupQuestion;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.*;
import com.psybergate.dap.repository.*;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.GroupQuestionRepository;
import com.psybergate.dap.repository.TextQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

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
    void createMcq_persistsMcqQuestion() {
        McqQuestionRequest request = new McqQuestionRequest(
                List.of(UUID.randomUUID()),
                "Which keyword declares a variable?",
                List.of("int", "for", "class"),
                List.of("int")
        );

        McqQuestion saved = new McqQuestion();
        saved.setId(UUID.randomUUID());
        saved.setCategory("Java");
        saved.setQuestion("Which keyword declares a variable?");
        saved.setOptions(List.of("int", "for", "class"));
        saved.setCorrectAnswers(List.of("int"));
        when(mcqQuestionRepository.save(any(McqQuestion.class))).thenReturn(saved);

        QuestionResponse response = questionService.create(request);

        assertThat(response).isInstanceOf(McqQuestionResponse.class);
        McqQuestionResponse mcqResponse = (McqQuestionResponse) response;
        assertThat(mcqResponse.options()).containsExactly("int", "for", "class");
        assertThat(mcqResponse.correctAnswers()).containsExactly("int");
    }

    @Test
    void createDoc_persistsDocQuestion() {
        DocQuestionRequest request = new DocQuestionRequest(
                List.of(UUID.randomUUID()),
                "Upload your design document",
                3
        );

        DocQuestion saved = new DocQuestion();
        saved.setId(UUID.randomUUID());
        saved.setCategory("Java");
        saved.setQuestion("Upload your design document");
        when(docQuestionRepository.save(any(DocQuestion.class))).thenReturn(saved);

        QuestionResponse response = questionService.create(request);

        assertThat(response).isInstanceOf(DocQuestionResponse.class);
        assertThat(((DocQuestionResponse) response).question()).isEqualTo("Upload your design document");
    }

    @Test
    void createText_withKeywords_persistsAndReturnsResponse() {
        TextQuestionRequest request = new TextQuestionRequest(
                List.of(UUID.randomUUID()),
                "Describe OOP",
                List.of("encapsulation", "polymorphism"),
                5
        );

        TextQuestion saved = new TextQuestion();
        saved.setId(UUID.randomUUID());
        saved.setCategory("Java");
        saved.setQuestion("Describe OOP");
        saved.setKeywords(List.of("encapsulation", "polymorphism"));
        when(textQuestionRepository.save(any(TextQuestion.class))).thenReturn(saved);

        QuestionResponse response = questionService.create(request);

        assertThat(response).isInstanceOf(TextQuestionResponse.class);
        TextQuestionResponse textResponse = (TextQuestionResponse) response;
        assertThat(textResponse.keywords()).containsExactly("encapsulation", "polymorphism");
        assertThat(textResponse.questionBanks()).isEmpty();
    }

    @Test
    void createText_withEmptyKeywords_persistsWithEmptyList() {
        TextQuestionRequest request = new TextQuestionRequest(
                List.of(UUID.randomUUID()),
                "Describe OOP",
                List.of(),
                5
        );

        ArgumentCaptor<TextQuestion> captor = ArgumentCaptor.forClass(TextQuestion.class);
        TextQuestion saved = new TextQuestion();
        saved.setId(UUID.randomUUID());
        saved.setCategory("Java");
        saved.setQuestion("Describe OOP");
        saved.setKeywords(List.of());
        when(textQuestionRepository.save(captor.capture())).thenReturn(saved);

        questionService.create(request);

        assertThat(captor.getValue().getKeywords()).isEmpty();
    }

    @Test
    void createGroup_returnsGroupWithEmptyChildren() {
        GroupQuestionRequest request = new GroupQuestionRequest(
                List.of(UUID.randomUUID()),
                "OOP concepts",
                true,
                List.of(new GroupChildRequest("What is encapsulation?", List.of(), 2))
        );

        GroupQuestion saved = new GroupQuestion();
        saved.setId(UUID.randomUUID());
        saved.setQuestion("OOP concepts");
        saved.setOrdered(true);
        saved.setFollowUpQuestions(new ArrayList<>());
        when(groupQuestionRepository.save(any(GroupQuestion.class))).thenReturn(saved);

        QuestionResponse response = questionService.create(request);

        assertThat(response).isInstanceOf(GroupQuestionResponse.class);
        GroupQuestionResponse groupResponse = (GroupQuestionResponse) response;
        assertThat(groupResponse.children()).isEmpty();
        assertThat(groupResponse.ordered()).isTrue();
    }
}
