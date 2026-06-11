package com.psybergate.dap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record TextQuestionRequest(
        @NotEmpty List<UUID> questionBankIds,
        @NotBlank String question,
        List<String> keywords,
        @Min(1) int marks
) implements QuestionRequest {
}
