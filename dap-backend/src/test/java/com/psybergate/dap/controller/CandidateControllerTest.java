package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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

    @org.springframework.boot.test.mock.mockito.MockBean
    private AuthService authService;

    @BeforeEach
    void stubUserDetailsService() {
        when(authService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            Role role = email.startsWith("admin") ? Role.ADMIN
                    : email.startsWith("marker") ? Role.MARKER : Role.CANDIDATE;
            AppUser user = AppUser.builder().email(email).passwordHash("x").name("User").role(role).build();
            user.setId(java.util.UUID.randomUUID());
            return user;
        });
    }

    @Test
    void registerCandidate_asAdmin_returns201() throws Exception {
        AppUser admin = AppUser.builder()
                .email("admin@example.com").passwordHash("x").name("Admin").role(Role.ADMIN).build();
        admin.setId(java.util.UUID.randomUUID());
        String token = jwtUtil.generateToken(admin);

        CandidateRequest request = new CandidateRequest("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void registerCandidate_asMarker_returns403() throws Exception {
        AppUser marker = AppUser.builder()
                .email("marker@example.com").passwordHash("x").name("Marker").role(Role.MARKER).build();
        marker.setId(java.util.UUID.randomUUID());
        String token = jwtUtil.generateToken(marker);

        CandidateRequest request = new CandidateRequest("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(post("/api/candidates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerCandidate_withoutToken_returns401() throws Exception {
        CandidateRequest request = new CandidateRequest("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
