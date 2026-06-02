package com.psybergate.dap.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class InvitationTokenUtil {

    private static final String ASSESSMENT_ID_CLAIM = "assessmentId";

    private final SecretKey secretKey;

    public InvitationTokenUtil(
            @Value("${app.invitation-token.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT containing only the assessmentId claim.
     * No expiration is set — expiry is enforced at the business-logic level
     * via the server-side timer on the assessment itself.
     */
    public String generateToken(UUID assessmentId) {
        return Jwts.builder()
                .claim(ASSESSMENT_ID_CLAIM, assessmentId.toString())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the assessmentId from a valid, signed token.
     *
     * @throws JwtException if the token is invalid or the signature does not match
     */
    public UUID extractAssessmentId(String token) {
        String raw = getClaims(token).get(ASSESSMENT_ID_CLAIM, String.class);
        return UUID.fromString(raw);
    }

    /**
     * Returns {@code true} if the token's signature is valid, {@code false} otherwise.
     * Note: because no expiration claim is set, an expired-style check is not performed.
     */
    public boolean isSignatureValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
