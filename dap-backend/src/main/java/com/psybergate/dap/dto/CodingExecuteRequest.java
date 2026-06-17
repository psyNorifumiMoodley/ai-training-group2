package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

public record CodingExecuteRequest(@NotBlank String code) {}
