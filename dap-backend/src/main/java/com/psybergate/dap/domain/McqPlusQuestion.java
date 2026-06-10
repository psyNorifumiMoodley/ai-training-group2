package com.psybergate.dap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "mcq_plus_question")
@Getter
@Setter
@NoArgsConstructor
public class McqPlusQuestion extends McqQuestion {

    @Column(name = "follow_up_question", columnDefinition = "TEXT", nullable = false)
    private String followUpQuestion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "follow_up_keywords", columnDefinition = "jsonb")
    private List<String> followUpKeywords;

    @Column(name = "follow_up_marks", nullable = false)
    private int followUpMarks;
}
