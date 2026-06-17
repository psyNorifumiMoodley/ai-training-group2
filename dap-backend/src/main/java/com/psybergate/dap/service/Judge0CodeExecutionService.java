package com.psybergate.dap.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.psybergate.dap.domain.CodingQuestion;
import com.psybergate.dap.domain.CodingQuestionLanguage;
import com.psybergate.dap.domain.TestCase;
import com.psybergate.dap.dto.TestCaseResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "code-execution.provider", havingValue = "judge0")
public class Judge0CodeExecutionService implements CodeExecutionService {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiHost;

    public Judge0CodeExecutionService(
            @Value("${code-execution.judge0.base-url}") String baseUrl,
            @Value("${code-execution.judge0.api-key:}") String apiKey,
            @Value("${code-execution.judge0.api-host:}") String apiHost) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.apiHost = apiHost;
    }

    @Override
    public List<TestCaseResultResponse> execute(CodingQuestion question, String code) {
        int langId = languageId(question.getLanguage());
        List<TestCase> testCases = question.getTestCases();

        if (testCases.isEmpty()) {
            return executeSandbox(code, langId);
        }

        List<SubmissionRequest> submissions = testCases.stream()
                .map(tc -> new SubmissionRequest(
                        code,
                        langId,
                        tc.getInput(),
                        tc.getExpectedOutput(),
                        tc.getTimeoutSeconds(),
                        tc.getMemoryMb() * 1024)) // MB → KB
                .toList();

        List<SubmissionResult> results = submitBatch(submissions);

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
            Long memoryUsedMb = r.memory() != null ? r.memory() / 1024 : null; // Judge0 memory is KB → MB
            mapped.add(new TestCaseResultResponse(
                    tc.getId(), passed, r.stdout(), executionTimeMs, memoryUsedMb,
                    errorMessage, tc.getOrdinal()));
        }
        return mapped;
    }

    /** Run without test cases — executes code with no stdin and no expected-output comparison. */
    private List<TestCaseResultResponse> executeSandbox(String code, int langId) {
        SubmissionRequest submission = new SubmissionRequest(code, langId, null, null, 10, 256 * 1024);
        List<SubmissionResult> results = submitBatch(List.of(submission));
        if (results.isEmpty()) {
            return List.of(new TestCaseResultResponse(null, false, null, null, null, "No result returned from executor", 1));
        }
        SubmissionResult r = results.get(0);
        boolean passed = r.status() != null && r.status().id() == 3;
        Long executionTimeMs = parseMillis(r.time());
        Long memoryUsedMb = r.memory() != null ? r.memory() / 1024 : null;
        return List.of(new TestCaseResultResponse(
                null, passed, r.stdout(), executionTimeMs, memoryUsedMb,
                firstNonBlank(r.stderr(), r.compileOutput()), 1));
    }

    private List<SubmissionResult> submitBatch(List<SubmissionRequest> submissions) {
        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri("/submissions/batch?base64_encoded=false&wait=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new BatchRequest(submissions));
            if (apiKey != null && !apiKey.isBlank()) {
                spec = spec.header("X-RapidAPI-Key", apiKey);
            }
            if (apiHost != null && !apiHost.isBlank()) {
                spec = spec.header("X-RapidAPI-Host", apiHost);
            }
            BatchResponse response = spec.retrieve().body(BatchResponse.class);
            return (response != null && response.submissions() != null) ? response.submissions() : List.of();
        } catch (RestClientException ex) {
            log.warn("Judge0 request failed: {}", ex.getMessage());
            return List.of();
        }
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
