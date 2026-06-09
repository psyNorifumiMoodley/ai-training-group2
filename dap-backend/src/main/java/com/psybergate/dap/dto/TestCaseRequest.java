package com.psybergate.dap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TestCaseRequest(
        String input,
        @NotBlank String expectedOutput,
        @Min(1) @Max(60) int timeoutSeconds,
        @Min(64) @Max(1024) int memoryMb
) {}
