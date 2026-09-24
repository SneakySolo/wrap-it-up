package com.wrapitup.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    /** Configure routes to backend services. */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**", "/oauth2/**", "/login/**")
                        .filters(f -> f
                                // OAuth sessions and redirect URIs belong to the public gateway host.
                                .preserveHostHeader())
                        .uri("http://localhost:8081"))

                .route("wrap-service", r -> r
                        .path("/wraps/**")
                        .uri("http://localhost:8084"))
                .build();
    }
}
