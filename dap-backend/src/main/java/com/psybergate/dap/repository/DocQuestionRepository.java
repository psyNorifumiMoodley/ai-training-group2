package com.psybergate.dap.repository;

import com.psybergate.dap.domain.DocQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocQuestionRepository extends JpaRepository<DocQuestion, UUID> {
}
