package com.psybergate.dap.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.domain.CodingQuestion;
import com.psybergate.dap.domain.CodingQuestionLanguage;
import com.psybergate.dap.domain.TestCase;
import com.psybergate.dap.dto.TestCaseResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "code-execution.provider", havingValue = "piston", matchIfMissing = true)
public class PistonCodeExecutionService implements CodeExecutionService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public PistonCodeExecutionService(
            @Value("${code-execution.piston.base-url}") String baseUrl,
            ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<TestCaseResultResponse> execute(CodingQuestion question, String code) {
        String language = pistonLanguage(question.getLanguage());
        String filename = pistonFilename(question.getLanguage());
        List<TestCase> testCases = question.getTestCases();
        if (testCases.isEmpty()) {
            return List.of(executeSandbox(code, language, filename));
        }

        List<TestCaseResultResponse> results = new ArrayList<>();
        for (TestCase tc : testCases) {
            results.add(executeTestCase(code, language, filename, tc));
        }
        return results;
    }

    private TestCaseResultResponse executeSandbox(String code, String language, String filename) {
        PistonResponse response = submit(code, language, filename, null);
        if (response == null) {
            return new TestCaseResultResponse(null, false, null, null, null, "No result returned from executor", 1);
        }
        String error = extractError(response);
        String stdout = response.run() != null ? response.run().stdout() : null;
        boolean passed = error == null && response.run() != null && response.run().code() == 0;
        return new TestCaseResultResponse(null, passed, stdout, null, null, error, 1);
    }

    private TestCaseResultResponse executeTestCase(String code, String language, String filename, TestCase tc) {
        PistonResponse response = submit(code, language, filename, tc.getInput());
        if (response == null) {
            return new TestCaseResultResponse(tc.getId(), false, null, null, null, "No result returned from executor", tc.getOrdinal());
        }
        String error = extractError(response);
        String stdout = response.run() != null ? response.run().stdout() : null;
        boolean passed = error == null && stdout != null && normalise(stdout).equals(normalise(tc.getExpectedOutput()));
        return new TestCaseResultResponse(tc.getId(), passed, stdout, null, null, error, tc.getOrdinal());
    }

    private PistonResponse submit(String code, String language, String filename, String stdin) {
        try {
            Map<String, Object> body = Map.of(
                    "language", language,
                    "version", "*",
                    "files", List.of(Map.of("name", filename, "content", code)),
                    "stdin", stdin == null ? "" : stdin
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v2/execute"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                log.warn("Piston returned {}: {}", httpResponse.statusCode(), httpResponse.body());
                return null;
            }
            return objectMapper.readValue(httpResponse.body(), PistonResponse.class);
        } catch (Exception ex) {
            log.warn("Piston request failed [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private String extractError(PistonResponse response) {
        if (response.compile() != null && response.compile().code() != 0) {
            String err = response.compile().stderr();
            return (err != null && !err.isBlank()) ? err : response.compile().output();
        }
        if (response.run() != null && response.run().code() != 0) {
            String err = response.run().stderr();
            return (err != null && !err.isBlank()) ? err : response.run().output();
        }
        return null;
    }

    private String normalise(String s) {
        return s == null ? "" : s.stripTrailing();
    }

    private String pistonLanguage(CodingQuestionLanguage language) {
        return switch (language) {
            case JAVA -> "java";
            case PYTHON -> "python";
            case CSHARP -> "mono";
        };
    }

    private String pistonFilename(CodingQuestionLanguage language) {
        return switch (language) {
            case JAVA -> "Solution.java";
            case PYTHON -> "solution.py";
            case CSHARP -> "Solution.cs";
        };
    }

    // ── Piston HTTP contract ──────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PistonResponse(PistonRunResult compile, PistonRunResult run) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PistonRunResult(String stdout, String stderr, String output, int code) {}
}
