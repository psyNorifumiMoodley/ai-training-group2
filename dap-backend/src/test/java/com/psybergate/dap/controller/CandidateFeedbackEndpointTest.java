package com.psybergate.dap.controller;

import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.FeedbackItem;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssessmentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class CandidateFeedbackEndpointTest {

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

    private UUID assessmentId;

    @BeforeEach
    void setUp() {
        assessmentId = UUID.randomUUID();
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

    private AppUser candidateUserWith(UUID id) {
        AppUser user = AppUser.builder()
                .email("candidate@test.com").passwordHash("x").name("Jane").role(Role.CANDIDATE).build();
        user.setId(id);
        return user;
    }

    @Test
    void getCandidateFeedback_markedAssessmentCorrectCandidate_returns200WithItems() throws Exception {
        UUID candidateUserId = UUID.randomUUID();
        AppUser candidateUser = candidateUserWith(candidateUserId);
        String token = jwtUtil.generateToken(candidateUser);

        when(authService.loadUserByUsername("candidate@test.com")).thenReturn(candidateUser);

        List<FeedbackItem> items = List.of(
                new FeedbackItem(UUID.randomUUID(), "Explain OOP", "Good explanation."),
                new FeedbackItem(UUID.randomUUID(), "What is SOLID?", "Needs more detail.")
        );
        when(feedbackService.getCandidateFeedback(eq(assessmentId), eq(candidateUserId)))
                .thenReturn(items);

        mockMvc.perform(get("/api/assessments/{id}/feedback", assessmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].questionBody").value("Explain OOP"))
                .andExpect(jsonPath("$[0].feedbackText").value("Good explanation."));
    }

    @Test
    void getCandidateFeedback_wrongCandidate_returns403() throws Exception {
        String token = tokenFor("candidate@test.com", Role.CANDIDATE);

        when(feedbackService.getCandidateFeedback(eq(assessmentId), any(UUID.class)))
                .thenThrow(new AccessDeniedException("You are not the assigned candidate for this assessment"));

        mockMvc.perform(get("/api/assessments/{id}/feedback", assessmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCandidateFeedback_assessmentNotMarked_returns403() throws Exception {
        String token = tokenFor("candidate@test.com", Role.CANDIDATE);

        when(feedbackService.getCandidateFeedback(eq(assessmentId), any(UUID.class)))
                .thenThrow(new AccessDeniedException("Feedback is not yet available"));

        mockMvc.perform(get("/api/assessments/{id}/feedback", assessmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCandidateFeedback_markerJwt_returns403() throws Exception {
        String token = tokenFor("marker@test.com", Role.MARKER);

        mockMvc.perform(get("/api/assessments/{id}/feedback", assessmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCandidateFeedback_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/assessments/{id}/feedback", assessmentId))
                .andExpect(status().isUnauthorized());
    }
}
