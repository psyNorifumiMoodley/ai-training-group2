package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.CandidateResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.AuthService;
import com.psybergate.dap.service.CandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CandidateController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class CandidateControllerTest {

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
    private AssessmentService assessmentService;

    private String adminToken;

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

        AppUser admin = AppUser.builder()
                .email("admin@example.com").passwordHash("x").name("Admin").role(Role.ADMIN).build();
        admin.setId(UUID.randomUUID());
        adminToken = jwtUtil.generateToken(admin);
    }

    @Test
    void register_asAdmin_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidateService.register(any(CandidateRequest.class)))
                .thenReturn(new CandidateResponse(id, "Jane Doe", "jane@example.com", null));

        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CandidateRequest("Jane Doe", "jane@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(candidateService.register(any(CandidateRequest.class)))
                .thenThrow(new ConflictException("Email already registered: jane@example.com"));

        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CandidateRequest("Jane Doe", "jane@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jane Doe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void register_asMarker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidateService.register(any(CandidateRequest.class)))
                .thenReturn(new CandidateResponse(id, "Jane Doe", "jane@example.com", "2026-06-05T00:00:00Z"));

        AppUser marker = AppUser.builder()
                .email("marker@example.com").passwordHash("x").name("Marker").role(Role.MARKER).build();
        marker.setId(UUID.randomUUID());

        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(marker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CandidateRequest("Jane Doe", "jane@example.com"))))
                .andExpect(status().isCreated());
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CandidateRequest("Jane Doe", "jane@example.com"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_asAdmin_returns200() throws Exception {
        when(candidateService.listCandidates(anyInt(), anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 20, 0));

        mockMvc.perform(get("/api/candidates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
