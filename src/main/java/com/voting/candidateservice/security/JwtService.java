package com.voting.candidateservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for validating and parsing JSON Web Tokens.
 * Shared with User Service via JWT_SECRET for stateless cross-service
 * authentication.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    /**
     * Extracts and validates the principal from the token.
     *
     * @param token JWT token string
     * @return Optional containing JwtPrincipal if valid
     */
    public Optional<JwtPrincipal> getPrincipalFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (isTokenExpired(claims)) {
                return Optional.empty();
            }

            UUID userId = UUID.fromString(claims.get("userId", String.class));
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            return Optional.of(new JwtPrincipal(userId, username, role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
