package com.wrapitup.wrap.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.event.EventEnvelope;
import com.wrapitup.common.event.payload.WrapGenerationFailedPayload;
import com.wrapitup.wrap.service.GenerationStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for wrap.generation.failed events.
 *
 * Flow:
 * 1. Any upstream service (Spotify, Analysis) encounters an error
 * 2. Service publishes wrap.generation.failed event
 * 3. This consumer receives and deserializes the event
 * 4. Extracts payload and converts to WrapGenerationFailedPayload
 * 5. Marks generation as FAILED with error details
 * 6. Frontend can poll and retrieve error details
 *
 * Consumer Group: wrap-service-group
 * Topic: wrap.generation.failed
 * Partition Strategy: generationId as key for ordered processing
 */
@Slf4j
@Service
public class WrapGenerationFailedConsumer {

    private final GenerationStateService stateService;
    private final ObjectMapper objectMapper;

    public WrapGenerationFailedConsumer(
            GenerationStateService stateService,
            ObjectMapper objectMapper
    ) {
        this.stateService = stateService;
        this.objectMapper = objectMapper;
    }

    /**
     * Consume wrap.generation.failed event and mark generation as failed.
     *
     * EventEnvelope has payload as JsonNode, so we deserialize it to WrapGenerationFailedPayload.
     *
     * @param event The Kafka event envelope containing failure details
     */
    @KafkaListener(
            topics = "wrap.generation.failed",
            groupId = "wrap-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onWrapGenerationFailed(EventEnvelope event) {
        try {
            String generationId = event.getGenerationId();
            String spotifyAccountId = event.getSpotifyAccountId();

            // Deserialize payload JsonNode to WrapGenerationFailedPayload
            WrapGenerationFailedPayload payload = objectMapper.treeToValue(
                    event.getPayload(),
                    WrapGenerationFailedPayload.class
            );

            log.warn("Processing wrap generation failed: generation={}, accountId={}, stage={}, errorCode={}",
                    generationId, spotifyAccountId, payload.getStage(), payload.getErrorCode());

            // Mark generation as FAILED with error details
            stateService.markFailed(
                    generationId,
                    payload.getErrorCode(),
                    payload.getMessage()
            );

            log.warn("Generation marked as failed: generation={}, accountId={}, errorCode={}",
                    generationId, spotifyAccountId, payload.getErrorCode());

            // TODO: Phase 7+ Implement retry logic
            // if (payload.isRetryable()) {
            //     publishRetryEvent(generationId, spotifyAccountId);
            // }

        } catch (Exception e) {
            log.error("Error processing wrap generation failed event", e);
            // Don't re-throw; let Kafka handle offset commit
        }
    }
}