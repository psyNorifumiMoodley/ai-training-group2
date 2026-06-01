package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

public record DocQuestionRequest(
        @NotBlank String category,
        @NotBlank String question
) implements QuestionRequest {
}