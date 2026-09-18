package com.wrapitup.auth.config;

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

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

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
                        // Explicitly set the authorization endpoint
                        .authorizationEndpoint(authEndpoint ->
                                authEndpoint
                                        .baseUri("/oauth2/authorize")
                        )
                        // Explicitly set the redirect endpoint (must match application.yml redirect-uri)
                        .redirectionEndpoint(redirectEndpoint ->
                                redirectEndpoint
                                        .baseUri("/auth/spotify/callback")
                        )
                        // After successful authentication, redirect to /auth/me
                        .defaultSuccessUrl("/auth/me", true)
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