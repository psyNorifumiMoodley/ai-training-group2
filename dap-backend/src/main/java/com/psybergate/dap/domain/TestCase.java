package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_case")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coding_question_id", nullable = false)
    @ToString.Exclude
    private CodingQuestion codingQuestion;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private int timeoutSeconds;

    @Column(nullable = false)
    private int memoryMb;

    @Column(nullable = false)
    private int ordinal;
}
