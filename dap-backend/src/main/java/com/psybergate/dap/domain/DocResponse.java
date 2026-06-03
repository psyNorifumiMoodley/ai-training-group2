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
@Table(name = "doc_response")
@DiscriminatorValue("DocResponse")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocResponse extends Response {

    @Column(name = "file_path", length = 1024)
    private String filePath;
}
