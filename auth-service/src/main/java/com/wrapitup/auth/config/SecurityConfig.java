package com.wrapitup.auth.config;

import com.wrapitup.auth.service.TokenInfo;
import com.wrapitup.auth.service.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private TokenStore tokenStore;

    /**
     * Configure security filter chain for OAuth2 login and CORS.
     *
     * Flow:
     * 1. Unauthenticated user accesses /auth/me
     * 2. Spring Security redirects to Spotify
     * 3. User logs in and authorizes
     * 4. Spotify redirects back to /auth/spotify/callback?code=...&state=...
     * 5. Spring Security exchanges code for token
     * 6. Success handler is called
     * 7. User is redirected to the gateway's wrap-start flow
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Allow health endpoints without authentication
                        .requestMatchers("/actuator/**", "/health", "/auth/internal/**").permitAll()
                        // All other /auth requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/auth/spotify/callback"))
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(accessTokenResponseClient()))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService()))
                        .successHandler((request, response, authentication) -> {
                            try {
                                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                                String clientName = oauthToken.getAuthorizedClientRegistrationId();

                                // Get the authorized client
                                OAuth2AuthorizedClient client = authorizedClientRepository
                                        .loadAuthorizedClient(clientName, oauthToken, request);

                                if (client != null && client.getAccessToken() != null) {
                                    // Extract Spotify account ID from user profile
                                    String spotifyAccountId = oauthToken.getPrincipal()
                                            .getAttribute("id");

                                    if (spotifyAccountId != null) {
                                        TokenInfo tokenInfo = TokenInfo.builder()
                                                .accessToken(client.getAccessToken().getTokenValue())
                                                .refreshToken(client.getRefreshToken() != null
                                                        ? client.getRefreshToken().getTokenValue()
                                                        : null)
                                                .expiresAt(client.getAccessToken().getExpiresAt())
                                                .build();

                                        tokenStore.saveToken(spotifyAccountId, tokenInfo);
                                        log.info("✓ OAuth successful - Captured and stored token for account: {}", spotifyAccountId);
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Failed to capture token after OAuth", e);
                            }

                            // /auth/me is a JSON diagnostic endpoint. Sending the
                            // browser there leaves the user stuck on auth JSON and
                            // never starts wrap generation.
                            response.sendRedirect("/start");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                );

        return http.build();
    }

    /**
     * Token endpoint client with explicit connect/read timeouts.
     * <p>
     * Spring Security's default client has no timeout, so a broken network
     * path to https://accounts.spotify.com/api/token (e.g. a dead IPv6
     * route or a firewall silently dropping the connection) hangs for
     * 20-40s before failing with an opaque "invalid_token_response" error.
     * Bounding the request to a few seconds makes failures fast and the
     * underlying cause (timeout vs. a real Spotify-side rejection) obvious
     * in the logs.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(8_000);
        requestFactory.setReadTimeout(8_000);

        RestTemplate restTemplate = new RestTemplate(Arrays.asList(
                new FormHttpMessageConverter(),
                new OAuth2AccessTokenResponseHttpMessageConverter()));
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRestOperations(restTemplate);
        return client;
    }

    /**
     * UserInfo client (GET https://api.spotify.com/v1/me) with the same
     * timeout bound as the token client, wrapped with a few quick retries.
     * See {@link RetryingOAuth2UserService} for why the retry is needed.
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(8_000);
        requestFactory.setReadTimeout(8_000);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        delegate.setRestOperations(restTemplate);

        return new RetryingOAuth2UserService(delegate);
    }

    /**
     * Configure CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://localhost:8081",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}