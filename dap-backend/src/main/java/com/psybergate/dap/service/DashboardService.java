package com.psybergate.dap.service;

import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.dto.DashboardStatsResponse;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final CandidateRepository candidateRepository;
    private final AssessmentRepository assessmentRepository;

    public DashboardService(CandidateRepository candidateRepository,
                            AssessmentRepository assessmentRepository) {
        this.candidateRepository = candidateRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalCandidates  = candidateRepository.count();
        long totalAssessments = assessmentRepository.count();
        long pendingCount     = assessmentRepository.countByStatus(AssessmentStatus.PENDING);
        long inProgressCount  = assessmentRepository.countByStatus(AssessmentStatus.IN_PROGRESS);
        long submittedCount   = assessmentRepository.countByStatus(AssessmentStatus.SUBMITTED);
        long markedCount      = assessmentRepository.countByStatus(AssessmentStatus.MARKED);

        return new DashboardStatsResponse(
                totalCandidates,
                totalAssessments,
                pendingCount,
                inProgressCount,
                submittedCount,
                markedCount
        );
    }
}
