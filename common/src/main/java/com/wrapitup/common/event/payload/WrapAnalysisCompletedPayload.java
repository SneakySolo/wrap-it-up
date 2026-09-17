package com.wrapitup.common.event.payload;

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
 * Payload for wrap.analysis.completed event.
 * Contains all analysis results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrapAnalysisCompletedPayload {

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
    private PersonalityResult personality;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonalityResult {

        @JsonProperty("category")
        private String category;

        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;
    }
}