package com.psybergate.dap.dto;

import java.time.Instant;
import java.util.List;

public record CodeExecuteResponse(
        List<TestCaseResultResponse> results,
        Instant executedAt
) {
}
