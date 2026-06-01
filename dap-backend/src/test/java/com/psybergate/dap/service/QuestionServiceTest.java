package com.psybergate.dap.service;

import com.psybergate.dap.domain.GroupQuestion;
import com.psybergate.dap.domain.TextQuestion;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Mock
    private TextQuestionRepository textQuestionRepository;

    @Mock
    private GroupQuestionRepository groupQuestionRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void createText_withKeywords_persistsAndReturnsResponse() {
        TextQuestionRequest request = new TextQuestionRequest("Java", "Describe OOP", List.of("encapsulation", "polymorphism"));

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
        assertThat(textResponse.category()).isEqualTo("Java");
    }

    @Test
    void createText_withEmptyKeywords_persistsWithEmptyList() {
        TextQuestionRequest request = new TextQuestionRequest("Java", "Describe OOP", List.of());

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
    void createGroup_withUnknownFollowUpId_throwsValidationException() {
        UUID unknownId = UUID.randomUUID();
        GroupQuestionRequest request = new GroupQuestionRequest("Java", "OOP concepts", true, List.of(unknownId));

        when(assessmentQuestionRepository.findTextQuestionsByIds(List.of(unknownId))).thenReturn(List.of());

        assertThatThrownBy(() -> questionService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("invalid or not text questions");
    }

    @Test
    void createGroup_withGroupQuestionId_throwsValidationException() {
        UUID groupId = UUID.randomUUID();
        GroupQuestionRequest request = new GroupQuestionRequest("Java", "OOP concepts", true, List.of(groupId));

        GroupQuestion existingGroup = new GroupQuestion();
        existingGroup.setId(groupId);
        existingGroup.setCategory("Java");
        existingGroup.setQuestion("Some group question");
        existingGroup.setOrdered(false);
        existingGroup.setFollowUpQuestions(new ArrayList<>());

        when(assessmentQuestionRepository.findTextQuestionsByIds(List.of(groupId)))
                .thenReturn(List.of(existingGroup));

        assertThatThrownBy(() -> questionService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Group questions cannot be used as follow-up questions");
    }

    @Test
    void createGroup_withValidFollowUpQuestions_preservesOrder() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        GroupQuestionRequest request = new GroupQuestionRequest("Java", "OOP concepts", true, List.of(id1, id2));

        TextQuestion t1 = new TextQuestion();
        t1.setId(id1);
        t1.setCategory("Java");
        t1.setQuestion("Q1");
        t1.setKeywords(List.of());

        TextQuestion t2 = new TextQuestion();
        t2.setId(id2);
        t2.setCategory("Java");
        t2.setQuestion("Q2");
        t2.setKeywords(List.of());

        when(assessmentQuestionRepository.findTextQuestionsByIds(List.of(id1, id2))).thenReturn(List.of(t1, t2));

        GroupQuestion saved = new GroupQuestion();
        saved.setId(UUID.randomUUID());
        saved.setCategory("Java");
        saved.setQuestion("OOP concepts");
        saved.setOrdered(true);
        saved.setFollowUpQuestions(List.of(t1, t2));
        when(groupQuestionRepository.save(any(GroupQuestion.class))).thenReturn(saved);

        QuestionResponse response = questionService.create(request);

        assertThat(response).isInstanceOf(GroupQuestionResponse.class);
        GroupQuestionResponse groupResponse = (GroupQuestionResponse) response;
        assertThat(groupResponse.followUpQuestions()).hasSize(2);
        assertThat(groupResponse.followUpQuestions().get(0).id()).isEqualTo(id1);
        assertThat(groupResponse.followUpQuestions().get(1).id()).isEqualTo(id2);
    }
}
