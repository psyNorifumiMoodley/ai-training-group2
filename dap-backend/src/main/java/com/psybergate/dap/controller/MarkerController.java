package com.psybergate.dap.controller;

import com.psybergate.dap.dto.MarkerRequest;
import com.psybergate.dap.dto.MarkerResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.service.MarkerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/markers")
@PreAuthorize("hasRole('ADMIN')")
public class MarkerController {

    private final MarkerService markerService;

    public MarkerController(MarkerService markerService) {
        this.markerService = markerService;
    }

    @PostMapping
    public ResponseEntity<MarkerResponse> register(@Valid @RequestBody MarkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(markerService.register(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MarkerResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(markerService.listMarkers(page, size));
    }
}
