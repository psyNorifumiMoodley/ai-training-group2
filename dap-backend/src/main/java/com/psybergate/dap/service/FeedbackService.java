package com.psybergate.dap.service;

import com.psybergate.dap.dto.FeedbackItem;
import com.psybergate.dap.dto.FeedbackUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FeedbackService {

    @Transactional
    public void updateFeedback(UUID assessmentId, UUID questionId, FeedbackUpdateRequest request) {
        // stub — no-op until Phase 5 implementation
    }

    @Transactional(readOnly = true)
    public List<FeedbackItem> getCandidateFeedback(UUID assessmentId, UUID requestingCandidateId) {
        return List.of();
    }
}
