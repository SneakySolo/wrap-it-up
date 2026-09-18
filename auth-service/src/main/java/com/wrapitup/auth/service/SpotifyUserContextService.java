package com.wrapitup.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for managing Spotify user context and extracting user information.
 * This service is responsible for:
 * - Fetching Spotify user profile (ID, name, etc.)
 * - Managing OAuth2 access tokens
 * - Handling token refresh
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyUserContextService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Fetch Spotify user profile from the /me endpoint.
     * This extracts the user_id (Spotify Account ID) from the API response.
     *
     * @param accessToken OAuth2 access token from Spotify
     * @return Spotify user profile as DTO
     */
    public Mono<SpotifyUserProfile> fetchUserProfile(String accessToken) {
        return webClientBuilder.build()
                .get()
                .uri("https://api.spotify.com/v1/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(SpotifyUserProfile.class)
                .doOnSuccess(profile -> log.info("Fetched Spotify user: {} (ID: {})", profile.getDisplayName(), profile.getId()))
                .doOnError(ex -> log.error("Failed to fetch Spotify user profile", ex));
    }

    /**
     * Extract and validate Spotify user ID from authorized client.
     * If user profile is not yet cached, this will fetch it from Spotify API.
     *
     * @param authorizedClient OAuth2 authorized client with access token
     * @return Spotify account ID (user_id)
     */
    public String extractSpotifyAccountId(OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null) {
            throw new IllegalArgumentException("Authorized client cannot be null");
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        if (accessToken == null || accessToken.getTokenValue() == null) {
            throw new IllegalStateException("No access token available");
        }

        // Fetch user profile synchronously (blocking) for simplicity
        // In production, you might want to make this async or cache results
        SpotifyUserProfile profile = fetchUserProfile(accessToken.getTokenValue())
                .block(); // Block for sync response

        if (profile == null || profile.getId() == null) {
            log.error("Failed to extract Spotify account ID from profile");
            throw new RuntimeException("Could not retrieve Spotify account ID");
        }

        log.debug("Extracted Spotify Account ID: {}", profile.getId());
        return profile.getId();
    }

    /**
     * Check if access token is expired.
     *
     * @param authorizedClient OAuth2 authorized client
     * @return true if token is expired
     */
    public boolean isTokenExpired(OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return true;
        }

        OAuth2AccessToken token = authorizedClient.getAccessToken();
        if (token.getExpiresAt() == null) {
            return false; // No expiration info, assume valid
        }

        boolean expired = token.getExpiresAt().isBefore(java.time.Instant.now());
        log.debug("Token expiration check: expired={}", expired);
        return expired;
    }

    /**
     * Get token expiration time in seconds from now.
     */
    public long getTokenExpiresIn(OAuth2AccessToken token) {
        if (token == null || token.getExpiresAt() == null) {
            return 0L;
        }
        long expiresIn = (token.getExpiresAt().getEpochSecond() - System.currentTimeMillis() / 1000);
        return Math.max(0, expiresIn);
    }
}