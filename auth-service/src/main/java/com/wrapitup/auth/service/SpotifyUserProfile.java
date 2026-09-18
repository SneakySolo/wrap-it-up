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

    /**
     * Spotify user ID (unique identifier).
     * This is the stable identifier we should use to track the user.
     */
    private String id;

    /**
     * User's display name (can be null).
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * User's email address (requires scope).
     */
    private String email;

    /**
     * User's external URLs.
     */
    @JsonProperty("external_urls")
    private ExternalUrls externalUrls;

    /**
     * User's country (if available).
     */
    private String country;

    /**
     * User's subscription product (free, premium).
     */
    private String product;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUrls {
        private String spotify;
    }
}