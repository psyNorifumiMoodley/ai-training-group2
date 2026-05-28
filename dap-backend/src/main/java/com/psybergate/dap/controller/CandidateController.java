package com.psybergate.dap.controller;

import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.CandidateResponse;
import com.psybergate.dap.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@PreAuthorize("hasRole('ADMIN')")
public class CandidateController {

    @PostMapping
    public ResponseEntity<CandidateResponse> register(@Valid @RequestBody CandidateRequest request) {
        CandidateResponse stub = new CandidateResponse(UUID.randomUUID(), request.name(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(stub);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CandidateResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(List.of(), 0, 0));
    }
}
