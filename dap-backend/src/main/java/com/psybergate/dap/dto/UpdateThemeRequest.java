package com.psybergate.dap.dto;

import com.psybergate.dap.domain.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateThemeRequest(
        @NotNull ThemePreference theme
) {
}
