package com.wrapitup.common.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

/**
 * Kafka configuration for Wrap-it-up.
 * Defines topics and their properties.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Create KafkaAdmin bean.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
        ));
    }

    /**
     * Topic: wrap.generation.requested
     * - Produced by: wrap-service
     * - Consumed by: spotify-service
     */
    @Bean
    public NewTopic wrapGenerationRequestedTopic() {
        return new NewTopic(
                "wrap.generation.requested",
                1,
                (short) 1
        ).configs(Map.of(
                TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000),
                TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
        ));
    }

    /**
     * Topic: spotify.snapshot.created
     * - Produced by: spotify-service
     * - Consumed by: analysis-service
     */
    @Bean
    public NewTopic spotifySnapshotCreatedTopic() {
        return new NewTopic(
                "spotify.snapshot.created",
                1,
                (short) 1
        ).configs(Map.of(
                TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000),
                TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
        ));
    }

    /**
     * Topic: wrap.analysis.completed
     * - Produced by: analysis-service
     * - Consumed by: wrap-service
     */
    @Bean
    public NewTopic wrapAnalysisCompletedTopic() {
        return new NewTopic(
                "wrap.analysis.completed",
                1,
                (short) 1
        ).configs(Map.of(
                TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000),
                TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
        ));
    }

    /**
     * Topic: wrap.generation.failed
     * - Produced by: any service
     * - Consumed by: wrap-service
     */
    @Bean
    public NewTopic wrapGenerationFailedTopic() {
        return new NewTopic(
                "wrap.generation.failed",
                1,
                (short) 1
        ).configs(Map.of(
                TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000),
                TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
        ));
    }
}