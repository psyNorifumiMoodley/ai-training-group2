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
        UUID bankId = UUID.randomUUID();
        McqQuestionResponse response = new McqQuestionResponse(id, List.of(), "Which is a keyword?", List.of("int", "for"), List.of("int"), false);
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "MCQ",
                "questionBankIds", List.of(bankId.toString()),
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
    void createDocQuestion_asMarker_returns410() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "DOC",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Upload your design document",
                "marks", 5
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isGone());
    }

    @Test
    void createMcqQuestion_withZeroCorrectAnswers_returns400() throws Exception {
        when(questionService.create(any())).thenThrow(new ValidationException("MCQ must have at least one correct answer"));

        Map<String, Object> body = Map.of(
                "type", "MCQ",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
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
                "questionBankIds", List.of(UUID.randomUUID().toString()),
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
        TextQuestionResponse response = new TextQuestionResponse(id, List.of(), "Describe OOP", List.of("encapsulation"), 5);
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Describe OOP",
                "keywords", List.of("encapsulation"),
                "marks", 5
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.marks").value(5));
    }

    @Test
    void createGroupQuestion_withChildren_returns201() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        GroupChildResponse child = new GroupChildResponse(childId, "What is encapsulation?", List.of(), 2);
        GroupQuestionResponse response = new GroupQuestionResponse(groupId, List.of(), "OOP concepts", true, List.of(child), 2);
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "GROUP",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "OOP concepts",
                "ordered", true,
                "children", List.of(Map.of("questionText", "What is encapsulation?", "marks", 2))
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.children[0].questionText").value("What is encapsulation?"));
    }

    @Test
    void createGroupQuestion_withEmptyChildren_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "GROUP",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "OOP concepts",
                "ordered", false,
                "children", List.of()
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createQuestion_missingQuestionBankIds_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "question", "Describe OOP",
                "keywords", List.of(),
                "marks", 5
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
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Describe OOP",
                "keywords", List.of(),
                "marks", 5
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
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Describe OOP",
                "keywords", List.of(),
                "marks", 5
        );

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuestion_asMarker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        TextQuestionResponse response = new TextQuestionResponse(id, List.of(), "Describe OOP", List.of(), 0);
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

    // --- Task 7.3: MCQ_PLUS stub → 201 ---

    @Test
    void createMcqPlusQuestion_asMarker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        McqPlusQuestionResponse response = new McqPlusQuestionResponse(
                id, List.of(), "Which is a keyword?",
                List.of("int", "for"), List.of("int"), false,
                "Explain why.", List.of(), 2, 3
        );
        when(questionService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "type", "MCQ_PLUS",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Which is a keyword?",
                "options", List.of("int", "for"),
                "correctAnswers", List.of("int"),
                "followUpQuestion", "Explain why.",
                "followUpMarks", 2
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MCQ_PLUS"))
                .andExpect(jsonPath("$.totalMarks").value(3));
    }

    // --- Task 7.4: TEXT without marks → 400 ---

    @Test
    void createTextQuestion_withoutMarks_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Describe OOP",
                "keywords", List.of()
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // --- Task 7.5: GROUP without children → 400 ---

    @Test
    void createGroupQuestion_withoutChildren_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "GROUP",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "OOP concepts",
                "ordered", true
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // --- Task 7.6: GET /api/questions/categories → 404 ---

    @Test
    void getCategories_returns404() throws Exception {
        mockMvc.perform(get("/api/questions/categories")
                        .header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isNotFound());
    }

    // --- Task 7.4: questionBankId filter ---

    @Test
    void listQuestions_withQuestionBankIdFilter_returns200() throws Exception {
        UUID bankId = UUID.randomUUID();
        UUID qId = UUID.randomUUID();
        QuestionBankResponse bank = new QuestionBankResponse(bankId, "Java Core");
        McqQuestionResponse q = new McqQuestionResponse(qId, List.of(bank), "What is Java?", List.of("A", "B"), List.of("A"), false);
        PageResponse<QuestionResponse> page = new PageResponse<>(List.of(q), 1, 1, 20, 0);
        when(questionService.list(0, 20, bankId)).thenReturn(page);

        mockMvc.perform(get("/api/questions")
                        .param("questionBankId", bankId.toString())
                        .header("Authorization", "Bearer " + markerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].questionBanks[0].id").value(bankId.toString()));
    }

    // --- Task 7.5: MCQ_PLUS validation ---

    @Test
    void createMcqPlusQuestion_followUpMarksZero_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "MCQ_PLUS",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Which is a keyword?",
                "options", List.of("int", "for"),
                "correctAnswers", List.of("int"),
                "followUpQuestion", "Explain why.",
                "followUpMarks", 0
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMcqPlusQuestion_missingFollowUpQuestion_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "MCQ_PLUS",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Which is a keyword?",
                "options", List.of("int", "for"),
                "correctAnswers", List.of("int"),
                "followUpMarks", 2
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // --- Task 7.7: marks validation for TEXT and DOC ---

    @Test
    void createTextQuestion_marksLessThanOne_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "TEXT",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Describe OOP",
                "marks", 0
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocQuestion_marksLessThanOne_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "type", "DOC",
                "questionBankIds", List.of(UUID.randomUUID().toString()),
                "question", "Upload your design",
                "marks", 0
        );

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + markerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
