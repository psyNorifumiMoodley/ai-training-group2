package com.psybergate.dap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record McqPlusQuestionRequest(
        @NotEmpty List<UUID> questionBankIds,
        @NotBlank String question,
        @NotEmpty List<String> options,
        @NotEmpty List<String> correctAnswers,
        @NotBlank String followUpQuestion,
        List<String> followUpKeywords,
        @Min(1) int followUpMarks
) implements QuestionRequest {
}
