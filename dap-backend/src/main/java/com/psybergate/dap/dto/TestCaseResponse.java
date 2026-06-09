package com.psybergate.dap.dto;

import java.util.UUID;

public record TestCaseResponse(
        UUID id,
        String input,
        String expectedOutput,
        int timeoutSeconds,
        int memoryMb,
        int ordinal
) {}
