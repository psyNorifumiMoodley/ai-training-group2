package com.psybergate.dap.dto;

import com.psybergate.dap.domain.Role;
import com.psybergate.dap.domain.ThemePreference;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        Role role,
        ThemePreference themePreference
) {
}
