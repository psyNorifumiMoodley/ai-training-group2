package com.psybergate.dap.service;

import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AssessmentService {

    public AssessmentResponse generate(AssessmentRequest request) {
        return new AssessmentResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                request.candidateId(),
                "PENDING",
                "http://localhost:4200/assessment/stub-token",
                request.timeLimitMinutes(),
                Instant.now().toString()
        );
    }
}
