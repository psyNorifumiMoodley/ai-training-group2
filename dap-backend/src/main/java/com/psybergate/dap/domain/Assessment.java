package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String invitationToken;

    @Column(nullable = false)
    private int timeLimitMinutes;

    private Instant startTime;

    private boolean autoSubmitted;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "assessment_question_link",
            joinColumns = @JoinColumn(name = "assessment_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    @ToString.Exclude
    @Builder.Default
    private List<AssessmentQuestion> questions = new ArrayList<>();
}
