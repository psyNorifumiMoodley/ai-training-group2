package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record GroupQuestionRequest(
        @NotBlank String category,
        @NotBlank String question,
        boolean ordered,
        List<UUID> followUpQuestionIds
) implements QuestionRequest {
}
