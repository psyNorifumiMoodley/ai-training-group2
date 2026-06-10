package com.psybergate.dap.controller;

import com.psybergate.dap.dto.DocQuestionRequest;
import com.psybergate.dap.dto.ErrorResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.dto.QuestionRequest;
import com.psybergate.dap.dto.QuestionResponse;
import com.psybergate.dap.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(@Valid @RequestBody QuestionRequest request) {
        if (request instanceof DocQuestionRequest) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ErrorResponse(410, "Gone",
                            "Doc question creation is deprecated. Use POST /api/questions with type CODING instead.",
                            Instant.now()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<QuestionResponse>> listQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID questionBankId) {
        return ResponseEntity.ok(questionService.list(page, size, questionBankId));
    }

    @GetMapping("/categories")
    public ResponseEntity<Void> categoriesRemoved() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable UUID id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
