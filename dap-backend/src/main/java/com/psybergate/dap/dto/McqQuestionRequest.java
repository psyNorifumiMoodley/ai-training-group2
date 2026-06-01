package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record McqQuestionRequest(
        @NotBlank String category,
        @NotBlank String question,
        @NotEmpty List<String> options,
        @NotEmpty List<String> correctAnswers
) implements QuestionRequest {
}