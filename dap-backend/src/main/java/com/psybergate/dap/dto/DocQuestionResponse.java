package com.psybergate.dap.dto;

import com.psybergate.dap.domain.QuestionType;

import java.util.UUID;

public record DocQuestionResponse(
        UUID id,
        QuestionType type,
        String category,
        String question
) implements QuestionResponse {
}
