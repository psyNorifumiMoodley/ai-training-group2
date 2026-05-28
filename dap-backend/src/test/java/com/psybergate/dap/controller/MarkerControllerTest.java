package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.MarkerRequest;
import com.psybergate.dap.dto.MarkerResponse;
import com.psybergate.dap.service.AuthService;
import com.psybergate.dap.service.MarkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarkerController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class MarkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @org.springframework.boot.test.mock.mockito.MockBean
    private AuthService authService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private MarkerService markerService;

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

    @Test
    void registerMarker_asAdmin_returns201() throws Exception {
        AppUser admin = AppUser.builder()
                .email("admin@example.com").passwordHash("x").name("Admin").role(Role.ADMIN).build();
        admin.setId(UUID.randomUUID());
        String token = jwtUtil.generateToken(admin);

        MarkerRequest request = new MarkerRequest("John Marker", "john@example.com", "password123");
        MarkerResponse response = new MarkerResponse(UUID.randomUUID(), "John Marker", "john@example.com");
        when(markerService.register(any(MarkerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/markers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Marker"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void registerMarker_missingName_returns400() throws Exception {
        AppUser admin = AppUser.builder()
                .email("admin@example.com").passwordHash("x").name("Admin").role(Role.ADMIN).build();
        admin.setId(UUID.randomUUID());
        String token = jwtUtil.generateToken(admin);

        String body = "{\"email\":\"john@example.com\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/markers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registerMarker_duplicateEmail_returns409() throws Exception {
        AppUser admin = AppUser.builder()
                .email("admin@example.com").passwordHash("x").name("Admin").role(Role.ADMIN).build();
        admin.setId(UUID.randomUUID());
        String token = jwtUtil.generateToken(admin);

        MarkerRequest request = new MarkerRequest("John Marker", "duplicate@example.com", "password123");
        when(markerService.register(any(MarkerRequest.class)))
                .thenThrow(new ConflictException("Email already in use: duplicate@example.com"));

        mockMvc.perform(post("/api/markers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registerMarker_asCandidate_returns403() throws Exception {
        AppUser candidate = AppUser.builder()
                .email("candidate@example.com").passwordHash("x").name("Candidate").role(Role.CANDIDATE).build();
        candidate.setId(UUID.randomUUID());
        String token = jwtUtil.generateToken(candidate);

        MarkerRequest request = new MarkerRequest("John Marker", "john@example.com", "password123");

        mockMvc.perform(post("/api/markers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerMarker_withoutToken_returns401() throws Exception {
        MarkerRequest request = new MarkerRequest("John Marker", "john@example.com", "password123");

        mockMvc.perform(post("/api/markers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
