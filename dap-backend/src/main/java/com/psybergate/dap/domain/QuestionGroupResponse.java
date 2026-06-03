package com.psybergate.dap.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_group_response")
@DiscriminatorValue("QuestionGroupResponse")
@Getter
@Setter
@NoArgsConstructor
public class QuestionGroupResponse extends Response {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinTable(
            name = "question_group_response_children",
            joinColumns = @JoinColumn(name = "group_response_id"),
            inverseJoinColumns = @JoinColumn(name = "child_response_id")
    )
    @ToString.Exclude
    private List<Response> childResponses = new ArrayList<>();
}
