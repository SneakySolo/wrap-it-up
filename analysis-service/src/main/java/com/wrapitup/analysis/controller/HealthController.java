package com.wrapitup.analysis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Basic health and info endpoints for analysis-service.
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.builder()
                .status("UP")
                .service("analysis-service")
                .build());
    }

    @GetMapping("/info")
    public ResponseEntity<InfoResponse> info() {
        return ResponseEntity.ok(InfoResponse.builder()
                .name("Analysis Service")
                .version("1.0.0")
                .description("Wrap analytics and personality engine")
                .build());
    }

    public static class HealthResponse {
        public String status;
        public String service;

        public HealthResponse(String status, String service) {
            this.status = status;
            this.service = service;
        }

        public static HealthResponseBuilder builder() {
            return new HealthResponseBuilder();
        }

        public static class HealthResponseBuilder {
            private String status;
            private String service;

            public HealthResponseBuilder status(String status) {
                this.status = status;
                return this;
            }

            public HealthResponseBuilder service(String service) {
                this.service = service;
                return this;
            }

            public HealthResponse build() {
                return new HealthResponse(status, service);
            }
        }
    }

    public static class InfoResponse {
        public String name;
        public String version;
        public String description;

        public InfoResponse(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }

        public static InfoResponseBuilder builder() {
            return new InfoResponseBuilder();
        }

        public static class InfoResponseBuilder {
            private String name;
            private String version;
            private String description;

            public InfoResponseBuilder name(String name) {
                this.name = name;
                return this;
            }

            public InfoResponseBuilder version(String version) {
                this.version = version;
                return this;
            }

            public InfoResponseBuilder description(String description) {
                this.description = description;
                return this;
            }

            public InfoResponse build() {
                return new InfoResponse(name, version, description);
            }
        }
    }
}