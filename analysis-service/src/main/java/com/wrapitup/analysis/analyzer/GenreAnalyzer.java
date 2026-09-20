package com.wrapitup.analysis.analyzer;

import com.wrapitup.common.domain.SpotifyArtist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes genre listening patterns.
 *
 * Responsibilities:
 * - Normalize detailed genres into broader categories (e.g., rap, trap, hip hop -> Hip-Hop)
 * - Identify the top genre based on weighted artist scores
 * - Calculate genre diversity using Shannon entropy
 */
@Slf4j
@Component
public class GenreAnalyzer {

    /**
     * Analyzes top 20 artists to determine the top genre.
     *
     * Algorithm:
     * - For each artist, assign a rank weight: rank 1 -> 20, rank 2 -> 19, ..., rank 20 -> 1
     * - For each genre tag on each artist, add the artist's weight to that genre's score
     * - Normalize detailed genres into broader categories
     * - Return the broad genre with the highest weighted score
     *
     * @param topArtists top 20 long-term artists
     * @return the top genre string
     */
    public String getTopGenre(List<SpotifyArtist> topArtists) {
        if (topArtists == null || topArtists.isEmpty()) {
            log.warn("No top artists available for genre analysis");
            return "Unknown";
        }

        Map<String, Integer> genreScores = new HashMap<>();

        for (int rank = 0; rank < topArtists.size(); rank++) {
            SpotifyArtist artist = topArtists.get(rank);
            int artistWeight = scoreByRank(rank, topArtists.size());

            if (artist.getGenres() != null) {
                for (String detailedGenre : artist.getGenres()) {
                    String broadGenre = normalizeToBroadGenre(detailedGenre);
                    genreScores.put(broadGenre, genreScores.getOrDefault(broadGenre, 0) + artistWeight);
                }
            }
        }

        if (genreScores.isEmpty()) {
            log.warn("No genres found in top artists");
            return "Unknown";
        }

        String topGenre = genreScores.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        log.info("Top genre identified: {}", topGenre);
        return topGenre;
    }

    /**
     * Calculates genre diversity using Shannon entropy.
     *
     * Algorithm:
     * - Score each genre based on artist weights (as in getTopGenre)
     * - Convert genre scores to probabilities: p(genre) = genreScore / totalGenreScore
     * - Calculate Shannon entropy: H = -sum(p * log(p))
     * - Normalize entropy to 0-100 scale
     * - Higher entropy = more genres = higher diversity
     *
     * @param topArtists top 20 long-term artists
     * @return integer score 0-100
     */
    public int calculateGenreDiversity(List<SpotifyArtist> topArtists) {
        if (topArtists == null || topArtists.isEmpty()) {
            log.warn("No top artists available for genre diversity calculation");
            return 0;
        }

        Map<String, Integer> genreScores = new HashMap<>();

        // Calculate genre scores
        for (int rank = 0; rank < topArtists.size(); rank++) {
            SpotifyArtist artist = topArtists.get(rank);
            int artistWeight = scoreByRank(rank, topArtists.size());

            if (artist.getGenres() != null) {
                for (String detailedGenre : artist.getGenres()) {
                    String broadGenre = normalizeToBroadGenre(detailedGenre);
                    genreScores.put(broadGenre, genreScores.getOrDefault(broadGenre, 0) + artistWeight);
                }
            }
        }

        if (genreScores.isEmpty()) {
            return 0;
        }

        // Calculate total score
        int totalScore = genreScores.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalScore == 0) {
            return 0;
        }

        // Calculate Shannon entropy
        double entropy = 0.0;
        for (int score : genreScores.values()) {
            double probability = (double) score / totalScore;
            if (probability > 0) {
                entropy -= probability * Math.log(probability) / Math.log(2); // log base 2
            }
        }

        // Normalize entropy to 0-100
        // Maximum entropy for n genres is log2(n)
        // We'll cap at log2(20) ≈ 4.32 for normalization purposes
        double maxEntropy = Math.log(genreScores.size()) / Math.log(2);
        double normalizedDiversity = (entropy / maxEntropy) * 100;
        int diversityScore = (int) Math.round(normalizedDiversity);
        diversityScore = Math.min(100, Math.max(0, diversityScore)); // Clamp to 0-100

        log.info("Genre diversity calculated: {}/100 (entropy: {}, genres: {})",
                diversityScore, entropy, genreScores.size());

        return diversityScore;
    }

    /**
     * Normalizes detailed Spotify genre tags into broader categories.
     *
     * Examples:
     * - rap, hip hop, trap, boom bap -> Hip-Hop
     * - dance pop, electropop, indie pop -> Pop
     * - rock, indie rock, alternative rock -> Rock
     */
    private String normalizeToBroadGenre(String detailedGenre) {
        if (detailedGenre == null || detailedGenre.isBlank()) {
            return "Other";
        }

        String normalized = detailedGenre.toLowerCase().trim();

        // Hip-Hop and Rap variants
        if (normalized.contains("hip hop") || normalized.contains("hip-hop") ||
                normalized.contains("rap") || normalized.contains("trap") ||
                normalized.contains("boom bap") || normalized.contains("grime")) {
            return "Hip-Hop";
        }

        // Pop variants
        if (normalized.contains("pop") && !normalized.contains("rock")) {
            return "Pop";
        }

        // Rock variants
        if (normalized.contains("rock") || normalized.contains("metal") ||
                normalized.contains("hardcore")) {
            return "Rock";
        }

        // Electronic variants
        if (normalized.contains("electronic") || normalized.contains("edm") ||
                normalized.contains("house") || normalized.contains("techno") ||
                normalized.contains("dubstep") || normalized.contains("synth")) {
            return "Electronic";
        }

        // Indie variants
        if (normalized.contains("indie")) {
            return "Indie";
        }

        // R&B and Soul
        if (normalized.contains("r&b") || normalized.contains("r b") ||
                normalized.contains("soul") || normalized.contains("funk")) {
            return "R&B/Soul";
        }

        // Country
        if (normalized.contains("country")) {
            return "Country";
        }

        // Jazz
        if (normalized.contains("jazz")) {
            return "Jazz";
        }

        // Classical
        if (normalized.contains("classical") || normalized.contains("orchestral")) {
            return "Classical";
        }

        // Reggae
        if (normalized.contains("reggae") || normalized.contains("dancehall")) {
            return "Reggae";
        }

        // Latin
        if (normalized.contains("latin") || normalized.contains("reggaeton")) {
            return "Latin";
        }

        // Ambient
        if (normalized.contains("ambient") || normalized.contains("chillwave")) {
            return "Ambient";
        }

        // Default: use first word capitalized or "Other"
        return "Other";
    }

    /**
     * Calculates a score based on rank.
     * rank 0 -> score = listSize
     * rank 1 -> score = listSize - 1
     */
    private int scoreByRank(int rank, int listSize) {
        return listSize - rank;
    }
}