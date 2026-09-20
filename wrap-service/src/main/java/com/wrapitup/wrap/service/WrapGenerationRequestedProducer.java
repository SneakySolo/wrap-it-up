package com.wrapitup.wrap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.event.EventEnvelope;
import com.wrapitup.common.event.payload.WrapGenerationRequestedPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer for wrap.generation.requested events.
 *
 * Orchestrates the start of a Wrap generation by publishing an event to Kafka.
 * The Spotify Service listens for this event and fetches Spotify data.
 */
@Slf4j
@Service
public class WrapGenerationRequestedProducer {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WrapGenerationRequestedProducer(
            KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish a wrap.generation.requested event.
     *
     * @param generationId unique identifier for this generation
     * @param spotifyAccountId user's Spotify account ID
     */
    public void publishGenerationRequested(String generationId, String spotifyAccountId) {
        try {
            // Build event envelope using customBuilder() to avoid conflicts
            EventEnvelope envelope = EventEnvelope.customBuilder()
                    .eventType("wrap.generation.requested")
                    .generationId(generationId)
                    .spotifyAccountId(spotifyAccountId)
                    .payload(objectMapper.valueToTree(new WrapGenerationRequestedPayload()))
                    .build();

            // Send to Kafka with generationId as partition key to ensure ordering
            kafkaTemplate.send("wrap.generation.requested", generationId, envelope);

            log.info("Published wrap.generation.requested event for generation={} spotify_account={}",
                    generationId, spotifyAccountId);
        } catch (Exception e) {
            log.error("Error publishing wrap.generation.requested event", e);
            throw new RuntimeException("Failed to publish generation requested event", e);
        }
    }
}