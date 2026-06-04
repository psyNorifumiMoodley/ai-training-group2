package com.psybergate.dap.dto;

import java.util.UUID;

public record AssessmentSummaryResponse(
        UUID id,
        String candidateName,
        String role,
        String bankName,
        String status,
        String assignedDate,
        String submittedAt,
        int timeLimitMinutes
) {}
