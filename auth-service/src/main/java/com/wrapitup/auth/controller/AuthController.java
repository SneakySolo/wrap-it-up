package com.wrapitup.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/spotify")
@RequiredArgsConstructor
public class AuthController {

    /**
     * Login endpoint: redirects to Spotify OAuth consent screen.
     * Spring Security handles the redirect automatically.
     */
    @GetMapping
    public String login() {
        log.info("Login endpoint called - Spring Security will redirect to Spotify");
        return "Redirecting to Spotify login...";
    }

    /**
     * OAuth2 callback endpoint.
     * Spring Security processes the authorization code and exchanges it for access token.
     * This endpoint receives the authenticated OAuth2 client.
     *
     * @param authorizedClient the OAuth2 authorized client with access token
     * @return response containing Spotify account ID and access token info
     */
    @GetMapping("/callback")
    public OAuthCallbackResponse callback(
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient authorizedClient
    ) {
        if (authorizedClient == null) {
            log.error("OAuth2 authorization failed: authorized client is null");
            throw new IllegalStateException("OAuth2 authorization failed");
        }

        String spotifyAccountId = extractSpotifyAccountId(authorizedClient);
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

        log.info("User authenticated successfully. Spotify Account ID: {}", spotifyAccountId);

        return OAuthCallbackResponse.builder()
                .spotifyAccountId(spotifyAccountId)
                .accessToken(accessToken.getTokenValue())
                .tokenType(accessToken.getTokenType().getValue())
                .expiresIn(calculateExpiresIn(accessToken))
                .message("Authentication successful")
                .build();
    }

    /**
     * Extract Spotify account ID (user_id) from the authorized client.
     * The user_id is obtained from Spotify's /me endpoint (user-info-uri).
     *
     * Note: This is a simplified implementation. In a production system,
     * you might want to fetch user details from Spotify API and cache them.
     */
    private String extractSpotifyAccountId(OAuth2AuthorizedClient authorizedClient) {
        // In a real scenario, you would extract this from the Principal or
        // make an API call to Spotify /me endpoint to get the user_id
        // For now, we'll use the clientRegistrationId as identifier
        String clientRegistrationId = authorizedClient.getClientRegistration().getRegistrationId();
        String principalName = authorizedClient.getPrincipalName();

        log.debug("Client Registration ID: {}, Principal Name: {}", clientRegistrationId, principalName);

        // TODO: Make actual call to Spotify /me endpoint to retrieve user_id
        // This is a placeholder - actual implementation should fetch from Spotify
        return principalName != null ? principalName : "unknown";
    }

    /**
     * Calculate token expiration time in seconds from now.
     */
    private long calculateExpiresIn(OAuth2AccessToken token) {
        if (token.getExpiresAt() == null) {
            return 0L;
        }
        return (token.getExpiresAt().getEpochSecond() - System.currentTimeMillis() / 1000);
    }

    @GetMapping("/me")
    public UserInfoResponse getUserInfo(
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient authorizedClient
    ) {
        if (authorizedClient == null) {
            log.error("User not authenticated");
            throw new IllegalStateException("User not authenticated");
        }

        String spotifyAccountId = extractSpotifyAccountId(authorizedClient);
        log.info("Fetching user info for account: {}", spotifyAccountId);

        return UserInfoResponse.builder()
                .spotifyAccountId(spotifyAccountId)
                .isAuthenticated(true)
                .tokenExpired(false)
                .build();
    }

    @GetMapping("/logout")
    public LogoutResponse logout() {
        log.info("Logout endpoint called");
        // Note: Actual logout is handled by Spring Security session management
        return LogoutResponse.builder()
                .message("Logged out successfully")
                .build();
    }
}