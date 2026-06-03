package com.psybergate.dap.service;

import com.psybergate.dap.dto.AssessmentSummaryResponse;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import com.psybergate.dap.dto.ResponseReviewItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MarkingService {

    @Transactional(readOnly = true)
    public Page<AssessmentSummaryResponse> listSubmitted(int page, int size) {
        return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
    }

    @Transactional(readOnly = true)
    public List<ResponseReviewItem> getResponsesForReview(UUID assessmentId) {
        return List.of();
    }

    @Transactional
    public void updateResponseFeedback(UUID assessmentId, UUID responseId, FeedbackUpdateRequest request) {
        // stub — no-op until Phase 5 implementation
    }
}
