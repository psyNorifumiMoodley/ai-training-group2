package com.psybergate.dap.controller;

import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.dto.QuestionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasAnyRole('MARKER', 'ADMIN')")
public class QuestionController {

    @PostMapping
    public ResponseEntity<Void> createQuestion(@RequestBody Object request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<QuestionResponse>> listQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(new PageResponse<>(List.of(), 0, 0));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> listCategories() {
        return ResponseEntity.ok(List.of());
    }
}
