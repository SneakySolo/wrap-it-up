package com.wrapitup.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wrapitup.common.domain.SpotifyAlbum;
import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for GET /wraps/{generationId} endpoint.
 * Contains the complete wrap with all analysis results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrapResponse {
    @JsonProperty("generationId")
    private String generationId;

    @JsonProperty("recentListeningMinutes")
    private int recentListeningMinutes;

    @JsonProperty("topArtist")
    private SpotifyArtist topArtist;

    @JsonProperty("topTracks")
    private List<SpotifyTrack> topTracks;

    @JsonProperty("topAlbum")
    private SpotifyAlbum topAlbum;

    @JsonProperty("topGenre")
    private String topGenre;

    @JsonProperty("risingStar")
    private SpotifyArtist risingStar;

    @JsonProperty("artistLoyalty")
    private int artistLoyalty;

    @JsonProperty("genreDiversity")
    private int genreDiversity;

    @JsonProperty("familiarityRatio")
    private double familiarityRatio;

    @JsonProperty("discoveryRatio")
    private double discoveryRatio;

    @JsonProperty("personality")
    private PersonalityDto personality;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonalityDto {
        @JsonProperty("category")
        private String category;

        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;
    }
}