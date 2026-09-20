package com.wrapitup.analysis.analyzer;

import com.wrapitup.common.domain.SpotifyAlbum;
import com.wrapitup.common.domain.SpotifyTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes listening patterns to identify the most-listened album.
 *
 * Algorithm:
 * - For each of the top 50 tracks, assign a rank weight: rank 1 -> 50, rank 2 -> 49, ..., rank 50 -> 1
 * - Group tracks by album ID
 * - For each album, sum the rank weights of all tracks in that album
 * - The album with the highest score is the most-listened album
 * - Album concentration = count of tracks from the most-listened album / 50
 */
@Slf4j
@Component
public class AlbumAnalyzer {

    /**
     * Analyzes top 50 tracks to determine the most-listened album and album concentration.
     *
     * @param topTracks the top 50 long-term tracks
     * @return a map containing:
     *         - "mostListenedAlbum": SpotifyAlbum object
     *         - "albumConcentration": Double (0.0 to 1.0)
     */
    public Map<String, Object> analyzeAlbums(List<SpotifyTrack> topTracks) {
        if (topTracks == null || topTracks.isEmpty()) {
            log.warn("No top tracks available for album analysis");
            return Map.of(
                    "mostListenedAlbum", new SpotifyAlbum(),
                    "albumConcentration", 0.0
            );
        }

        // Group tracks by album ID and calculate scores
        Map<String, AlbumScore> albumScores = new HashMap<>();

        for (int rank = 0; rank < topTracks.size(); rank++) {
            SpotifyTrack track = topTracks.get(rank);
            int rankWeight = (topTracks.size() - rank); // rank 0 -> 50, rank 1 -> 49, etc.

            String albumId = track.getAlbumId();
            albumScores.putIfAbsent(albumId, new AlbumScore(track));
            albumScores.get(albumId).addTrackWeight(rankWeight);
        }

        // Find album with highest score
        AlbumScore mostListenedAlbumScore = albumScores.values().stream()
                .max(Comparator.comparingInt(AlbumScore::getScore))
                .orElse(new AlbumScore(new SpotifyTrack()));

        // Calculate album concentration
        double albumConcentration = mostListenedAlbumScore.getTrackCount() / (double) topTracks.size();

        log.info("Album analysis: {} tracks from {}, concentration: {}",
                mostListenedAlbumScore.getTrackCount(),
                mostListenedAlbumScore.getAlbumName(),
                albumConcentration);

        // Build album object from first track of most-listened album
        SpotifyAlbum mostListenedAlbum = buildAlbumFromTrack(mostListenedAlbumScore.getSampleTrack());

        return Map.of(
                "mostListenedAlbum", mostListenedAlbum,
                "albumConcentration", albumConcentration
        );
    }

    private SpotifyAlbum buildAlbumFromTrack(SpotifyTrack track) {
        return SpotifyAlbum.builder()
                .id(track.getAlbumId())
                .name(track.getAlbumName())
                .releaseDate(track.getAlbumReleaseDate())
                .imageUrl(track.getImageUrl())
                .artistId(track.getArtistId())
                .artistName(track.getArtistName())
                .build();
    }

    /**
     * Internal helper class to track album scores.
     */
    private static class AlbumScore {
        private final String albumId;
        private final String albumName;
        private final SpotifyTrack sampleTrack;
        private int score;
        private int trackCount;

        AlbumScore(SpotifyTrack sampleTrack) {
            this.sampleTrack = sampleTrack;
            this.albumId = sampleTrack.getAlbumId();
            this.albumName = sampleTrack.getAlbumName();
            this.score = 0;
            this.trackCount = 0;
        }

        void addTrackWeight(int weight) {
            this.score += weight;
            this.trackCount++;
        }

        int getScore() {
            return score;
        }

        int getTrackCount() {
            return trackCount;
        }

        String getAlbumName() {
            return albumName;
        }

        SpotifyTrack getSampleTrack() {
            return sampleTrack;
        }
    }
}