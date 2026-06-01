package com.psybergate.dap.dto;

import java.util.UUID;

public record AssessmentResponse(
        UUID id,
        UUID candidateId,
        String status,
        String invitationLink,
        int timeLimitMinutes,
        String createdAt
) {
}
