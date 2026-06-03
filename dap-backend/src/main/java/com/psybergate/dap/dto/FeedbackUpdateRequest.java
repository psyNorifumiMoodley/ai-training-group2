package com.psybergate.dap.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedbackUpdateRequest(@NotBlank String feedbackText) {
}
