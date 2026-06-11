package com.psybergate.dap.controller;

import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.dto.QuestionBankRequest;
import com.psybergate.dap.dto.QuestionBankResponse;
import com.psybergate.dap.service.QuestionBankService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/question-banks")
@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<QuestionBankResponse>> listQuestionBanks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(questionBankService.list(page, size));
    }

    @PostMapping
    public ResponseEntity<QuestionBankResponse> createQuestionBank(@Valid @RequestBody QuestionBankRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionBankResponse> updateQuestionBank(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionBankRequest request) {
        return ResponseEntity.ok(questionBankService.rename(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable UUID id) {
        questionBankService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
