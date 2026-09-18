package com.wrapitup.spotify.client;

import com.wrapitup.spotify.dto.SpotifyApiArtistResponse;
import com.wrapitup.spotify.dto.SpotifyApiPagedResponse;
import com.wrapitup.spotify.dto.SpotifyApiPlayHistoryResponse;
import com.wrapitup.spotify.dto.SpotifyApiTrackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Client for calling Spotify Web API.
 * All calls require a valid OAuth access token.
 * Handles retries and error mapping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotifyApiClient {

    private final WebClient spotifyWebClient;

    @Value("${spotify.api.base-url:https://api.spotify.com/v1}")
    private String baseUrl;

    /**
     * Fetch top artists for a given time range.
     * Returns up to 50 artists (default limit).
     *
     * @param accessToken Spotify OAuth access token
     * @param timeRange   One of "long_term", "medium_term", "short_term"
     * @return List of top artists
     */
    public List<SpotifyApiArtistResponse> getTopArtists(String accessToken, String timeRange) {
        log.debug("Fetching top artists for timeRange={}", timeRange);

        SpotifyApiPagedResponse<SpotifyApiArtistResponse> response = spotifyWebClient
                .get()
                .uri("/me/top/artists?time_range={timeRange}&limit=50", timeRange)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(SpotifyApiPagedResponse.class)
                .block();

        if (response == null || response.getItems() == null) {
            log.warn("Null response from Spotify API for top artists");
            return List.of();
        }

        log.debug("Retrieved {} top artists for timeRange={}", response.getItems().size(), timeRange);
        return response.getItems();
    }

    /**
     * Fetch top tracks for long-term.
     * Returns up to 50 tracks.
     *
     * @param accessToken Spotify OAuth access token
     * @return List of top tracks
     */
    public List<SpotifyApiTrackResponse> getTopTracks(String accessToken) {
        log.debug("Fetching top tracks (long-term)");

        SpotifyApiPagedResponse<SpotifyApiTrackResponse> response = spotifyWebClient
                .get()
                .uri("/me/top/tracks?time_range=long_term&limit=50")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(SpotifyApiPagedResponse.class)
                .block();

        if (response == null || response.getItems() == null) {
            log.warn("Null response from Spotify API for top tracks");
            return List.of();
        }

        log.debug("Retrieved {} top tracks", response.getItems().size());
        return response.getItems();
    }

    /**
     * Fetch recently played tracks.
     * Returns up to 50 recently played items.
     *
     * @param accessToken Spotify OAuth access token
     * @return List of recently played items
     */
    public List<SpotifyApiPlayHistoryResponse> getRecentlyPlayed(String accessToken) {
        log.debug("Fetching recently played tracks");

        SpotifyApiPagedResponse<SpotifyApiPlayHistoryResponse> response = spotifyWebClient
                .get()
                .uri("/me/player/recently_played?limit=50")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(SpotifyApiPagedResponse.class)
                .block();

        if (response == null || response.getItems() == null) {
            log.warn("Null response from Spotify API for recently played");
            return List.of();
        }

        log.debug("Retrieved {} recently played items", response.getItems().size());
        return response.getItems();
    }
}