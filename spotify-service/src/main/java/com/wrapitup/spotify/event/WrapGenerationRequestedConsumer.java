package com.wrapitup.spotify.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.event.EventEnvelope;
import com.wrapitup.common.event.payload.SpotifySnapshotPayload;
import com.wrapitup.spotify.service.SpotifyListeningSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Consumer for wrap.generation.requested events.
 *
 * Listens on the wrap.generation.requested topic and:
 * 1. Fetches Spotify data for the user (requires access token from auth context)
 * 2. Normalizes it
 * 3. Publishes spotify.snapshot.created event
 *
 * NOTE: Phase 4 is simplified - we don't have auth context yet.
 * In Phase 5+, will need to retrieve the access token for spotifyAccountId.
 */
@Slf4j
@Service
public class WrapGenerationRequestedConsumer {

    private final SpotifyListeningSnapshotService snapshotService;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WrapGenerationRequestedConsumer(
            SpotifyListeningSnapshotService snapshotService,
            KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.snapshotService = snapshotService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen for wrap.generation.requested events.
     *
     * @param envelope the event envelope containing generation and account info
     */
    @KafkaListener(
            topics = "wrap.generation.requested",
            groupId = "spotify-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeGenerationRequested(EventEnvelope envelope) {
        try {
            String generationId = envelope.getGenerationId();
            String spotifyAccountId = envelope.getSpotifyAccountId();

            log.info("Received wrap.generation.requested event: generation={} account={}",
                    generationId, spotifyAccountId);

            // TODO: Phase 5 - Retrieve access token for spotifyAccountId from auth service/cache
            // For now, this will fail. Will need to integrate with auth service.
            String accessToken = "PLACEHOLDER_ACCESS_TOKEN"; // Will be retrieved from auth context

            // Fetch snapshot from Spotify using the CORRECT method name
            SpotifySnapshotPayload snapshot = snapshotService.fetchListeningSnapshot(accessToken);

            // Publish spotify.snapshot.created event
            publishSnapshot(envelope, snapshot);

        } catch (Exception e) {
            log.error("Error processing wrap.generation.requested event", e);
            // TODO: Publish wrap.generation.failed event (Phase 5)
            // publishFailedEvent(envelope, e);
        }
    }

    /**
     * Publish the spotify.snapshot.created event.
     */
    private void publishSnapshot(EventEnvelope requestEnvelope, SpotifySnapshotPayload snapshot) {
        try {
            EventEnvelope snapshotEvent = EventEnvelope.customBuilder()
                    .eventType("spotify.snapshot.created")
                    .generationId(requestEnvelope.getGenerationId())
                    .spotifyAccountId(requestEnvelope.getSpotifyAccountId())
                    .payload(objectMapper.valueToTree(snapshot))
                    .build();

            // Send to kafka using generationId as partition key
            kafkaTemplate.send(
                    "spotify.snapshot.created",
                    requestEnvelope.getGenerationId(),
                    snapshotEvent
            );

            log.info("Published spotify.snapshot.created event for generation={}",
                    requestEnvelope.getGenerationId());
        } catch (Exception e) {
            log.error("Error publishing spotify.snapshot.created event", e);
            throw new RuntimeException("Failed to publish snapshot event", e);
        }
    }
}