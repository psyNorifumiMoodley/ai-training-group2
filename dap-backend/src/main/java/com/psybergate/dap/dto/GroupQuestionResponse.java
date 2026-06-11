package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record GroupQuestionResponse(
        UUID id,
        List<QuestionBankResponse> questionBanks,
        String question,
        boolean ordered,
        List<GroupChildResponse> children,
        int totalMarks
) implements QuestionResponse {
}
