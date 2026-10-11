package com.voting.candidateservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voting.candidateservice.dto.CandidateResponseDTO;
import com.voting.candidateservice.model.enums.CandidateStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Redis cache serializers must round-trip cached values exactly, without Java type metadata.
 */
class CacheConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final CandidateResponseDTO candidate = CandidateResponseDTO.builder()
            .id(UUID.randomUUID())
            .name("Asha Rao")
            .party("Civic Party")
            .electionId(UUID.randomUUID())
            .status(CandidateStatus.ACTIVE)
            .createdAt(LocalDateTime.of(2026, 10, 11, 9, 30))
            .updatedAt(LocalDateTime.of(2026, 10, 11, 9, 45))
            .build();

    @Test
    void candidateSerializer_roundTripsDto() {
        RedisSerializer<CandidateResponseDTO> serializer = CacheConfig.candidateSerializer(objectMapper);

        assertThat(serializer.deserialize(serializer.serialize(candidate))).isEqualTo(candidate);
    }

    @Test
    void candidateListSerializer_roundTripsList() {
        RedisSerializer<List<CandidateResponseDTO>> serializer = CacheConfig.candidateListSerializer(objectMapper);

        assertThat(serializer.deserialize(serializer.serialize(List.of(candidate)))).containsExactly(candidate);
    }

    @Test
    void serializedValue_containsNoJavaTypeMetadata() {
        byte[] json = CacheConfig.candidateSerializer(objectMapper).serialize(candidate);

        assertThat(new String(json)).doesNotContain("@class").doesNotContain("com.voting");
    }
}
