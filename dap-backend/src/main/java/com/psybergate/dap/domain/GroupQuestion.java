package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class GroupQuestion extends TextQuestion {

    @Column(nullable = false)
    private boolean ordered;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_question_follow_up",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    @OrderColumn(name = "display_order")
    @ToString.Exclude
    private List<TextQuestion> followUpQuestions = new ArrayList<>();
}
