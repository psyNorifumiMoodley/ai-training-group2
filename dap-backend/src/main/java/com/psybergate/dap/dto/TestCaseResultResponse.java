package com.psybergate.dap.dto;

import java.util.UUID;

public record TestCaseResultResponse(
        UUID testCaseId,
        boolean passed,
        String actualOutput,
        Long executionTimeMs,
        Long memoryUsedMb,
        String errorMessage,
        int ordinal
) {
}
