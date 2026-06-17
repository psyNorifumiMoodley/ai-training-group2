package com.psybergate.dap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "coding_response")
@DiscriminatorValue("CodingResponse")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingResponse extends Response {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "executed_at")
    private Instant executedAt;
}
