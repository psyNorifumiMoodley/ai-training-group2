package com.psybergate.dap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssessmentRequest(
        @NotNull UUID candidateId,
        @NotEmpty List<UUID> questionIds,
        @Min(5) int timeLimitMinutes
) {
}
