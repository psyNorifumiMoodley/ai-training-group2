package com.psybergate.dap.dto;

import com.psybergate.dap.domain.QuestionType;

import java.util.List;
import java.util.UUID;

public record McqQuestionResponse(
        UUID id,
        QuestionType type,
        String category,
        String question,
        List<String> options,
        List<String> correctAnswers
) implements QuestionResponse {
}