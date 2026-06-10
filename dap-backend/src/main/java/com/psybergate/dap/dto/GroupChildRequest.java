package com.psybergate.dap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GroupChildRequest(
        @NotBlank String questionText,
        List<String> keywords,
        @Min(1) int marks
) {
}
