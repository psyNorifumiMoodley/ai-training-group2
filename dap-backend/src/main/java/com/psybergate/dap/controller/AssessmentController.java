package com.psybergate.dap.controller;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.dto.AssessmentAccessResponse;
import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.dto.ResponseRequest;
import com.psybergate.dap.dto.SubmitRequest;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final ResponseService responseService;

    public AssessmentController(AssessmentService assessmentService, ResponseService responseService) {
        this.assessmentService = assessmentService;
        this.responseService = responseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MARKER') or hasRole('ADMIN')")
    public ResponseEntity<AssessmentResponse> generateAssessment(@Valid @RequestBody AssessmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.generate(request));
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<AssessmentAccessResponse> accessAssessment(@PathVariable String token) {
        return ResponseEntity.ok(assessmentService.access(token));
    }

    @PutMapping("/{id}/responses/{questionId}")
    public ResponseEntity<Void> saveResponse(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody ResponseRequest request) {
        responseService.saveResponse(id, questionId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<AssessmentResponse> submitAssessment(
            @PathVariable UUID id,
            @RequestBody(required = false) SubmitRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(assessmentService.submit(id, currentUser.getId()));
    }
}
