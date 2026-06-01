package com.psybergate.dap.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "doc_question")
@Getter
@Setter
@Builder
@NoArgsConstructor
public class DocQuestion extends AssessmentQuestion {
}
