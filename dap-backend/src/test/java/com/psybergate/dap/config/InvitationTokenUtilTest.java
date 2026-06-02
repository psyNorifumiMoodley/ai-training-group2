package com.psybergate.dap.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTokenUtilTest {

    // 64 chars = 512 bits — well above the 256-bit minimum for HS256
    private static final String TEST_SECRET =
            "invitation-test-secret-that-is-long-enough-for-hs256-64-chars!!";

    private InvitationTokenUtil tokenUtil;

    @BeforeEach
    void setUp() {
        tokenUtil = new InvitationTokenUtil(TEST_SECRET);
    }

    @Test
    void generateToken_extractAssessmentId_roundTrip() {
        UUID assessmentId = UUID.randomUUID();

        String token = tokenUtil.generateToken(assessmentId);

        assertThat(token).isNotBlank();
        assertThat(tokenUtil.extractAssessmentId(token)).isEqualTo(assessmentId);
    }

    @Test
    void isSignatureValid_validToken_returnsTrue() {
        UUID assessmentId = UUID.randomUUID();
        String token = tokenUtil.generateToken(assessmentId);

        assertThat(tokenUtil.isSignatureValid(token)).isTrue();
    }

    @Test
    void isSignatureValid_tamperedToken_returnsFalse() {
        UUID assessmentId = UUID.randomUUID();
        String token = tokenUtil.generateToken(assessmentId);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(tokenUtil.isSignatureValid(tampered)).isFalse();
    }

    @Test
    void isSignatureValid_tokenSignedWithDifferentSecret_returnsFalse() {
        String otherSecret = "completely-different-secret-key-that-is-64-chars-loooooooooooong!";
        InvitationTokenUtil otherUtil = new InvitationTokenUtil(otherSecret);

        UUID assessmentId = UUID.randomUUID();
        String tokenFromOther = otherUtil.generateToken(assessmentId);

        assertThat(tokenUtil.isSignatureValid(tokenFromOther)).isFalse();
    }

    @Test
    void isSignatureValid_blankToken_returnsFalse() {
        assertThat(tokenUtil.isSignatureValid("")).isFalse();
        assertThat(tokenUtil.isSignatureValid("not.a.jwt")).isFalse();
    }

    @Test
    void generateToken_hasNoExpirationClaim() {
        UUID assessmentId = UUID.randomUUID();
        String token = tokenUtil.generateToken(assessmentId);

        // Parse the token manually to inspect the claims payload directly
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getExpiration()).isNull();
    }

    @Test
    void generateToken_differentAssessments_produceDifferentTokens() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        String token1 = tokenUtil.generateToken(id1);
        String token2 = tokenUtil.generateToken(id2);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(tokenUtil.extractAssessmentId(token1)).isEqualTo(id1);
        assertThat(tokenUtil.extractAssessmentId(token2)).isEqualTo(id2);
    }
}
