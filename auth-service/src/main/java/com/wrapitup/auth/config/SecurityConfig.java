package com.wrapitup.auth.config;

import com.wrapitup.auth.service.TokenInfo;
import com.wrapitup.auth.service.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.beans.factory.annotation.Autowired;

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
     * 1. User visits protected endpoint (e.g., /auth/me)
     * 2. Spring Security redirects to Spotify authorization URL
     * 3. User logs in and authorizes
     * 4. Spotify redirects back to /auth/spotify/callback?code=...&state=...
     * 5. Spring Security exchanges code for access token
     * 6. User is authenticated
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Allow actuator endpoints without authentication
                        .requestMatchers("/actuator/**", "/health").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
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
                                            .getAttribute("id"); // Spotify's user ID field

                                    if (spotifyAccountId != null) {
                                        TokenInfo tokenInfo = TokenInfo.builder()
                                                .accessToken(client.getAccessToken().getTokenValue())
                                                .refreshToken(client.getRefreshToken() != null
                                                        ? client.getRefreshToken().getTokenValue()
                                                        : null)
                                                .expiresAt(client.getAccessToken().getExpiresAt())
                                                .build();

                                        tokenStore.saveToken(spotifyAccountId, tokenInfo);
                                        log.info("✓ Captured and stored token for account: {}", spotifyAccountId);
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Failed to capture token after OAuth", e);
                            }

                            // Redirect to home or dashboard
                            response.sendRedirect("/");
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
     * Configure CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:8081",
                "http://127.0.0.1:8081"
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