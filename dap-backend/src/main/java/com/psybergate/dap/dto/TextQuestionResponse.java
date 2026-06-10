package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record TextQuestionResponse(
        UUID id,
        List<QuestionBankResponse> questionBanks,
        String question,
        List<String> keywords,
        int marks
) implements QuestionResponse {
}
