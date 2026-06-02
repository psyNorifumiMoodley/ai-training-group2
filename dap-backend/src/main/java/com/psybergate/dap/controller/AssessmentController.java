package com.psybergate.dap.controller;

import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MARKER') or hasRole('ADMIN')")
    public ResponseEntity<AssessmentResponse> generateAssessment(@Valid @RequestBody AssessmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.generate(request));
    }
}
