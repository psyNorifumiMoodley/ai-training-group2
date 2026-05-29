package com.psybergate.dap.dto;

import com.psybergate.dap.domain.QuestionType;

import java.util.List;
import java.util.UUID;

public record TextQuestionResponse(
        UUID id,
        QuestionType type,
        String category,
        String question,
        List<String> keywords
) implements QuestionResponse {
}