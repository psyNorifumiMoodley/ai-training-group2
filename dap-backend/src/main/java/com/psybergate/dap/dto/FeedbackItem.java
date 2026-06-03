package com.psybergate.dap.dto;

import java.util.UUID;

public record FeedbackItem(UUID questionId, String questionBody, String feedbackText) {
}
