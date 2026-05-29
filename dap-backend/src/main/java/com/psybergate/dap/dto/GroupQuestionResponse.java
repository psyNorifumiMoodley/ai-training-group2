package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record GroupQuestionResponse(
        UUID id,
        String category,
        String question,
        boolean ordered,
        List<TextQuestionResponse> followUpQuestions
) implements QuestionResponse {
}
