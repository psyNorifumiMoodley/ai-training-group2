package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssessmentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class AssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private AssessmentService assessmentService;

    @BeforeEach
    void stubUserDetailsService() {
        when(authService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            Role role = email.startsWith("admin") ? Role.ADMIN
                    : email.startsWith("marker") ? Role.MARKER : Role.CANDIDATE;
            AppUser user = AppUser.builder().email(email).passwordHash("x").name("User").role(role).build();
            user.setId(UUID.randomUUID());
            return user;
        });
    }

    private String tokenFor(String email, Role role) {
        AppUser user = AppUser.builder().email(email).passwordHash("x").name("User").role(role).build();
        user.setId(UUID.randomUUID());
        return jwtUtil.generateToken(user);
    }

    private AssessmentRequest validRequest() {
        return new AssessmentRequest(UUID.randomUUID(), List.of(UUID.randomUUID()), 60);
    }

    @Test
    void generateAssessment_asMarker_returns201WithBody() throws Exception {
        String token = tokenFor("marker@example.com", Role.MARKER);
        UUID candidateId = UUID.randomUUID();
        AssessmentResponse stubResponse = new AssessmentResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                candidateId, "PENDING",
                "http://localhost:4200/assessment/stub-token",
                60, Instant.now().toString()
        );
        when(assessmentService.generate(any(AssessmentRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invitationLink").isNotEmpty())
                .andExpect(jsonPath("$.timeLimitMinutes").value(60));
    }

    @Test
    void generateAssessment_asCandidate_returns403() throws Exception {
        String token = tokenFor("candidate@example.com", Role.CANDIDATE);

        mockMvc.perform(post("/api/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void generateAssessment_asAdmin_returns403() throws Exception {
        String token = tokenFor("admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void generateAssessment_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateAssessment_missingCandidateId_returns400() throws Exception {
        String token = tokenFor("marker@example.com", Role.MARKER);
        String body = "{\"questionIds\":[\"" + UUID.randomUUID() + "\"],\"timeLimitMinutes\":60}";

        mockMvc.perform(post("/api/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
