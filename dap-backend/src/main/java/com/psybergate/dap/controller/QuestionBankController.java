package com.psybergate.dap.controller;

import com.psybergate.dap.dto.QuestionBankRequest;
import com.psybergate.dap.dto.QuestionBankResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/question-banks")
@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
public class QuestionBankController {

    @GetMapping
    public ResponseEntity<List<QuestionBankResponse>> listQuestionBanks() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<QuestionBankResponse> createQuestionBank(@Valid @RequestBody QuestionBankRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new QuestionBankResponse(UUID.randomUUID(), request.name()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionBankResponse> updateQuestionBank(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionBankRequest request) {
        return ResponseEntity.ok(new QuestionBankResponse(id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }
}
