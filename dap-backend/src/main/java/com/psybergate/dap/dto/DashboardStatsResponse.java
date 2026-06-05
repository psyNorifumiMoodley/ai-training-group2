package com.psybergate.dap.dto;

public record DashboardStatsResponse(
        long totalCandidates,
        long totalAssessments,
        long pendingCount,
        long inProgressCount,
        long submittedCount,
        long markedCount
) {}
