package com.wrapitup.wrap.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.dto.WrapResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for caching completed Wrap results.
 *
 * Configures:
 * - RedisTemplate with Jackson2JsonRedisSerializer
 * - String keys, JSON-serialized WrapResponse values
 * - Custom ObjectMapper with type information
 *
 * This allows WrapResponse objects to be serialized to JSON
 * and cached in Redis, then deserialized back to objects
 * for client retrieval.
 */
@Configuration
public class RedisConfig {

    /**
     * Create and configure RedisTemplate for WrapResponse caching.
     *
     * Uses:
     * - StringRedisSerializer for cache keys (human-readable)
     * - Jackson2JsonRedisSerializer for cache values (structured data)
     *
     * @param connectionFactory Spring-provided Redis connection factory
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, WrapResponse> redisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, WrapResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure ObjectMapper with type information for polymorphic deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // Create Jackson serializer for WrapResponse with configured ObjectMapper
        Jackson2JsonRedisSerializer<WrapResponse> jacksonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, WrapResponse.class);

        // String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Set serializers
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jacksonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}