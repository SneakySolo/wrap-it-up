package com.wrapitup.auth.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenStore {
    private final ConcurrentHashMap<String, TokenInfo> store = new ConcurrentHashMap<>();

    public void saveToken(String spotifyAccountId, TokenInfo tokenInfo) {
        store.put(spotifyAccountId, tokenInfo);
    }

    public TokenInfo getToken(String spotifyAccountId) {
        return store.get(spotifyAccountId);
    }

    public boolean hasToken(String spotifyAccountId) {
        return store.containsKey(spotifyAccountId);
    }
}