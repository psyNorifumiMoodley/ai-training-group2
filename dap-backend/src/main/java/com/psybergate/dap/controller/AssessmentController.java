package com.psybergate.dap.controller;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.dto.AssessmentAccessResponse;
import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.dto.AssessmentSummaryResponse;
import com.psybergate.dap.dto.FeedbackItem;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.dto.ResponseRequest;
import com.psybergate.dap.dto.ResponseReviewItem;
import com.psybergate.dap.dto.SubmitRequest;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.FeedbackService;
import com.psybergate.dap.service.MarkingService;
import com.psybergate.dap.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final ResponseService responseService;
    private final MarkingService markingService;
    private final FeedbackService feedbackService;

    public AssessmentController(AssessmentService assessmentService,
                                ResponseService responseService,
                                MarkingService markingService,
                                FeedbackService feedbackService) {
        this.assessmentService = assessmentService;
        this.responseService = responseService;
        this.markingService = markingService;
        this.feedbackService = feedbackService;
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

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Void> startAssessment(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUser currentUser) {
        assessmentService.startAssessment(id, currentUser.getId());
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

    @GetMapping
    @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
    public ResponseEntity<Page<AssessmentSummaryResponse>> listAssessments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(markingService.listAssessments(status, page, size));
    }

    @GetMapping("/{id}/responses")
    @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
    public ResponseEntity<List<ResponseReviewItem>> getResponsesForReview(@PathVariable UUID id) {
        return ResponseEntity.ok(markingService.getResponsesForReview(id));
    }

    @PatchMapping("/{id}/responses/{responseId}")
    @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
    public ResponseEntity<Void> updateResponseFeedback(
            @PathVariable UUID id,
            @PathVariable UUID responseId,
            @Valid @RequestBody FeedbackUpdateRequest request) {
        markingService.updateResponseFeedback(id, responseId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalise")
    @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
    public ResponseEntity<Void> finaliseMarking(@PathVariable UUID id) {
        assessmentService.finalise(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/feedback/{questionId}")
    @PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
    public ResponseEntity<Void> updateFeedback(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody FeedbackUpdateRequest request) {
        feedbackService.updateFeedback(id, questionId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/feedback")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<FeedbackItem>> getCandidateFeedback(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(feedbackService.getCandidateFeedback(id, currentUser.getId()));
    }
}
