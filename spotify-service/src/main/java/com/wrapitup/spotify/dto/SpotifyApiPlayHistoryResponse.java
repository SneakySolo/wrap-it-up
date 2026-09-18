package com.wrapitup.spotify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spotify API response DTO for recently played item.
 * Contains track info and played_at timestamp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifyApiPlayHistoryResponse {

    @JsonProperty("track")
    private SpotifyApiTrackResponse track;

    @JsonProperty("played_at")
    private String playedAt;

    @JsonProperty("context")
    private ContextDto context;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContextDto {
        @JsonProperty("type")
        private String type;

        @JsonProperty("href")
        private String href;

        @JsonProperty("external_urls")
        private Object externalUrls;

        @JsonProperty("uri")
        private String uri;
    }
}