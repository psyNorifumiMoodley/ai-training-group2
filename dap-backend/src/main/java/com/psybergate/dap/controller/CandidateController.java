package com.psybergate.dap.controller;

import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.CandidateResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.CandidateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@PreAuthorize("hasRole('ADMIN') or hasRole('MARKER')")
public class CandidateController {

    private final CandidateService candidateService;
    private final AssessmentService assessmentService;

    public CandidateController(CandidateService candidateService, AssessmentService assessmentService) {
        this.candidateService = candidateService;
        this.assessmentService = assessmentService;
    }

    @PostMapping
    public ResponseEntity<CandidateResponse> register(@Valid @RequestBody CandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateService.register(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CandidateResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(candidateService.listCandidates(page, size));
    }

    @GetMapping("/{candidateId}/seen-questions")
    public ResponseEntity<List<UUID>> getSeenQuestions(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(assessmentService.getSeenQuestionIds(candidateId));
    }
}
