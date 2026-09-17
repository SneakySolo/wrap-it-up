package com.wrapitup.common.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wrapitup.common.domain.RecentlyPlayedItem;
import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload for spotify.snapshot.created event.
 * Contains normalized Spotify data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifySnapshotPayload {

    @JsonProperty("topArtists")
    private TopArtistsData topArtists;

    @JsonProperty("topTracks")
    private TopTracksData topTracks;

    @JsonProperty("recentlyPlayed")
    private List<RecentlyPlayedItem> recentlyPlayed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopArtistsData {

        @JsonProperty("shortTerm")
        private List<SpotifyArtist> shortTerm;

        @JsonProperty("mediumTerm")
        private List<SpotifyArtist> mediumTerm;

        @JsonProperty("longTerm")
        private List<SpotifyArtist> longTerm;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopTracksData {

        @JsonProperty("longTerm")
        private List<SpotifyTrack> longTerm;
    }
}