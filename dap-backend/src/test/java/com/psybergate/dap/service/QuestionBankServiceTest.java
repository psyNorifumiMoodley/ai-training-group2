package com.psybergate.dap.service;

import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.QuestionBank;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.QuestionBankRequest;
import com.psybergate.dap.dto.QuestionBankResponse;
import com.psybergate.dap.repository.QuestionBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionBankRepository questionBankRepository;

    private QuestionBankService service;

    @BeforeEach
    void setUp() {
        service = new QuestionBankService(questionBankRepository);
    }

    @Test
    void create_success_persistsAndReturnsResponse() {
        QuestionBank saved = new QuestionBank("Java Core");
        saved.setId(UUID.randomUUID());
        when(questionBankRepository.findByName("Java Core")).thenReturn(Optional.empty());
        when(questionBankRepository.save(any(QuestionBank.class))).thenReturn(saved);

        QuestionBankResponse result = service.create(new QuestionBankRequest("Java Core"));

        assertThat(result.name()).isEqualTo("Java Core");
        assertThat(result.id()).isEqualTo(saved.getId());
    }

    @Test
    void create_duplicateName_throwsConflictException() {
        when(questionBankRepository.findByName("Java Core")).thenReturn(Optional.of(new QuestionBank("Java Core")));

        assertThatThrownBy(() -> service.create(new QuestionBankRequest("Java Core")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Java Core");
    }

    @Test
    void rename_toSameName_isIdempotentAndDoesNotSave() {
        UUID id = UUID.randomUUID();
        QuestionBank bank = new QuestionBank("Java Core");
        bank.setId(id);
        when(questionBankRepository.findById(id)).thenReturn(Optional.of(bank));

        QuestionBankResponse result = service.rename(id, new QuestionBankRequest("Java Core"));

        assertThat(result.name()).isEqualTo("Java Core");
        verify(questionBankRepository, never()).save(any());
    }

    @Test
    void rename_toDuplicateName_throwsConflictException() {
        UUID id = UUID.randomUUID();
        QuestionBank bank = new QuestionBank("Old Name");
        bank.setId(id);
        when(questionBankRepository.findById(id)).thenReturn(Optional.of(bank));
        when(questionBankRepository.findByName("New Name")).thenReturn(Optional.of(new QuestionBank("New Name")));

        assertThatThrownBy(() -> service.rename(id, new QuestionBankRequest("New Name")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rename_notFound_throwsNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(questionBankRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rename(id, new QuestionBankRequest("Name")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void delete_withNoQuestions_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        QuestionBank bank = new QuestionBank("Empty Bank");
        bank.setId(id);
        when(questionBankRepository.findById(id)).thenReturn(Optional.of(bank));
        when(questionBankRepository.existsByQuestionsId(id)).thenReturn(false);

        service.delete(id);

        verify(questionBankRepository).delete(bank);
    }

    @Test
    void delete_withQuestions_throwsValidationException() {
        UUID id = UUID.randomUUID();
        QuestionBank bank = new QuestionBank("In-Use Bank");
        bank.setId(id);
        when(questionBankRepository.findById(id)).thenReturn(Optional.of(bank));
        when(questionBankRepository.existsByQuestionsId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("in use");
    }

    @Test
    void delete_notFound_throwsNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(questionBankRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NoSuchElementException.class);
    }
}
