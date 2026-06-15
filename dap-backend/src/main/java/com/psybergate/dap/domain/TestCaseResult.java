package com.psybergate.dap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "test_case_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coding_response_id", nullable = false)
    @ToString.Exclude
    private CodingResponse codingResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @ToString.Exclude
    private TestCase testCase;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_mb")
    private Long memoryUsedMb;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int ordinal;
}
