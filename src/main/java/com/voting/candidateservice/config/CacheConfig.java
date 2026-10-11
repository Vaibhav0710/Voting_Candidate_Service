package com.voting.candidateservice.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.candidateservice.dto.CandidateResponseDTO;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Read caches for candidate lookups (Redis in production, in-memory in tests).
 * <p>
 * Each cache has its own typed JSON serializer, so cached values carry no Java class names (no
 * polymorphic deserialization from Redis). Entries live {@value #TTL_MINUTES} minutes and are evicted on
 * every write. Cache failures (e.g. Redis down) are logged and the database is read instead.
 * The vote-time validation endpoint is deliberately not cached.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String CANDIDATE = "candidate";
    public static final String CANDIDATES_BY_ELECTION = "candidatesByElection";
    public static final String ACTIVE_CANDIDATES_BY_ELECTION = "activeCandidatesByElection";
    static final long TTL_MINUTES = 10;

    @Bean
    public RedisCacheManagerBuilderCustomizer candidateCaches(ObjectMapper objectMapper) {
        return builder -> builder
                .withCacheConfiguration(CANDIDATE, cacheFor(candidateSerializer(objectMapper)))
                .withCacheConfiguration(CANDIDATES_BY_ELECTION, cacheFor(candidateListSerializer(objectMapper)))
                .withCacheConfiguration(ACTIVE_CANDIDATES_BY_ELECTION, cacheFor(candidateListSerializer(objectMapper)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    static RedisSerializer<CandidateResponseDTO> candidateSerializer(ObjectMapper objectMapper) {
        return new Jackson2JsonRedisSerializer<>(objectMapper, CandidateResponseDTO.class);
    }

    static RedisSerializer<List<CandidateResponseDTO>> candidateListSerializer(ObjectMapper objectMapper) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, CandidateResponseDTO.class);
        return new Jackson2JsonRedisSerializer<>(objectMapper, listType);
    }

    private static RedisCacheConfiguration cacheFor(RedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(TTL_MINUTES))
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(valueSerializer));
    }
}
