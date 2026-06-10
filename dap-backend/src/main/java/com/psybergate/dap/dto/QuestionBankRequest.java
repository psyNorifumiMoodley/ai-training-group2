package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionBankRequest(
        @NotBlank String name
) {
}
