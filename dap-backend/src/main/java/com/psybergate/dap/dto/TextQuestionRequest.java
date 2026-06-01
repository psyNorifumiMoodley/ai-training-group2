package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TextQuestionRequest(
        @NotBlank String category,
        @NotBlank String question,
        List<String> keywords
) implements QuestionRequest {
}