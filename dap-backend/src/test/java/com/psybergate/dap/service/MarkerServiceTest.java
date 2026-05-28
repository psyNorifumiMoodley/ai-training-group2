package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.MarkerRequest;
import com.psybergate.dap.dto.MarkerResponse;
import com.psybergate.dap.repository.AppUserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkerServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MarkerService markerService;

    @Test
    void register_duplicateEmail_throwsConflictException() {
        when(appUserRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        MarkerRequest request = new MarkerRequest("Jane Marker", "duplicate@example.com", "password123");

        assertThatThrownBy(() -> markerService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("duplicate@example.com");
    }

    @Test
    void register_passwordIsBcryptHashed() {
        when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$hashed");

        AppUser saved = AppUser.builder()
                .name("Jane Marker")
                .email("jane@example.com")
                .passwordHash("$2a$hashed")
                .role(Role.MARKER)
                .build();
        saved.setId(UUID.randomUUID());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        when(appUserRepository.save(captor.capture())).thenReturn(saved);

        MarkerRequest request = new MarkerRequest("Jane Marker", "jane@example.com", "plaintext");
        MarkerResponse response = markerService.register(request);

        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$hashed");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("plaintext");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MARKER);
        assertThat(response.email()).isEqualTo("jane@example.com");
    }
}
