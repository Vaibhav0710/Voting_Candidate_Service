package com.voting.candidateservice.security;

import java.util.UUID;

/**
 * Principal object representing the authenticated user in the Candidate Service.
 *
 * @param userId   Unique identifier of the user
 * @param username Username of the user
 * @param role     Role assigned to the user (e.g., ROLE_VOTER, ROLE_ADMIN)
 */
public record JwtPrincipal(
    UUID userId,
    String username,
    String role
) {}
