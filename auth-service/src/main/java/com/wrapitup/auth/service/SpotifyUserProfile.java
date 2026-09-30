package com.wrapitup.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spotify user profile from /me endpoint.
 * Maps to Spotify's user profile response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyUserProfile {

    private String id; // Spotify user ID (unique identifier)

    @JsonProperty("display_name")
    private String displayName; // User's display name (can be null)

    private String email; // User's email address (requires scope)

    @JsonProperty("external_urls")
    private ExternalUrls externalUrls; // User's external URLs

    private String country; // User's country

    private String product; // User's subscription product (free or premium)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUrls {
        private String spotify;
    }
}