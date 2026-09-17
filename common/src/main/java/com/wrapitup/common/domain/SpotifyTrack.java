package com.wrapitup.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifyTrack {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("durationMs")
    private int durationMs;

    @JsonProperty("artistId")
    private String artistId;

    @JsonProperty("artistName")
    private String artistName;

    @JsonProperty("albumId")
    private String albumId;

    @JsonProperty("albumName")
    private String albumName;

    @JsonProperty("albumReleaseDate")
    private LocalDate albumReleaseDate;

    @JsonProperty("imageUrl")
    private String imageUrl;
}