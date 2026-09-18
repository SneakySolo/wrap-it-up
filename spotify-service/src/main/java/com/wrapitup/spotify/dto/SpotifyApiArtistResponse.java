package com.wrapitup.spotify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spotify API response DTO for artist.
 * Wraps only the fields we need from Spotify's full artist response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifyApiArtistResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("genres")
    private List<String> genres;

    @JsonProperty("images")
    private List<ImageDto> images;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageDto {
        @JsonProperty("url")
        private String url;

        @JsonProperty("height")
        private Integer height;

        @JsonProperty("width")
        private Integer width;
    }
}