package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record ResponseReviewItem(
        UUID responseId,
        UUID questionId,
        String questionBody,
        String questionType,
        Object answer,
        Boolean correct,
        String feedbackDraft,
        Integer marks,
        Integer score,
        List<ResponseReviewItem> childItems) {
}
