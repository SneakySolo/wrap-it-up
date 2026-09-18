package com.wrapitup.spotify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient to communicate with Spotify API.
 */
@Configuration
public class WebClientConfig {

    @Value("${spotify.api.base-url:https://api.spotify.com/v1}")
    private String spotifyBaseUrl;

    /**
     * Create a WebClient preconfigured for Spotify API calls.
     */
    @Bean
    public WebClient spotifyWebClient() {
        return WebClient.builder()
                .baseUrl(spotifyBaseUrl)
                .build();
    }
}