package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "group_question_child")
@Getter
@Setter
@NoArgsConstructor
public class GroupQuestionChild extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude
    private GroupQuestion groupQuestion;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywords;

    @Column(nullable = false)
    private int marks;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
