package com.wrapitup.spotify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spotify API response DTO for track.
 * Wraps only the fields we need from Spotify's full track response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifyApiTrackResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("duration_ms")
    private Long durationMs;

    @JsonProperty("artists")
    private List<SpotifyApiArtistResponse> artists;

    @JsonProperty("album")
    private AlbumDto album;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlbumDto {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("release_date")
        private String releaseDate;

        @JsonProperty("images")
        private List<SpotifyApiArtistResponse.ImageDto> images;
    }
}