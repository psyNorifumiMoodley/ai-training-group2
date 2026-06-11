package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record McqQuestionResponse(
        UUID id,
        List<QuestionBankResponse> questionBanks,
        String question,
        List<String> options,
        List<String> correctAnswers,
        boolean multiCorrect
) implements QuestionResponse {
}
