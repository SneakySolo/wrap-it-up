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
public class SpotifyAlbum {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("releaseDate")
    private LocalDate releaseDate;

    @JsonProperty("artistId")
    private String artistId;

    @JsonProperty("artistName")
    private String artistName;
}