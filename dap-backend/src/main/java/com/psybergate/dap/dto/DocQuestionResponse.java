package com.psybergate.dap.dto;

import java.util.UUID;

public record DocQuestionResponse(
        UUID id,
        String category,
        String question
) implements QuestionResponse {
}
