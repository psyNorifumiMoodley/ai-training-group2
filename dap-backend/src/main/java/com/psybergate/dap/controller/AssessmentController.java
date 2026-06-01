package com.psybergate.dap.controller;

import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    @PostMapping
    @PreAuthorize("hasRole('MARKER')")
    public ResponseEntity<AssessmentResponse> generateAssessment(@Valid @RequestBody AssessmentRequest request) {
        AssessmentResponse stub = new AssessmentResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                request.candidateId(),
                "PENDING",
                "http://localhost:4200/assessment/stub-token",
                request.timeLimitMinutes(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(stub);
    }
}
