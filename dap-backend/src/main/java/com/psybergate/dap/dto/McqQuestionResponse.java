package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record McqQuestionResponse(
        UUID id,
        String category,
        String question,
        List<String> options,
        List<String> correctAnswers,
        boolean multiCorrect
) implements QuestionResponse {
}