package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record AssessmentAccessResponse(
        UUID assessmentId,
        List<QuestionResponse> questions,
        int remainingSeconds,
        String candidateToken,
        boolean alreadyStarted
) {
}
