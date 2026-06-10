package com.psybergate.dap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "mcq_plus_response")
@DiscriminatorValue("McqPlusResponse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class McqPlusResponse extends McqResponse {

    @Column(name = "follow_up_answer", columnDefinition = "TEXT")
    private String followUpAnswer;

    @Column(name = "follow_up_score")
    private Integer followUpScore;
}
