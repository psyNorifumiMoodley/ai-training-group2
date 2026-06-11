package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coding_question")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingQuestion extends AssessmentQuestion {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodingQuestionLanguage language;

    @OneToMany(mappedBy = "codingQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
}
