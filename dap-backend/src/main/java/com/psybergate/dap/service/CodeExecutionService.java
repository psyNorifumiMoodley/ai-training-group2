package com.psybergate.dap.service;

import com.psybergate.dap.domain.CodingQuestion;
import com.psybergate.dap.dto.TestCaseResultResponse;

import java.util.List;

public interface CodeExecutionService {

    List<TestCaseResultResponse> execute(CodingQuestion question, String code);
}
