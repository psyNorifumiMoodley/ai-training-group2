package com.psybergate.dap.dto;

import com.psybergate.dap.domain.QuestionType;

import java.util.List;
import java.util.UUID;

public record GroupQuestionResponse(
        UUID id,
        QuestionType type,
        String category,
        String question,
        boolean ordered,
        List<TextQuestionResponse> followUpQuestions
) implements QuestionResponse {
}
