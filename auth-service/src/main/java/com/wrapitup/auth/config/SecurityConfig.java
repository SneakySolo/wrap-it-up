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
     * 1. User accesses /auth/spotify → redirected to Spotify login
     * 2. Spotify redirects back to /auth/spotify/callback with authorization code
     * 3. Spring Security exchanges code for access token
     * 4. User is authenticated with OAuth2 token
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity in V1
                .authorizeHttpRequests(authz -> authz
                        // Allow health checks without authentication
                        .requestMatchers("/actuator/**", "/health").permitAll()
                        // All OAuth endpoints require authentication (except implicit redirect)
                        .requestMatchers("/auth/spotify").permitAll() // Login endpoint is public
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        // Configure OAuth2 login
                        .loginPage("/auth/spotify")
                        .defaultSuccessUrl("/auth/spotify/callback", true)
                        .failureUrl("/auth/error")
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                );

        return http.build();
    }

    /**
     * Configure CORS to allow requests from frontend (gateway).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",
                "http://127.0.0.1:8080",
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