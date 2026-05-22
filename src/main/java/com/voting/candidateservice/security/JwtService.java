package com.voting.candidateservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for validating and parsing JSON Web Tokens.
 * Shared with User Service via JWT_SECRET for stateless cross-service
 * authentication.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtService initialized — algorithm=HS256 (read-only mode)");
    }

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
                log.warn("Token expired for subject: {}", claims.getSubject());
                return Optional.empty();
            }

            UUID userId = UUID.fromString(claims.get("userId", String.class));
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            return Optional.of(new JwtPrincipal(userId, username, role));
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return Optional.empty();
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error parsing JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
