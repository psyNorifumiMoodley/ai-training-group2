package com.psybergate.dap.controller;

import com.psybergate.dap.dto.TestCaseRequest;
import com.psybergate.dap.dto.TestCaseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coding-questions")
@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
public class CodingQuestionController {

    @PostMapping("/{questionId}/test-cases")
    public ResponseEntity<TestCaseResponse> addTestCase(
            @PathVariable UUID questionId,
            @Valid @RequestBody TestCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{questionId}/test-cases")
    public ResponseEntity<List<TestCaseResponse>> getTestCases(@PathVariable UUID questionId) {
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{questionId}/test-cases/{testCaseId}")
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable UUID questionId,
            @PathVariable UUID testCaseId,
            @Valid @RequestBody TestCaseRequest request) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{questionId}/test-cases/{testCaseId}")
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable UUID questionId,
            @PathVariable UUID testCaseId) {
        return ResponseEntity.noContent().build();
    }
}
