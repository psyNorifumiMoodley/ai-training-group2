package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.ChangePasswordRequest;
import com.psybergate.dap.dto.UpdateProfileRequest;
import com.psybergate.dap.dto.UpdateThemeRequest;
import com.psybergate.dap.dto.UserProfileResponse;
import com.psybergate.dap.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSettingsService(AppUserRepository appUserRepository,
                               PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(AppUser principal) {
        return toResponse(principal);
    }

    @Transactional
    public UserProfileResponse updateProfile(AppUser principal, UpdateProfileRequest request) {
        if (appUserRepository.existsByEmailAndIdNot(request.email(), principal.getId())) {
            throw new ConflictException("Email address is already in use");
        }
        principal.setName(request.name());
        principal.setEmail(request.email());
        AppUser saved = appUserRepository.save(principal);
        return toResponse(saved);
    }

    @Transactional
    public void changePassword(AppUser principal, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), principal.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException("New password and confirm password do not match");
        }
        principal.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(principal);
    }

    @Transactional
    public UserProfileResponse updateTheme(AppUser principal, UpdateThemeRequest request) {
        principal.setThemePreference(request.theme());
        AppUser saved = appUserRepository.save(principal);
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getThemePreference()
        );
    }
}
