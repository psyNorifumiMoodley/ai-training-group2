package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record DocQuestionResponse(
        UUID id,
        List<QuestionBankResponse> questionBanks,
        String question,
        int marks
) implements QuestionResponse {
}
