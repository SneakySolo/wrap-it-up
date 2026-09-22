package com.wrapitup.auth.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class TokenInfo {
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;  // When the token expires

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}