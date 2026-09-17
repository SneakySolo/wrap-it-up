package com.wrapitup.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for auth-service.
 * Allows public access to actuator endpoints; OAuth2 login for protected endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Allow public access to actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // Allow public access to auth endpoints (we'll add these in Phase 2)
                        .requestMatchers("/auth/spotify").permitAll()
                        .requestMatchers("/auth/spotify/callback").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                );
        return http.build();
    }
}