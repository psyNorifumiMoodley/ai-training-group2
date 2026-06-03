package com.psybergate.dap.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "mcq_response")
@DiscriminatorValue("McqResponse")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqResponse extends Response {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> selectedAnswers;

    private Boolean correct;
}
