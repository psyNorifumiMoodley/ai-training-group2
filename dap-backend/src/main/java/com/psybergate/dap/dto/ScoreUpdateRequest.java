package com.psybergate.dap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreUpdateRequest(@NotNull @Min(0) Integer score) {}
