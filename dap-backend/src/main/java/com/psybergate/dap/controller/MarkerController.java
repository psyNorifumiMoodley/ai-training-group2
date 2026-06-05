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

import java.util.UUID;

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

    @PutMapping("/{markerId}")
    public ResponseEntity<MarkerResponse> update(
            @PathVariable UUID markerId,
            @Valid @RequestBody MarkerRequest request) {
        return ResponseEntity.ok(markerService.updateMarker(markerId, request));
    }

    @DeleteMapping("/{markerId}")
    public ResponseEntity<Void> delete(@PathVariable UUID markerId) {
        markerService.deleteMarker(markerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<MarkerResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(markerService.listMarkers(page, size, search, sortBy, sortDir));
    }
}
