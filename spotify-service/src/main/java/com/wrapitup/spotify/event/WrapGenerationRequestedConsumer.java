package com.wrapitup.spotify.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.event.EventEnvelope;
import com.wrapitup.common.event.payload.SpotifySnapshotPayload;
import com.wrapitup.spotify.service.SpotifyListeningSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.wrapitup.common.event.payload.WrapGenerationFailedPayload;
import reactor.core.publisher.Mono;

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

    @Autowired
    private WebClient webClient;

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

            // REAL: Fetch access token from auth-service
            String accessToken = fetchAccessToken(spotifyAccountId);
            if (accessToken == null) {
                log.error("Failed to fetch access token for account: {}", spotifyAccountId);
                publishFailedEvent(envelope, "MISSING_TOKEN", "No token available for account");
                return;
            }

            // Fetch snapshot from Spotify
            SpotifySnapshotPayload snapshot = snapshotService.fetchListeningSnapshot(accessToken);

            // Publish spotify.snapshot.created event
            publishSnapshot(envelope, snapshot);

        } catch (Exception e) {
            log.error("Error processing wrap.generation.requested event: {}", e.getMessage(), e);
            publishFailedEvent(envelope, "SPOTIFY_FETCH_FAILED", e.getMessage());
        }
    }

    private String fetchAccessToken(String spotifyAccountId) {
        try {
            String authServiceUrl = "http://localhost:8081/internal/tokens/" + spotifyAccountId;

            return webClient.get()
                    .uri(authServiceUrl)
                    .retrieve()
                    .bodyToMono(com.fasterxml.jackson.databind.JsonNode.class)
                    .map(response -> response.get("accessToken").asText())
                    .doOnError(error -> {
                        log.warn("Auth service error fetching token for account {}: {}",
                                spotifyAccountId, error.getMessage());
                    })
                    .onErrorReturn(null)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch token from auth-service for account: {}",
                    spotifyAccountId, e);
            return null;
        }
    }

    // Add this method to publish failed events (implement the TODO from before)
    private void publishFailedEvent(EventEnvelope requestEnvelope, String errorCode, String message) {
        try {
            WrapGenerationFailedPayload failPayload = WrapGenerationFailedPayload.builder()
                    .stage("SPOTIFY_FETCH")
                    .errorCode(errorCode)
                    .retryable(true)
                    .message(message)
                    .build();

            EventEnvelope failedEvent = EventEnvelope.customBuilder()
                    .eventType("wrap.generation.failed")
                    .generationId(requestEnvelope.getGenerationId())
                    .spotifyAccountId(requestEnvelope.getSpotifyAccountId())
                    .payload(objectMapper.valueToTree(failPayload))
                    .build();

            kafkaTemplate.send(
                    "wrap.generation.failed",
                    requestEnvelope.getGenerationId(),
                    failedEvent
            );

            log.info("Published wrap.generation.failed event for generation={}",
                    requestEnvelope.getGenerationId());
        } catch (Exception e) {
            log.error("Error publishing wrap.generation.failed event", e);
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