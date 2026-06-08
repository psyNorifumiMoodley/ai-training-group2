package com.psybergate.dap.controller;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.dto.ChangePasswordRequest;
import com.psybergate.dap.dto.UpdateProfileRequest;
import com.psybergate.dap.dto.UpdateThemeRequest;
import com.psybergate.dap.dto.UserProfileResponse;
import com.psybergate.dap.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("hasRole('ADMIN') or hasRole('MARKER')")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal AppUser principal) {
        return ResponseEntity.ok(userSettingsService.getProfile(principal));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AppUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userSettingsService.updateProfile(principal, request));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AppUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userSettingsService.changePassword(principal, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/theme")
    public ResponseEntity<UserProfileResponse> updateTheme(
            @AuthenticationPrincipal AppUser principal,
            @Valid @RequestBody UpdateThemeRequest request) {
        return ResponseEntity.ok(userSettingsService.updateTheme(principal, request));
    }
}
