package com.psybergate.dap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "doc_question")
@Getter
@Setter
@NoArgsConstructor
public class DocQuestion extends AssessmentQuestion {

    @Column(name = "marks", nullable = false)
    private int marks;
}
