package com.psybergate.dap.config;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    // 64 chars = 512 bits — well above the 256-bit minimum for HS256
    private static final String TEST_SECRET =
            "test-secret-key-that-is-long-enough-for-hs256-at-least-64-chars!";

    private JwtUtil jwtUtil;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 24L);

        testUser = AppUser.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$irrelevant")
                .name("Test User")
                .role(Role.ADMIN)
                .build();
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void generateToken_roundTrip_extractsUserIdAndRole() {
        String token = jwtUtil.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtUtil.extractRole(token)).isEqualTo(Role.ADMIN);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // Use 0 expiration hours to produce an already-expired token
        JwtUtil expiredJwtUtil = new JwtUtil(TEST_SECRET, 0L);
        String token = expiredJwtUtil.generateToken(testUser);

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_blankToken_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
        assertThat(jwtUtil.isTokenValid("not.a.jwt")).isFalse();
    }
}
