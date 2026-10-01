package com.wrapitup.auth.controller;

import com.wrapitup.auth.service.SpotifyUserContextService;
import com.wrapitup.auth.service.TokenInfo;
import com.wrapitup.auth.service.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private TokenStore tokenStore;

    @Value("${auth.internal-service-token}")
    private String internalServiceToken;

    private final SpotifyUserContextService spotifyUserContextService;

    /**
     * frontend calls this after successful OAuth Login
     * request comes in with OAuth2 cookie
     * then Spring Security intercepts that
     * then it extracts OAuth2AuthorizedClient from cookie/session
     * then it calls AuthController.getUserInfo()
     * then it extracts Spotify account ID via SpotifyUserContextService
     * returns a JSON
     */
    // we use this to confirms the user is logged in and gets their Spotify ID.
    @GetMapping("/me")
    public UserInfoResponse getUserInfo(
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null) {
            log.error("User not authenticated");
            throw new IllegalStateException("User not authenticated");
        }

        String spotifyAccountId = spotifyUserContextService.extractSpotifyAccountId(authorizedClient);
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        log.info("Fetching Spotify user info for account: {}", spotifyAccountId);

        return UserInfoResponse.builder()
                .spotifyAccountId(spotifyAccountId)
                .isAuthenticated(true)
                .tokenExpired(false)
                .build();
    }

    /**
     * Other microservices (spotify-service, analysis-service, etc.) calls this
     * and each call requires internal service token in header
     * Also the X-Internal-Service-Token header acts as a password. Only services with this secret can fetch tokens.
     */

    /**
     * say spotify-service calls: GET http://auth-service:8081/auth/internal/tokens/user123456
     * along with Header: X-Internal-Service-Token: {INTERNAL_SERVICE_TOKEN}
     *
     * then this checks if provided token matches configured internal token
     * if invalid → 401 Unauthorized (rejected)
     * if valid, looks up token in TokenStore.getToken(spotifyAccountId)
     * if not found → 404 Not Found
     * if found but expired → 401 Unauthorized
     * if found and valid → 200 OK with a JSON
     */
    @GetMapping("/internal/tokens/{spotifyAccountId}")
    public ResponseEntity<TokenResponse> getTokenForAccount(
            @PathVariable("spotifyAccountId") String spotifyAccountId,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String providedToken) {
        if (!internalServiceToken.equals(providedToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenInfo tokenInfo = tokenStore.getToken(spotifyAccountId);
        if (tokenInfo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (tokenInfo.isExpired()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(tokenInfo.getAccessToken())
                .build());
    }

    @GetMapping("/logout")
    public LogoutResponse logout() {
        log.info("Logout endpoint called");
        return LogoutResponse.builder().message("Logged out successfully").build();
    }
}
