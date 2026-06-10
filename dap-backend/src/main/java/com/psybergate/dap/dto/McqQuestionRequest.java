package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record McqQuestionRequest(
        @NotEmpty List<UUID> questionBankIds,
        @NotBlank String question,
        @NotEmpty List<String> options,
        @NotEmpty List<String> correctAnswers
) implements QuestionRequest {
}
