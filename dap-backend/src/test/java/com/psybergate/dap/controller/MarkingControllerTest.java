package com.psybergate.dap.controller;

import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.service.AssessmentService;
import com.psybergate.dap.service.AuthService;
import com.psybergate.dap.service.FeedbackService;
import com.psybergate.dap.service.MarkingService;
import com.psybergate.dap.service.ResponseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssessmentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class MarkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private AssessmentService assessmentService;

    @MockBean
    private ResponseService responseService;

    @MockBean
    private MarkingService markingService;

    @MockBean
    private FeedbackService feedbackService;

    @BeforeEach
    void stubUserDetailsService() {
        when(authService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            Role role = email.startsWith("marker") ? Role.MARKER : Role.CANDIDATE;
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

    @Test
    void getSubmittedAssessments_asMarker_returns200() throws Exception {
        when(markingService.listSubmitted(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/assessments")
                        .header("Authorization", "Bearer " + tokenFor("marker@example.com", Role.MARKER))
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk());
    }

    @Test
    void getSubmittedAssessments_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/assessments")
                        .header("Authorization", "Bearer " + tokenFor("candidate@example.com", Role.CANDIDATE))
                        .param("status", "SUBMITTED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResponsesForReview_asMarker_returns200() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(markingService.getResponsesForReview(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/assessments/{id}/responses", assessmentId)
                        .header("Authorization", "Bearer " + tokenFor("marker@example.com", Role.MARKER)))
                .andExpect(status().isOk());
    }

    @Test
    void getResponsesForReview_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/assessments/{id}/responses", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenFor("candidate@example.com", Role.CANDIDATE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateResponseFeedback_asMarker_returns204() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        UUID responseId = UUID.randomUUID();

        mockMvc.perform(patch("/api/assessments/{id}/responses/{responseId}", assessmentId, responseId)
                        .header("Authorization", "Bearer " + tokenFor("marker@example.com", Role.MARKER))
                        .contentType("application/json")
                        .content("{\"feedbackText\":\"Good answer\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateResponseFeedback_asCandidate_returns403() throws Exception {
        mockMvc.perform(patch("/api/assessments/{id}/responses/{responseId}", UUID.randomUUID(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenFor("candidate@example.com", Role.CANDIDATE))
                        .contentType("application/json")
                        .content("{\"feedbackText\":\"Good answer\"}"))
                .andExpect(status().isForbidden());
    }
}
