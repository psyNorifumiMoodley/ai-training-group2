package com.psybergate.dap.repository;

import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.QuestionBank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class QuestionBankRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private McqQuestionRepository mcqQuestionRepository;

    @Test
    void findByName_returnsPresent_whenBankExists() {
        questionBankRepository.save(new QuestionBank("Spring Core"));

        Optional<QuestionBank> result = questionBankRepository.findByName("Spring Core");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Spring Core");
    }

    @Test
    void findByName_returnsEmpty_whenBankDoesNotExist() {
        Optional<QuestionBank> result = questionBankRepository.findByName("Nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByQuestionsId_returnsTrue_whenQuestionIsAssociated() {
        QuestionBank bank = questionBankRepository.save(new QuestionBank("Java OOP"));

        McqQuestion q = new McqQuestion();
        q.setQuestion("What is polymorphism?");
        q.setOptions(List.of("A", "B"));
        q.setCorrectAnswers(List.of("A"));
        q.getQuestionBanks().add(bank);
        McqQuestion saved = mcqQuestionRepository.save(q);

        assertThat(questionBankRepository.existsByQuestionsId(saved.getId())).isTrue();
    }

    @Test
    void existsByQuestionsId_returnsFalse_whenNoQuestionsAssociated() {
        QuestionBank bank = questionBankRepository.save(new QuestionBank("Empty Bank"));

        assertThat(questionBankRepository.existsByQuestionsId(bank.getId())).isFalse();
    }
}
