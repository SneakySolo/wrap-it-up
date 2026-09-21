package com.wrapitup.wrap.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.common.dto.WrapResponse;
import com.wrapitup.common.event.EventEnvelope;
import com.wrapitup.common.event.payload.WrapAnalysisCompletedPayload;
import com.wrapitup.wrap.cache.WrapCacheService;
import com.wrapitup.wrap.service.GenerationStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for wrap.analysis.completed events.
 *
 * Flow:
 * 1. Analysis Service publishes wrap.analysis.completed event
 * 2. This consumer receives and deserializes the event
 * 3. Extracts payload and converts to WrapAnalysisCompletedPayload
 * 4. Builds WrapResponse DTO from payload
 * 5. Caches result in Redis with TTL
 * 6. Marks generation as COMPLETED in state service
 *
 * Consumer Group: wrap-service-group
 * Topic: wrap.analysis.completed
 * Partition Strategy: generationId as key for ordered processing
 */
@Slf4j
@Service
public class WrapAnalysisCompletedConsumer {

    private final WrapCacheService cacheService;
    private final GenerationStateService stateService;
    private final ObjectMapper objectMapper;

    public WrapAnalysisCompletedConsumer(
            WrapCacheService cacheService,
            GenerationStateService stateService,
            ObjectMapper objectMapper
    ) {
        this.cacheService = cacheService;
        this.stateService = stateService;
        this.objectMapper = objectMapper;
    }

    /**
     * Consume wrap.analysis.completed event and process results.
     *
     * EventEnvelope has payload as JsonNode, so we deserialize it to WrapAnalysisCompletedPayload.
     *
     * @param event The Kafka event envelope containing analysis results
     */
    @KafkaListener(
            topics = "wrap.analysis.completed",
            groupId = "wrap-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onWrapAnalysisCompleted(EventEnvelope event) {
        try {
            String generationId = event.getGenerationId();
            String spotifyAccountId = event.getSpotifyAccountId();

            log.info("Processing wrap analysis completed: generation={}, accountId={}",
                    generationId, spotifyAccountId);

            // Deserialize payload JsonNode to WrapAnalysisCompletedPayload
            WrapAnalysisCompletedPayload payload = objectMapper.treeToValue(
                    event.getPayload(),
                    WrapAnalysisCompletedPayload.class
            );

            // Build WrapResponse from the analysis payload
            WrapResponse wrap = buildWrapResponse(payload);

            // Cache the completed wrap
            cacheService.save(spotifyAccountId, wrap);

            // Mark generation as COMPLETED
            stateService.markCompleted(generationId);

            log.info("Wrap analysis completed and cached: generation={}, accountId={}",
                    generationId, spotifyAccountId);

        } catch (Exception e) {
            log.error("Error processing wrap analysis completed event", e);
            // Don't re-throw; let Kafka handle offset commit
            // Future: implement dead-letter queue for failed events
        }
    }

    /**
     * Build WrapResponse DTO from analysis payload.
     *
     * Transforms the flat payload structure into the structured WrapResponse
     * that will be returned to the frontend.
     *
     * @param payload Analysis results from Analysis Service
     * @return Complete WrapResponse DTO
     */
    private WrapResponse buildWrapResponse(WrapAnalysisCompletedPayload payload) {
        WrapResponse.PersonalityDto personality = WrapResponse.PersonalityDto.builder()
                .category(payload.getPersonality().getCategory())
                .title(payload.getPersonality().getTitle())
                .description(payload.getPersonality().getDescription())
                .build();

        return WrapResponse.builder()
                .recentListeningMinutes(payload.getRecentListeningMinutes())
                .topArtist(payload.getTopArtist())
                .topTracks(payload.getTopTracks())
                .topAlbum(payload.getTopAlbum())
                .topGenre(payload.getTopGenre())
                .risingStar(payload.getRisingStar())
                .artistLoyalty(payload.getArtistLoyalty())
                .genreDiversity(payload.getGenreDiversity())
                .familiarityRatio(payload.getFamiliarityRatio())
                .discoveryRatio(payload.getDiscoveryRatio())
                .personality(personality)
                .build();
    }
}