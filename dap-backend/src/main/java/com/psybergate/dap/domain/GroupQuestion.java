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
public class GroupQuestion extends AssessmentQuestion {

    @Column(nullable = false)
    private boolean ordered;

    @OneToMany(mappedBy = "groupQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "display_order")
    @ToString.Exclude
    private List<GroupQuestionChild> children = new ArrayList<>();
}
