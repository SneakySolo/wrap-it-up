package com.wrapitup.spotify.service;

import com.wrapitup.common.domain.RecentlyPlayedItem;
import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import com.wrapitup.common.event.payload.SpotifySnapshotPayload;
import com.wrapitup.spotify.client.SpotifyApiClient;
import com.wrapitup.spotify.dto.SpotifyApiArtistResponse;
import com.wrapitup.spotify.dto.SpotifyApiPlayHistoryResponse;
import com.wrapitup.spotify.dto.SpotifyApiTrackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to orchestrate fetching a complete Spotify listening snapshot.
 * Coordinates:
 * 1. Fetching top artists (short, medium, long term)
 * 2. Fetching top tracks (long term)
 * 3. Fetching recently played tracks
 * 4. Normalizing all data into domain models
 * 5. Building the snapshot payload for Kafka
 *
 * Requires a valid Spotify OAuth access token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyListeningSnapshotService {

    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyDataNormalizer normalizer;

    /**
     * Fetch complete Spotify listening snapshot for a user.
     * This is called when a wrap generation is requested.
     *
     * @param accessToken Valid Spotify OAuth access token
     * @return Complete normalized snapshot payload
     */
    public SpotifySnapshotPayload fetchListeningSnapshot(String accessToken) {
        log.info("Fetching complete Spotify listening snapshot");

        try {
            // Fetch top artists in all three time ranges
            log.debug("Step 1: Fetching top artists (all time ranges)");
            List<SpotifyApiArtistResponse> shortTermArtists =
                    spotifyApiClient.getTopArtists(accessToken, "short_term");
            List<SpotifyApiArtistResponse> mediumTermArtists =
                    spotifyApiClient.getTopArtists(accessToken, "medium_term");
            List<SpotifyApiArtistResponse> longTermArtists =
                    spotifyApiClient.getTopArtists(accessToken, "long_term");

            // Fetch top tracks (long term, up to 50)
            log.debug("Step 2: Fetching top tracks (long-term)");
            List<SpotifyApiTrackResponse> topTracks =
                    spotifyApiClient.getTopTracks(accessToken);

            // Fetch recently played tracks (up to 50)
            log.debug("Step 3: Fetching recently played tracks");
            List<SpotifyApiPlayHistoryResponse> recentlyPlayed =
                    spotifyApiClient.getRecentlyPlayed(accessToken);

            // Normalize all data
            log.debug("Step 4: Normalizing all data to domain models");
            SpotifySnapshotPayload snapshot = SpotifySnapshotPayload.builder()
                    .topArtists(
                            SpotifySnapshotPayload.TopArtistsData.builder()
                                    .shortTerm(normalizer.normalizeArtists(shortTermArtists))
                                    .mediumTerm(normalizer.normalizeArtists(mediumTermArtists))
                                    .longTerm(normalizer.normalizeArtists(longTermArtists))
                                    .build()
                    )
                    .topTracks(
                            SpotifySnapshotPayload.TopTracksData.builder()
                                    .longTerm(normalizer.normalizeTracks(topTracks))
                                    .build()
                    )
                    .recentlyPlayed(normalizer.normalizePlayHistory(recentlyPlayed))
                    .build();

            log.info("Successfully fetched and normalized Spotify listening snapshot");
            return snapshot;

        } catch (Exception e) {
            log.error("Failed to fetch Spotify listening snapshot", e);
            throw new RuntimeException("Failed to fetch Spotify data", e);
        }
    }
}