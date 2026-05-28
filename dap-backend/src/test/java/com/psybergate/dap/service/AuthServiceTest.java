package com.psybergate.dap.service;

import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.LoginRequest;
import com.psybergate.dap.dto.LoginResponse;
import com.psybergate.dap.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .email("admin@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .name("Admin User")
                .role(Role.ADMIN)
                .build();
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void authenticate_validCredentials_returnsLoginResponse() {
        LoginRequest request = new LoginRequest("admin@example.com", "correctPassword");
        String fakeToken = "fake.jwt.token";

        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(testUser)).thenReturn(fakeToken);

        LoginResponse response = authService.authenticate(request);

        assertThat(response.token()).isEqualTo(fakeToken);
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void authenticate_invalidPassword_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("admin@example.com", "wrongPassword");

        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_unknownEmail_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("unknown@example.com", "anyPassword");

        when(appUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
