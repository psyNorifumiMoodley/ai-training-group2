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

@Entity
@Table(name = "text_response")
@DiscriminatorValue("TextResponse")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextResponse extends Response {

    @Column(columnDefinition = "TEXT")
    private String answer;
}
