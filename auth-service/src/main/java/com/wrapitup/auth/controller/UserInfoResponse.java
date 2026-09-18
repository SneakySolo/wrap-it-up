package com.wrapitup.auth.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    @JsonProperty("spotify_account_id")
    private String spotifyAccountId;

    @JsonProperty("is_authenticated")
    private boolean isAuthenticated;

    @JsonProperty("token_expired")
    private boolean tokenExpired;
}