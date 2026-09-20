package com.wrapitup.analysis.analyzer;

import com.wrapitup.common.domain.RecentlyPlayedItem;
import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes discovery vs. familiarity patterns.
 *
 * Compares recent listening against established long-term taste to determine
 * what percentage of recent listening is familiar vs. new discovery.
 */
@Slf4j
@Component
public class DiscoveryAnalyzer {

    /**
     * Calculates familiarity and discovery ratios based on recently played tracks
     * compared to long-term top artists.
     *
     * Algorithm:
     * - Extract unique artists from recently played tracks
     * - Extract long-term top artists
     * - Familiar artists = intersection of recent and long-term
     * - Discovery artists = recent artists not in long-term
     * - Familiarity ratio = familiarArtists.size / recentArtists.size
     * - Discovery ratio = discoveryArtists.size / recentArtists.size
     *
     * @param recentlyPlayed list of recently played items
     * @param longTermArtists list of long-term top artists
     * @return a map containing:
     *         - "familiarityRatio": Double (0.0 to 1.0)
     *         - "discoveryRatio": Double (0.0 to 1.0)
     *         - "tasteMovement": Double (0.0 to 1.0) - internal metric for personality engine
     */
    public Map<String, Object> analyzeFamiliarityAndDiscovery(
            List<RecentlyPlayedItem> recentlyPlayed,
            List<SpotifyArtist> longTermArtists) {

        if (recentlyPlayed == null || recentlyPlayed.isEmpty()) {
            log.warn("No recently played items available");
            return Map.of(
                    "familiarityRatio", 0.0,
                    "discoveryRatio", 0.0,
                    "tasteMovement", 0.0
            );
        }

        // Extract unique artists from recently played tracks
        Set<String> recentArtistIds = recentlyPlayed.stream()
                .filter(item -> item.getTrack() != null)
                .map(item -> item.getTrack().getArtistId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (recentArtistIds.isEmpty()) {
            log.warn("No artists found in recently played items");
            return Map.of(
                    "familiarityRatio", 0.0,
                    "discoveryRatio", 0.0,
                    "tasteMovement", 0.0
            );
        }

        // Extract long-term artist IDs
        Set<String> longTermArtistIds = longTermArtists.stream()
                .map(SpotifyArtist::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Calculate intersection (familiar) and difference (discovery)
        Set<String> familiarArtists = new HashSet<>(recentArtistIds);
        familiarArtists.retainAll(longTermArtistIds);

        Set<String> discoveryArtists = new HashSet<>(recentArtistIds);
        discoveryArtists.removeAll(longTermArtistIds);

        // Calculate ratios
        double familiarityRatio = (double) familiarArtists.size() / recentArtistIds.size();
        double discoveryRatio = (double) discoveryArtists.size() / recentArtistIds.size();

        log.info("Familiarity analysis: {} familiar, {} discovery out of {} recent artists",
                familiarArtists.size(), discoveryArtists.size(), recentArtistIds.size());

        // Store ratios for later use
        return Map.of(
                "familiarityRatio", familiarityRatio,
                "discoveryRatio", discoveryRatio,
                "tasteMovement", 0.0 // Will be calculated separately if needed
        );
    }

    /**
     * Calculates taste movement based on short-term vs. long-term artist overlap.
     *
     * Algorithm:
     * - Compare top 20 short-term artists with top 20 long-term artists
     * - Calculate overlap ratio = intersection.size / union.size
     * - Taste movement = 1 - overlap ratio (0 = stable taste, 1 = completely different)
     * - Normalize to 0-100 if useful
     *
     * This is an internal metric used by the Personality Engine but not displayed as its own card.
     *
     * @param shortTermArtists top 20 short-term artists
     * @param longTermArtists top 20 long-term artists
     * @return Double representing taste movement (0.0 to 1.0)
     */
    public double calculateTasteMovement(List<SpotifyArtist> shortTermArtists,
                                         List<SpotifyArtist> longTermArtists) {

        if (shortTermArtists == null || longTermArtists == null ||
                shortTermArtists.isEmpty() || longTermArtists.isEmpty()) {
            log.warn("Insufficient artist data for taste movement calculation");
            return 0.0;
        }

        Set<String> shortTermIds = shortTermArtists.stream()
                .map(SpotifyArtist::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> longTermIds = longTermArtists.stream()
                .map(SpotifyArtist::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Calculate intersection
        Set<String> intersection = new HashSet<>(shortTermIds);
        intersection.retainAll(longTermIds);

        // Calculate union
        Set<String> union = new HashSet<>(shortTermIds);
        union.addAll(longTermIds);

        if (union.isEmpty()) {
            return 0.0;
        }

        double overlapRatio = (double) intersection.size() / union.size();
        double tasteMovement = 1.0 - overlapRatio;

        log.info("Taste movement calculated: {} (overlap: {}, union: {})",
                tasteMovement, intersection.size(), union.size());

        return tasteMovement;
    }
}