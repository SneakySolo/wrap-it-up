package com.wrapitup.analysis.analyzer;

import com.wrapitup.common.domain.SpotifyArtist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes artist listening patterns.
 *
 * Responsibilities:
 * - Extract top artist from long-term rankings
 * - Calculate Rising Star: compare short-term vs long-term rankings with growth scoring
 * - Calculate Artist Loyalty using Herfindahl-Hirschman Index (HHI)
 */
@Slf4j
@Component
public class ArtistAnalyzer {

    private static final int RISING_STAR_PROTECTION_RANK = 10;

    /**
     * Extracts the top long-term artist.
     *
     * @param longTermArtists list of artists ranked by long-term listening
     * @return the top artist, or empty if list is empty
     */
    public SpotifyArtist getTopArtist(List<SpotifyArtist> longTermArtists) {
        if (longTermArtists == null || longTermArtists.isEmpty()) {
            log.warn("No long-term artists available");
            return new SpotifyArtist();
        }
        return longTermArtists.get(0);
    }

    /**
     * Calculates the Rising Star based on growth between short-term and long-term rankings.
     *
     * Algorithm:
     * - Score short-term artists: rank 1 -> 20, rank 2 -> 19, ..., rank 20 -> 1
     * - Score long-term artists: rank 1 -> 20, rank 2 -> 19, ..., rank 20 -> 1
     * - For each artist: growthScore = shortTermScore - longTermScore
     * - Protection rule: short-term rank must be <= 10 (position <= 9)
     * - Return artist with highest growth score
     *
     * @param shortTermArtists top 20 short-term artists
     * @param longTermArtists top 20 long-term artists
     * @return the rising star artist, or empty if no qualified artist exists
     */
    public SpotifyArtist getRisingStar(List<SpotifyArtist> shortTermArtists,
                                       List<SpotifyArtist> longTermArtists) {
        if (shortTermArtists == null || shortTermArtists.isEmpty()) {
            log.warn("No short-term artists available for rising star calculation");
            return new SpotifyArtist();
        }

        // Create mappings of artist ID to rank (0-indexed) for scoring
        Map<String, Integer> shortTermRanks = buildArtistRankMap(shortTermArtists);
        Map<String, Integer> longTermRanks = buildArtistRankMap(longTermArtists);

        SpotifyArtist risingStar = new SpotifyArtist();
        int maxGrowthScore = Integer.MIN_VALUE;

        // Iterate through short-term artists
        for (int shortRank = 0; shortRank < shortTermArtists.size(); shortRank++) {
            SpotifyArtist artist = shortTermArtists.get(shortRank);

            // Protection rule: artist must be in top 10 of short-term list
            if (shortRank >= RISING_STAR_PROTECTION_RANK) {
                continue;
            }

            int shortTermScore = scoreByRank(shortRank, shortTermArtists.size());
            int longTermScore = longTermRanks.containsKey(artist.getId())
                    ? scoreByRank(longTermRanks.get(artist.getId()), 20)
                    : 0;

            int growthScore = shortTermScore - longTermScore;

            if (growthScore > maxGrowthScore) {
                maxGrowthScore = growthScore;
                risingStar = artist;
            }
        }

        if (maxGrowthScore == Integer.MIN_VALUE) {
            log.warn("No qualified rising star found");
            return new SpotifyArtist();
        }

        log.info("Rising Star identified: {} (growth score: {})", risingStar.getName(), maxGrowthScore);
        return risingStar;
    }

    /**
     * Calculates artist loyalty using the Herfindahl-Hirschman Index (HHI).
     *
     * Algorithm:
     * - Count how many of the top 50 tracks belong to each artist
     * - Calculate each artist's market share: artistShare = trackCount / 50
     * - Calculate HHI = sum(artistShare²)
     * - Normalize HHI into a 0-100 score
     * - Higher HHI (more concentration) = higher loyalty score
     *
     * @param topTracksArtistCounts map of artist IDs to track counts in top 50
     * @return integer score 0-100
     */
    public int calculateArtistLoyalty(Map<String, Integer> topTracksArtistCounts) {
        if (topTracksArtistCounts == null || topTracksArtistCounts.isEmpty()) {
            log.warn("No artist counts available for loyalty calculation");
            return 0;
        }

        int totalTracks = topTracksArtistCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalTracks == 0) {
            return 0;
        }

        // Calculate HHI: sum of squared market shares
        double hhi = topTracksArtistCounts.values().stream()
                .mapToDouble(count -> (double) count / totalTracks)
                .map(share -> share * share)
                .sum();

        // Normalize HHI to 0-100 scale
        // HHI ranges from 0 (perfect competition) to 1 (monopoly)
        // Map this to 0-100 user-friendly score
        int loyaltyScore = (int) Math.round(hhi * 100);
        loyaltyScore = Math.min(100, Math.max(0, loyaltyScore)); // Clamp to 0-100

        log.info("Artist loyalty calculated: {}/100 (HHI: {})", loyaltyScore, hhi);
        return loyaltyScore;
    }

    /**
     * Builds a map of artist ID to rank (0-indexed).
     */
    private Map<String, Integer> buildArtistRankMap(List<SpotifyArtist> artists) {
        Map<String, Integer> rankMap = new HashMap<>();
        for (int i = 0; i < artists.size(); i++) {
            rankMap.put(artists.get(i).getId(), i);
        }
        return rankMap;
    }

    /**
     * Calculates a score based on rank.
     * rank 0 -> score = listSize
     * rank 1 -> score = listSize - 1
     * ...
     * rank n -> score = listSize - n
     */
    private int scoreByRank(int rank, int listSize) {
        return listSize - rank;
    }
}