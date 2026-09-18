package com.wrapitup.spotify.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.event.payload.SpotifySnapshotPayload;
import com.wrapitup.common.event.payload.WrapGenerationRequestedPayload;
import com.wrapitup.spotify.service.SpotifyListeningSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for wrap.generation.requested events.
 * Triggered when wrap generation starts.
 * Responsibilities:
 * 1. Receive the generation request
 * 2. Fetch Spotify data for the user
 * 3. Publish spotify.snapshot.created event
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WrapGenerationRequestedConsumer {

    private final SpotifyListeningSnapshotService snapshotService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Handle wrap.generation.requested events.
     * Fetches Spotify data and publishes snapshot event.
     */
    @KafkaListener(
            topics = "wrap.generation.requested",
            groupId = "spotify-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWrapGenerationRequested(String eventMessage) {
        log.info("Received wrap.generation.requested event");

        try {
            // Parse the event as a generic map first
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(eventMessage, java.util.Map.class);

            String generationId = (String) eventMap.get("generationId");
            String spotifyAccountId = (String) eventMap.get("spotifyAccountId");

            log.debug("Processing generation request: generationId={}, accountId={}",
                    generationId, spotifyAccountId);

            // TODO: Fetch access token from Auth Service
            // For now, this will be integrated after Auth Service exposes token management
            // String accessToken = getAccessTokenForUser(spotifyAccountId);

            // Fetch Spotify snapshot
            SpotifySnapshotPayload snapshot = snapshotService.fetchListeningSnapshot(
                    "dummy-token" // Will be replaced with actual token from Auth Service
            );

            // Build and publish spotify.snapshot.created event
            publishSpotifySnapshotEvent(generationId, spotifyAccountId, snapshot);

            log.info("Successfully processed wrap generation request: generationId={}", generationId);

        } catch (Exception e) {
            log.error("Failed to process wrap.generation.requested event", e);
            // TODO: Publish wrap.generation.failed event
        }
    }

    /**
     * Publish spotify.snapshot.created event to Kafka.
     */
    private void publishSpotifySnapshotEvent(String generationId, String spotifyAccountId,
                                             SpotifySnapshotPayload snapshot) {
        try {
            // Build event as a map to avoid generic type issues
            java.util.Map<String, Object> event = new java.util.LinkedHashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", "spotify.snapshot.created");
            event.put("eventVersion", 1);
            event.put("occurredAt", java.time.Instant.now().toString());
            event.put("generationId", generationId);
            event.put("spotifyAccountId", spotifyAccountId);
            event.put("payload", snapshot);

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("spotify.snapshot.created", generationId, message);

            log.info("Published spotify.snapshot.created event: generationId={}", generationId);

        } catch (Exception e) {
            log.error("Failed to publish spotify.snapshot.created event", e);
            throw new RuntimeException("Failed to publish snapshot event", e);
        }
    }
}