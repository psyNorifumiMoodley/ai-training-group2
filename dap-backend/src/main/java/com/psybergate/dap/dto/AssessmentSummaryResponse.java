package com.psybergate.dap.dto;

import java.util.UUID;

public record AssessmentSummaryResponse(UUID id, String candidateName, String submittedAt, String status) {
}
