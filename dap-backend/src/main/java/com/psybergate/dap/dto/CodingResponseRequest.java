package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

public record CodingResponseRequest(@NotBlank String code) implements ResponseRequest {
}
