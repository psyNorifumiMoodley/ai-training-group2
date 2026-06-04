package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.MarkerRequest;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.AuthService;
import com.psybergate.dap.service.CandidateService;
import com.psybergate.dap.service.MarkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CandidateController.class, MarkerController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class RbacPhase1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private CandidateService candidateService;

    @MockBean
    private MarkerService markerService;

    @MockBean
    private AssessmentService assessmentService;

    private String markerToken;
    private String candidateToken;

    @BeforeEach
    void setUp() {
        when(authService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            Role role = email.startsWith("admin") ? Role.ADMIN
                    : email.startsWith("marker") ? Role.MARKER : Role.CANDIDATE;
            AppUser user = AppUser.builder().email(email).passwordHash("x").name("User").role(role).build();
            user.setId(UUID.randomUUID());
            return user;
        });

        AppUser marker = AppUser.builder().email("marker@test.com").passwordHash("x").name("Marker").role(Role.MARKER).build();
        marker.setId(UUID.randomUUID());
        markerToken = jwtUtil.generateToken(marker);

        AppUser candidate = AppUser.builder().email("candidate@test.com").passwordHash("x").name("Candidate").role(Role.CANDIDATE).build();
        candidate.setId(UUID.randomUUID());
        candidateToken = jwtUtil.generateToken(candidate);
    }

    // --- GET /api/candidates ---

    @Test
    void getCandidates_asMarker_returns200() throws Exception {
        mockMvc.perform(get("/api/candidates").header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isOk());
    }

    @Test
    void getCandidates_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/candidates").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCandidates_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/candidates ---

    @Test
    void registerCandidate_asCandidate_returns403() throws Exception {
        CandidateRequest req = new CandidateRequest("Jane Doe", "jane@test.com");
        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/markers ---

    @Test
    void getMarkers_asMarker_returns403() throws Exception {
        mockMvc.perform(get("/api/markers").header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMarkers_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/markers").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMarkers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/markers"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/markers ---

    @Test
    void registerMarker_asMarker_returns403() throws Exception {
        MarkerRequest req = new MarkerRequest("John Marker", "john@test.com", "password123");
        mockMvc.perform(post("/api/markers")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
