package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_feedback_assessment_question",
                columnNames = {"assessment_id", "question_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(columnDefinition = "TEXT")
    private String draft;

    @Column(nullable = false)
    private boolean finalised;
}
