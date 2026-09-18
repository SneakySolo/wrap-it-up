package com.wrapitup.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SpotifyUserContextService Tests")
@ExtendWith(MockitoExtension.class)
class SpotifyUserContextServiceTest {

    private SpotifyUserContextService service;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    @BeforeEach
    void setUp() {
        service = new SpotifyUserContextService(webClientBuilder);
    }

    @Test
    @DisplayName("Should throw exception when authorized client is null")
    void testExtractAccountIdThrowsWhenClientIsNull() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.extractSpotifyAccountId(null);
        });
    }

    @Test
    @DisplayName("Should throw exception when access token is null")
    void testExtractAccountIdThrowsWhenTokenIsNull() {
        // Arrange
        when(authorizedClient.getAccessToken()).thenReturn(null);

        // Assert
        assertThrows(IllegalStateException.class, () -> {
            service.extractSpotifyAccountId(authorizedClient);
        });
    }

    @Test
    @DisplayName("Should detect expired token")
    void testIsTokenExpiredDetectsExpiry() {
        // Arrange
        Instant pastTime = Instant.now().minusSeconds(3600); // 1 hour ago
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getExpiresAt()).thenReturn(pastTime);

        // Act
        boolean isExpired = service.isTokenExpired(authorizedClient);

        // Assert
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should detect valid token")
    void testIsTokenExpiredDetectsValid() {
        // Arrange
        Instant futureTime = Instant.now().plusSeconds(3600); // 1 hour from now
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getExpiresAt()).thenReturn(futureTime);

        // Act
        boolean isExpired = service.isTokenExpired(authorizedClient);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should return true when authorized client is null")
    void testIsTokenExpiredWhenClientIsNull() {
        // Act & Assert
        assertTrue(service.isTokenExpired(null));
    }

    @Test
    @DisplayName("Should calculate expires in seconds")
    void testGetTokenExpiresIn() {
        // Arrange
        long secondsFromNow = 1800; // 30 minutes
        Instant futureTime = Instant.now().plusSeconds(secondsFromNow);
        when(accessToken.getExpiresAt()).thenReturn(futureTime);

        // Act
        long expiresIn = service.getTokenExpiresIn(accessToken);

        // Assert
        assertTrue(expiresIn > 0);
        assertTrue(expiresIn <= secondsFromNow);
    }

    @Test
    @DisplayName("Should return 0 when token is null")
    void testGetTokenExpiresInWhenTokenIsNull() {
        // Act
        long expiresIn = service.getTokenExpiresIn(null);

        // Assert
        assertEquals(0L, expiresIn);
    }

    @Test
    @DisplayName("Should return 0 when token has no expiration")
    void testGetTokenExpiresInWhenNoExpirationData() {
        // Arrange
        when(accessToken.getExpiresAt()).thenReturn(null);

        // Act
        long expiresIn = service.getTokenExpiresIn(accessToken);

        // Assert
        assertEquals(0L, expiresIn);
    }
}