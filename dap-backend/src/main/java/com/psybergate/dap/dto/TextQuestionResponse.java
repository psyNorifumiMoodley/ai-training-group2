package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record TextQuestionResponse(
        UUID id,
        String category,
        String question,
        List<String> keywords
) implements QuestionResponse {
}