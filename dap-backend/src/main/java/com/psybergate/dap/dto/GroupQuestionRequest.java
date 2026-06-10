package com.psybergate.dap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record GroupQuestionRequest(
        @NotEmpty List<UUID> questionBankIds,
        @NotBlank String question,
        boolean ordered,
        @NotEmpty @Valid List<GroupChildRequest> children
) implements QuestionRequest {
}
