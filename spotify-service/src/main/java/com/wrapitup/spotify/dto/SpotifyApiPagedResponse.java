package com.wrapitup.spotify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic wrapper for Spotify API paginated responses.
 * Spotify returns paged results with items, next, previous, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifyApiPagedResponse<T> {

    @JsonProperty("items")
    private List<T> items;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("next")
    private String next;

    @JsonProperty("previous")
    private String previous;

    @JsonProperty("href")
    private String href;
}