package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "question_bank")
@Getter
@Setter
@NoArgsConstructor
public class QuestionBank extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "questionBanks", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<AssessmentQuestion> questions = new HashSet<>();

    public QuestionBank(String name) {
        this.name = name;
    }
}
