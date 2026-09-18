package com.wrapitup.auth.controller;

import com.wrapitup.auth.service.SpotifyUserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SpotifyUserContextService spotifyUserContextService;

    /**
     * Get authenticated user's Spotify profile.
     * This endpoint requires authentication — Spring Security will redirect to Spotify if not authenticated.
     *
     * @param authorizedClient the OAuth2 authorized client
     * @return user profile containing Spotify account ID and details
     */
    @GetMapping("/me")
    public UserInfoResponse getUserInfo(
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient authorizedClient
    ) {
        if (authorizedClient == null) {
            log.error("User not authenticated");
            throw new IllegalStateException("User not authenticated");
        }

        String spotifyAccountId = spotifyUserContextService.extractSpotifyAccountId(authorizedClient);
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

        log.info("Fetching user info for account: {}", spotifyAccountId);

        return UserInfoResponse.builder()
                .spotifyAccountId(spotifyAccountId)
                .isAuthenticated(true)
                .tokenExpired(false)
                .build();
    }

    /**
     * Logout endpoint.
     * Spring Security handles the actual session invalidation.
     *
     * @return logout response
     */
    @GetMapping("/logout")
    public LogoutResponse logout() {
        log.info("Logout endpoint called");
        return LogoutResponse.builder()
                .message("Logged out successfully")
                .build();
    }
}