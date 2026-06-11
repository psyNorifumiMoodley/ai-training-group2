package com.psybergate.dap.service;

import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.QuestionBank;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CodingQuestionRequest;
import com.psybergate.dap.dto.CodingQuestionResponse;
import com.psybergate.dap.repository.AppUserRepository;
import com.psybergate.dap.repository.QuestionBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CodingQuestionServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TransactionTemplate txTemplate;

    private String markerToken;
    private UUID bankId;

    @BeforeEach
    void setUp() {
        txTemplate.execute(status -> {
            AppUser marker = AppUser.builder()
                    .email("marker-coding-" + UUID.randomUUID() + "@test.com")
                    .name("Marker")
                    .passwordHash("x")
                    .role(Role.MARKER)
                    .build();
            appUserRepository.save(marker);
            markerToken = jwtUtil.generateToken(marker);

            QuestionBank bank = new QuestionBank("Test Bank " + UUID.randomUUID());
            questionBankRepository.save(bank);
            bankId = bank.getId();
            return null;
        });
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(markerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // --- Test 5.1: Valid create returns 201 with persisted language and question banks ---

    @Test
    void createCodingQuestion_validRequest_returns201WithLanguageAndBanks() {
        CodingQuestionRequest request = new CodingQuestionRequest(
                List.of(bankId),
                "Write a method to reverse a string.",
                com.psybergate.dap.domain.CodingQuestionLanguage.JAVA,
                null
        );

        ResponseEntity<CodingQuestionResponse> response = restTemplate.exchange(
                "/api/questions",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                CodingQuestionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().language()).isEqualTo(com.psybergate.dap.domain.CodingQuestionLanguage.JAVA);
        assertThat(response.getBody().questionBanks()).isNotEmpty();
        assertThat(response.getBody().testCases()).isEmpty();
        assertThat(response.getBody().id()).isNotNull();
    }

    // --- Test 5.2: Missing language returns 400 ---

    @Test
    void createCodingQuestion_missingLanguage_returns400() {
        Map<String, Object> body = Map.of(
                "type", "CODING",
                "questionBankIds", List.of(bankId.toString()),
                "question", "Write a sorting algorithm."
        );

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/questions",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- Test 5.3: Empty questionBankIds returns 400 ---

    @Test
    void createCodingQuestion_emptyQuestionBankIds_returns400() {
        CodingQuestionRequest request = new CodingQuestionRequest(
                List.of(),
                "Write a sorting algorithm.",
                com.psybergate.dap.domain.CodingQuestionLanguage.PYTHON,
                null
        );

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/questions",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- Test 5.4: Unknown questionBankIds entry returns 400 ---

    @Test
    void createCodingQuestion_unknownQuestionBankId_returns400() {
        CodingQuestionRequest request = new CodingQuestionRequest(
                List.of(UUID.randomUUID()),
                "Write a BFS traversal.",
                com.psybergate.dap.domain.CodingQuestionLanguage.JAVA,
                null
        );

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/questions",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- Test 5.5: GET /api/questions/{id} returns language, questionBanks, testCases ---

    @Test
    void getCodingQuestion_returnsLanguageAndEmptyTestCases() {
        CodingQuestionRequest request = new CodingQuestionRequest(
                List.of(bankId),
                "Write a Fibonacci function.",
                com.psybergate.dap.domain.CodingQuestionLanguage.CSHARP,
                null
        );

        ResponseEntity<CodingQuestionResponse> created = restTemplate.exchange(
                "/api/questions",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                CodingQuestionResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = created.getBody().id();

        ResponseEntity<CodingQuestionResponse> fetched = restTemplate.exchange(
                "/api/questions/" + id,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                CodingQuestionResponse.class
        );

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().language()).isEqualTo(com.psybergate.dap.domain.CodingQuestionLanguage.CSHARP);
        assertThat(fetched.getBody().questionBanks()).isNotEmpty();
        assertThat(fetched.getBody().testCases()).isEmpty();
    }
}
