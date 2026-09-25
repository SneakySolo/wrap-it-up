package com.wrapitup.analysis.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapitup.analysis.analyzer.*;
import com.wrapitup.analysis.engine.PersonalityEngine;
import com.wrapitup.common.domain.RecentlyPlayedItem;
import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import com.wrapitup.common.event.EventEnvelope;
import com.wrapitup.common.event.payload.SpotifySnapshotPayload;
import com.wrapitup.common.event.payload.WrapAnalysisCompletedPayload;
import com.wrapitup.common.event.payload.WrapGenerationFailedPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Kafka consumer that listens to spotify.snapshot.created events.
 *
 * Flow:
 * 1. Receive normalized Spotify data from Spotify Service
 * 2. Run all analyzers (Artist, Album, Genre, Discovery)
 * 3. Calculate internal metrics (taste movement)
 * 4. Run Personality Engine for classification
 * 5. Publish wrap.analysis.completed event
 *
 * Handles errors by publishing wrap.generation.failed event.
 */
@Slf4j
@Service
public class SpotifySnapshotListener {

    private final ArtistAnalyzer artistAnalyzer;
    private final AlbumAnalyzer albumAnalyzer;
    private final GenreAnalyzer genreAnalyzer;
    private final DiscoveryAnalyzer discoveryAnalyzer;
    private final PersonalityEngine personalityEngine;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SpotifySnapshotListener(
            ArtistAnalyzer artistAnalyzer,
            AlbumAnalyzer albumAnalyzer,
            GenreAnalyzer genreAnalyzer,
            DiscoveryAnalyzer discoveryAnalyzer,
            PersonalityEngine personalityEngine,
            KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.artistAnalyzer = artistAnalyzer;
        this.albumAnalyzer = albumAnalyzer;
        this.genreAnalyzer = genreAnalyzer;
        this.discoveryAnalyzer = discoveryAnalyzer;
        this.personalityEngine = personalityEngine;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens to spotify.snapshot.created events and performs analysis.
     */
    @KafkaListener(
            topics = "spotify.snapshot.created",
            groupId = "analysis-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSpotifySnapshot(EventEnvelope event) {
        try {
            log.info("Processing spotify snapshot for generation: {}", event.getGenerationId());

            // Extract and deserialize payload from JsonNode
            SpotifySnapshotPayload payload = objectMapper.treeToValue(event.getPayload(), SpotifySnapshotPayload.class);

            if (payload == null) {
                throw new IllegalArgumentException("Snapshot payload is missing");
            }

            // Extract data. Spotify can legitimately return empty artist lists for
            // accounts without enough listening history, so treat those lists as
            // optional and keep the analysis pipeline null-safe.
            List<SpotifyArtist> shortTermArtists = payload.getTopArtists() != null
                    && payload.getTopArtists().getShortTerm() != null
                    ? payload.getTopArtists().getShortTerm()
                    : List.of();
            List<SpotifyArtist> mediumTermArtists = payload.getTopArtists() != null
                    && payload.getTopArtists().getMediumTerm() != null
                    ? payload.getTopArtists().getMediumTerm()
                    : List.of();
            List<SpotifyArtist> longTermArtists = payload.getTopArtists() != null
                    && payload.getTopArtists().getLongTerm() != null
                    ? payload.getTopArtists().getLongTerm()
                    : List.of();
            List<SpotifyTrack> topTracks = payload.getTopTracks() != null
                    && payload.getTopTracks().getLongTerm() != null
                    ? payload.getTopTracks().getLongTerm()
                    : List.of();
            List<RecentlyPlayedItem> recentlyPlayed = payload.getRecentlyPlayed() != null
                    ? payload.getRecentlyPlayed()
                    : List.of();

            // The top-tracks response includes the primary artist even when the
            // separate top-artists endpoint is empty. Use it as a best-effort
            // fallback so the completed wrap still has a useful top artist.
            if (longTermArtists.isEmpty()) {
                longTermArtists = deriveArtistsFromTracks(topTracks);
            }

            // Validate critical data
            if (topTracks.isEmpty()) {
                throw new IllegalArgumentException("Snapshot contains no top tracks");
            }

            // ===== PERFORM ANALYSIS =====

            // 1. Listening Time
            int recentListeningMinutes = calculateRecentListeningMinutes(recentlyPlayed);

            // 2. Top Artist
            SpotifyArtist topArtist = artistAnalyzer.getTopArtist(longTermArtists);

            // 3. Top Tracks (take first 5 from top 50 for display)
            List<SpotifyTrack> topTracksDisplay = topTracks.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            // 4. Album Analysis
            Map<String, Object> albumAnalysis = albumAnalyzer.analyzeAlbums(topTracks);
            Object mostListenedAlbumObj = albumAnalysis.get("mostListenedAlbum");
            Double albumConcentrationObj = (Double) albumAnalysis.get("albumConcentration");
            double albumConcentration = albumConcentrationObj != null ? albumConcentrationObj : 0.0;

            // 5. Top Genre
            String topGenre = genreAnalyzer.getTopGenre(longTermArtists.subList(0, Math.min(20, longTermArtists.size())));

            // 6. Rising Star
            SpotifyArtist risingStar = artistAnalyzer.getRisingStar(
                    shortTermArtists.subList(0, Math.min(20, shortTermArtists.size())),
                    longTermArtists.subList(0, Math.min(20, longTermArtists.size()))
            );

            // 7. Artist Loyalty (HHI)
            Map<String, Integer> artistTrackCounts = countTracksPerArtist(topTracks);
            int artistLoyalty = artistAnalyzer.calculateArtistLoyalty(artistTrackCounts);

            // 8. Genre Diversity
            int genreDiversity = genreAnalyzer.calculateGenreDiversity(
                    longTermArtists.subList(0, Math.min(20, longTermArtists.size()))
            );

            // 9. Familiar vs Discovery
            Map<String, Object> discoveryAnalysis = discoveryAnalyzer.analyzeFamiliarityAndDiscovery(
                    recentlyPlayed,
                    longTermArtists.subList(0, Math.min(20, longTermArtists.size()))
            );
            double familiarityRatio = (Double) discoveryAnalysis.get("familiarityRatio");
            double discoveryRatio = (Double) discoveryAnalysis.get("discoveryRatio");

            // 10. Taste Movement (internal metric)
            double tasteMovement = discoveryAnalyzer.calculateTasteMovement(
                    shortTermArtists.subList(0, Math.min(20, shortTermArtists.size())),
                    longTermArtists.subList(0, Math.min(20, longTermArtists.size()))
            );

            // 11. Personality Classification
            WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                    artistLoyalty,
                    genreDiversity,
                    albumConcentration,
                    tasteMovement,
                    familiarityRatio,
                    discoveryRatio
            );

            // ===== BUILD COMPLETED ANALYSIS EVENT =====

            WrapAnalysisCompletedPayload analysisPayload = WrapAnalysisCompletedPayload.builder()
                    .recentListeningMinutes(recentListeningMinutes)
                    .topArtist(topArtist)
                    .topTracks(topTracksDisplay)
                    .topAlbum((Object) mostListenedAlbumObj != null ? (com.wrapitup.common.domain.SpotifyAlbum) mostListenedAlbumObj : new com.wrapitup.common.domain.SpotifyAlbum())
                    .topGenre(topGenre)
                    .risingStar(risingStar)
                    .artistLoyalty(artistLoyalty)
                    .genreDiversity(genreDiversity)
                    .familiarityRatio(familiarityRatio)
                    .discoveryRatio(discoveryRatio)
                    .personality(personality)
                    .build();

            EventEnvelope completedEvent = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("wrap.analysis.completed")
                    .eventVersion(1)
                    .occurredAt(Instant.now().toString())
                    .generationId(event.getGenerationId())
                    .spotifyAccountId(event.getSpotifyAccountId())
                    .payload(objectMapper.valueToTree(analysisPayload))
                    .build();

            log.info("Publishing wrap.analysis.completed for generation: {}", event.getGenerationId());
            kafkaTemplate.send("wrap.analysis.completed", event.getGenerationId(), completedEvent);

        } catch (Exception e) {
            log.error("Error processing spotify snapshot for generation: {}", event.getGenerationId(), e);
            publishFailureEvent(event, "ANALYSIS", e);
        }
    }

    /**
     * Calculates total listening time in minutes from recently played items.
     */
    private int calculateRecentListeningMinutes(List<RecentlyPlayedItem> recentlyPlayed) {
        if (recentlyPlayed == null || recentlyPlayed.isEmpty()) {
            return 0;
        }

        return (int) recentlyPlayed.stream()
                .filter(item -> item.getTrack() != null)
                .mapToLong(item -> item.getTrack().getDurationMs())
                .sum() / 60000; // Convert milliseconds to minutes
    }

    /**
     * Counts how many top-50 tracks belong to each artist.
     */
    private Map<String, Integer> countTracksPerArtist(List<SpotifyTrack> topTracks) {
        Map<String, Integer> counts = new HashMap<>();
        for (SpotifyTrack track : topTracks) {
            String artistId = track.getArtistId();
            counts.put(artistId, counts.getOrDefault(artistId, 0) + 1);
        }
        return counts;
    }

    private List<SpotifyArtist> deriveArtistsFromTracks(List<SpotifyTrack> topTracks) {
        Map<String, SpotifyArtist> artistsById = new LinkedHashMap<>();
        for (SpotifyTrack track : topTracks) {
            if (track == null || track.getArtistId() == null || track.getArtistId().isBlank()) {
                continue;
            }

            artistsById.putIfAbsent(track.getArtistId(), SpotifyArtist.builder()
                    .id(track.getArtistId())
                    .name(track.getArtistName())
                    .genres(List.of())
                    .build());
        }
        return new ArrayList<>(artistsById.values());
    }

    /**
     * Publishes a wrap.generation.failed event when analysis fails.
     */
    private void publishFailureEvent(EventEnvelope originalEvent, String stage, Exception e) {
        try {
            WrapGenerationFailedPayload failurePayload = WrapGenerationFailedPayload.builder()
                    .stage(stage)
                    .errorCode("ANALYSIS_FAILURE")
                    .retryable(false)
                    .message(e.getMessage())
                    .build();

            EventEnvelope failureEvent = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("wrap.generation.failed")
                    .eventVersion(1)
                    .occurredAt(Instant.now().toString())
                    .generationId(originalEvent.getGenerationId())
                    .spotifyAccountId(originalEvent.getSpotifyAccountId())
                    .payload(objectMapper.valueToTree(failurePayload))
                    .build();

            kafkaTemplate.send("wrap.generation.failed", originalEvent.getGenerationId(), failureEvent);
            log.info("Published wrap.generation.failed for generation: {}", originalEvent.getGenerationId());
        } catch (Exception ex) {
            log.error("Failed to publish failure event", ex);
        }
    }
}
