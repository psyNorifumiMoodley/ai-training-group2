package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record McqPlusQuestionResponse(
        UUID id,
        List<QuestionBankResponse> questionBanks,
        String question,
        List<String> options,
        List<String> correctAnswers,
        boolean multiCorrect,
        String followUpQuestion,
        List<String> followUpKeywords,
        int followUpMarks,
        int totalMarks
) implements QuestionResponse {
}
