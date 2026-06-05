package com.psybergate.dap.service;

import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.dto.DashboardStatsResponse;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

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
        Instant weekStart = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        Instant todayStart = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        long totalCandidates         = candidateRepository.count();
        long candidatesAddedThisWeek = candidateRepository.countByUserCreatedAtAfter(weekStart);
        long totalAssessments        = assessmentRepository.count();
        long pendingCount            = assessmentRepository.countByStatus(AssessmentStatus.PENDING);
        long inProgressCount         = assessmentRepository.countByStatus(AssessmentStatus.IN_PROGRESS);
        long submittedCount          = assessmentRepository.countByStatus(AssessmentStatus.SUBMITTED);
        long markedCount             = assessmentRepository.countByStatus(AssessmentStatus.MARKED);
        long markedToday             = assessmentRepository.countByStatusAndUpdatedAtAfter(AssessmentStatus.MARKED, todayStart);

        return new DashboardStatsResponse(
                totalCandidates,
                candidatesAddedThisWeek,
                totalAssessments,
                pendingCount,
                inProgressCount,
                submittedCount,
                markedCount,
                markedToday
        );
    }
}
