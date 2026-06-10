package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AssessmentQuestion extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "question_question_bank",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "question_bank_id")
    )
    @ToString.Exclude
    private Set<QuestionBank> questionBanks = new HashSet<>();
}
