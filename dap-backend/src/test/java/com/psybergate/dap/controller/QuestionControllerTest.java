package com.psybergate.dap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psybergate.dap.config.JwtAuthFilter;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.config.PasswordEncoderConfig;
import com.psybergate.dap.config.SecurityConfig;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.*;
import com.psybergate.dap.service.AuthService;
import com.psybergate.dap.service.QuestionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!",
        "jwt.expiration-hours=24"
})
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private QuestionService questionService;

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
    void createMcqQuestion_asMarker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        McqQuestionResponse response = new McqQuestionResponse(id, "Java", "Which is a keyword?", List.of("int", "for"), List.of("int"));
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "MCQ",
                "category", "Java",
                "question", "Which is a keyword?",
                "options", List.of("int", "for"),
                "correctAnswers", List.of("int")
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("MCQ"))
                .andExpect(jsonPath("$.options[0]").value("int"));
    }

    @Test
    void createDocQuestion_asMarker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        DocQuestionResponse response = new DocQuestionResponse(id, "Java", "Upload your design document");
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "DOC",
                "category", "Java",
                "question", "Upload your design document"
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("DOC"));
    }

    @Test
    void createMcqQuestion_withZeroCorrectAnswers_returns400() throws Exception {
        when(questionService.create(any())).thenThrow(new ValidationException("MCQ must have at least one correct answer"));

        Map<String, Object> body = Map.of(
                "type", "MCQ",
                "category", "Java",
                "question", "Which is a keyword?",
                "options", List.of("int", "for"),
                "correctAnswers", List.of()
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMcqQuestion_asCandidate_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "MCQ",
                "category", "Java",
                "question", "Which is a keyword?",
                "options", List.of("int", "for"),
                "correctAnswers", List.of("int")
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTextQuestion_asMarker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        TextQuestionResponse response = new TextQuestionResponse(id, "Java", "Describe OOP", List.of("encapsulation"));
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "category", "Java",
                "question", "Describe OOP",
                "keywords", List.of("encapsulation")
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.category").value("Java"));
    }

    @Test
    void createGroupQuestion_withValidFollowUpIds_returns201() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID followUpId = UUID.randomUUID();
        TextQuestionResponse followUp = new TextQuestionResponse(followUpId, "Java", "What is encapsulation?", List.of());
        GroupQuestionResponse response = new GroupQuestionResponse(groupId, "Java", "OOP concepts", true, List.of(followUp));
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "GROUP",
                "category", "Java",
                "question", "OOP concepts",
                "ordered", true,
                "followUpQuestionIds", List.of(followUpId.toString())
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.followUpQuestions[0].id").value(followUpId.toString()));
    }

    @Test
    void createGroupQuestion_withInvalidFollowUpId_returns400() throws Exception {
        when(questionService.create(any())).thenThrow(new ValidationException("One or more follow-up question IDs are invalid or not text questions"));

        Map<String, Object> body = Map.of(
                "type", "GROUP",
                "category", "Java",
                "question", "OOP concepts",
                "ordered", false,
                "followUpQuestionIds", List.of(UUID.randomUUID().toString())
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createQuestion_missingCategory_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "question", "Describe OOP",
                "keywords", List.of()
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQuestion_asCandidate_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "category", "Java",
                "question", "Describe OOP",
                "keywords", List.of()
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createQuestion_withoutToken_returns401() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "category", "Java",
                "question", "Describe OOP",
                "keywords", List.of()
        );

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuestion_asMarker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        TextQuestionResponse response = new TextQuestionResponse(id, "Java", "Describe OOP", List.of());
        when(questionService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/questions/{id}", id)
                        .header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deleteQuestion_asMarker_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/questions/{id}", id)
                        .header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isNoContent());
    }
}
