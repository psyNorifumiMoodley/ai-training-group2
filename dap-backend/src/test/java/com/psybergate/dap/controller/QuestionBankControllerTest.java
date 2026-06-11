package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.dto.QuestionBankRequest;
import com.psybergate.dap.dto.QuestionBankResponse;
import com.psybergate.dap.service.AuthService;
import com.psybergate.dap.service.QuestionBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionBankController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class QuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private QuestionBankService questionBankService;

    private String markerToken;
    private String candidateToken;

    @BeforeEach
    void setUp() {
        when(authService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            Role role = email.startsWith("marker") ? Role.MARKER : Role.CANDIDATE;
            AppUser user = AppUser.builder().email(email).passwordHash("x").name("User").role(role).build();
            user.setId(UUID.randomUUID());
            return user;
        });

        AppUser marker = AppUser.builder()
                .email("marker@example.com").passwordHash("x").name("Marker").role(Role.MARKER).build();
        marker.setId(UUID.randomUUID());
        markerToken = jwtUtil.generateToken(marker);

        AppUser candidate = AppUser.builder()
                .email("candidate@example.com").passwordHash("x").name("Candidate").role(Role.CANDIDATE).build();
        candidate.setId(UUID.randomUUID());
        candidateToken = jwtUtil.generateToken(candidate);
    }

    @Test
    void createQuestionBank_asMarker_returns201WithRealUuidAndName() throws Exception {
        UUID id = UUID.randomUUID();
        when(questionBankService.create(any(QuestionBankRequest.class)))
                .thenReturn(new QuestionBankResponse(id, "Java Core", 0L));

        Map<String, Object> body = Map.of("name", "Java Core");

        mockMvc.perform(post("/api/question-banks")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Java Core"));
    }

    @Test
    void createQuestionBank_duplicateName_returns409() throws Exception {
        when(questionBankService.create(any(QuestionBankRequest.class)))
                .thenThrow(new ConflictException("Question bank with name 'Java Core' already exists"));

        Map<String, Object> body = Map.of("name", "Java Core");

        mockMvc.perform(post("/api/question-banks")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void listQuestionBanks_asMarker_returnsPaginatedResults() throws Exception {
        UUID id = UUID.randomUUID();
        PageResponse<QuestionBankResponse> page = new PageResponse<>(
                List.of(new QuestionBankResponse(id, "Java Core", 0L)), 1, 1, 20, 0);
        when(questionBankService.list(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/question-banks")
                        .header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Java Core"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void renameQuestionBank_asMarker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(questionBankService.rename(eq(id), any(QuestionBankRequest.class)))
                .thenReturn(new QuestionBankResponse(id, "Java Advanced", 0L));

        Map<String, Object> body = Map.of("name", "Java Advanced");

        mockMvc.perform(put("/api/question-banks/{id}", id)
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java Advanced"));
    }

    @Test
    void renameQuestionBank_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(questionBankService.rename(eq(id), any(QuestionBankRequest.class)))
                .thenThrow(new NoSuchElementException("Question bank not found: " + id));

        Map<String, Object> body = Map.of("name", "New Name");

        mockMvc.perform(put("/api/question-banks/{id}", id)
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteQuestionBank_asMarker_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(questionBankService).delete(id);

        mockMvc.perform(delete("/api/question-banks/{id}", id)
                        .header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteQuestionBank_asCandidate_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/question-banks/{id}", id)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }
}
