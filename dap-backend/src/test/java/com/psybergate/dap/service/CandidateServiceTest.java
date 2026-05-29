package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.CandidateResponse;
import com.psybergate.dap.repository.AppUserRepository;
import com.psybergate.dap.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CandidateService candidateService;

    @Test
    void register_duplicateEmail_throwsConflictException() {
        when(appUserRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> candidateService.register(new CandidateRequest("Jane Doe", "jane@example.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("jane@example.com");

        verify(appUserRepository, never()).save(any());
        verify(candidateRepository, never()).save(any());
    }

    @Test
    void register_newEmail_savesUserAndCandidateAndReturnsResponse() {
        when(appUserRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        AppUser savedUser = AppUser.builder()
                .email("jane@example.com").name("Jane Doe").role(Role.CANDIDATE).passwordHash("$2a$hashed").build();
        UUID userId = UUID.randomUUID();
        savedUser.setId(userId);

        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedUser);

        Candidate savedCandidate = Candidate.builder().user(savedUser).build();
        savedCandidate.setId(userId);
        when(candidateRepository.save(any(Candidate.class))).thenReturn(savedCandidate);

        CandidateResponse response = candidateService.register(new CandidateRequest("Jane Doe", "jane@example.com"));

        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.name()).isEqualTo("Jane Doe");
        assertThat(response.id()).isEqualTo(userId);
    }

    @Test
    void register_passwordHashIsEncoded_notRawValue() {
        when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        AppUser savedUser = AppUser.builder()
                .email("jane@example.com").name("Jane Doe").role(Role.CANDIDATE).passwordHash("$2a$hashed").build();
        savedUser.setId(UUID.randomUUID());
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedUser);

        Candidate savedCandidate = Candidate.builder().user(savedUser).build();
        savedCandidate.setId(savedUser.getId());
        when(candidateRepository.save(any(Candidate.class))).thenReturn(savedCandidate);

        candidateService.register(new CandidateRequest("Jane Doe", "jane@example.com"));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());

        String hash = userCaptor.getValue().getPasswordHash();
        assertThat(hash).isEqualTo("$2a$hashed");
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void register_candidateRoleIsSet() {
        when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        AppUser savedUser = AppUser.builder()
                .email("jane@example.com").name("Jane Doe").role(Role.CANDIDATE).passwordHash("$2a$hashed").build();
        savedUser.setId(UUID.randomUUID());
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedUser);

        Candidate savedCandidate = Candidate.builder().user(savedUser).build();
        savedCandidate.setId(savedUser.getId());
        when(candidateRepository.save(any(Candidate.class))).thenReturn(savedCandidate);

        candidateService.register(new CandidateRequest("Jane Doe", "jane@example.com"));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.CANDIDATE);
    }
}
