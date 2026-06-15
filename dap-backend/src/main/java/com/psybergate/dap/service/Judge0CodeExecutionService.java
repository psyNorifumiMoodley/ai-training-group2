package com.psybergate.dap.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.psybergate.dap.domain.CodingQuestion;
import com.psybergate.dap.domain.CodingQuestionLanguage;
import com.psybergate.dap.domain.TestCase;
import com.psybergate.dap.dto.TestCaseResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class Judge0CodeExecutionService implements CodeExecutionService {

    private final RestClient restClient;
    private final String apiKey;

    public Judge0CodeExecutionService(
            @Value("${code-execution.judge0.base-url}") String baseUrl,
            @Value("${code-execution.judge0.api-key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public List<TestCaseResultResponse> execute(CodingQuestion question, String code) {
        List<TestCase> testCases = question.getTestCases();
        if (testCases.isEmpty()) {
            return List.of();
        }

        int langId = languageId(question.getLanguage());
        List<SubmissionRequest> submissions = testCases.stream()
                .map(tc -> new SubmissionRequest(
                        code,
                        langId,
                        tc.getInput(),
                        tc.getExpectedOutput(),
                        tc.getTimeoutSeconds(),
                        tc.getMemoryMb() * 1024)) // MB → KB
                .toList();

        BatchRequest body = new BatchRequest(submissions);

        RestClient.RequestBodySpec spec = restClient.post()
                .uri("/submissions/batch?base64_encoded=false&wait=true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);

        if (apiKey != null && !apiKey.isBlank()) {
            spec = spec.header("X-RapidAPI-Key", apiKey);
        }

        BatchResponse response = spec.retrieve().body(BatchResponse.class);

        List<SubmissionResult> results = (response != null && response.submissions() != null)
                ? response.submissions() : List.of();

        List<TestCaseResultResponse> mapped = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            if (i >= results.size()) {
                mapped.add(new TestCaseResultResponse(
                        tc.getId(), false, null, null, null, "No result returned", tc.getOrdinal()));
                continue;
            }
            SubmissionResult r = results.get(i);
            boolean passed = r.status() != null && r.status().id() == 3;
            String errorMessage = firstNonBlank(r.stderr(), r.compileOutput());
            Long executionTimeMs = parseMillis(r.time());
            // Judge0 memory is in KB → convert to MB
            Long memoryUsedMb = r.memory() != null ? r.memory() / 1024 : null;
            mapped.add(new TestCaseResultResponse(
                    tc.getId(), passed, r.stdout(), executionTimeMs, memoryUsedMb,
                    errorMessage, tc.getOrdinal()));
        }
        return mapped;
    }

    private int languageId(CodingQuestionLanguage language) {
        return switch (language) {
            case JAVA -> 91;
            case PYTHON -> 71;
            case CSHARP -> 51;
        };
    }

    private Long parseMillis(String time) {
        if (time == null || time.isBlank()) return null;
        try {
            return Math.round(Double.parseDouble(time) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    // ── Inner types for Judge0 HTTP contract ──────────────────────────────────

    private record BatchRequest(List<SubmissionRequest> submissions) {}

    private record SubmissionRequest(
            @JsonProperty("source_code") String sourceCode,
            @JsonProperty("language_id") int languageId,
            String stdin,
            @JsonProperty("expected_output") String expectedOutput,
            @JsonProperty("cpu_time_limit") int cpuTimeLimit,
            @JsonProperty("memory_limit") int memoryLimit) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchResponse(List<SubmissionResult> submissions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SubmissionResult(
            String stdout,
            String time,
            Long memory,
            String stderr,
            @JsonProperty("compile_output") String compileOutput,
            StatusDto status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StatusDto(int id, String description) {}
}
