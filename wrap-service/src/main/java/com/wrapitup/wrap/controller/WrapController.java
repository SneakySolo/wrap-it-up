package com.wrapitup.wrap.controller;

import com.wrapitup.common.dto.GenerateWrapResponse;
import com.wrapitup.wrap.service.WrapGenerationRequestedProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for Wrap generation.
 *
 * Phase 4 implements:
 * - POST /wraps → initiates async generation, returns generationId
 */
@Slf4j
@RestController
@RequestMapping("/wraps")
public class WrapController {

    private final WrapGenerationRequestedProducer producer;

    public WrapController(WrapGenerationRequestedProducer producer) {
        this.producer = producer;
    }

    /**
     * POST /wraps
     *
     * Initiates a new Wrap generation for the authenticated user.
     * Returns immediately with a generationId.
     *
     * The actual generation happens asynchronously via Kafka:
     * - wrap-service publishes wrap.generation.requested
     * - spotify-service processes it and publishes spotify.snapshot.created
     * - analysis-service processes it and publishes wrap.analysis.completed
     * - wrap-service consumes the result and caches it in Redis
     *
     * TODO: Extract spotifyAccountId from authenticated user context
     * For now, this is a placeholder.
     *
     * @return generation ID for tracking
     */
    @PostMapping
    public ResponseEntity<GenerateWrapResponse> generateWrap() {
        try {
            // TODO: Get from authentication context
            String spotifyAccountId = "placeholder-account-id";

            // Generate unique ID for this wrap generation
            String generationId = UUID.randomUUID().toString();

            // Publish event to Kafka
            producer.publishGenerationRequested(generationId, spotifyAccountId);

            log.info("Initiated wrap generation: generation={}", generationId);

            // Return immediately with generationId
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    GenerateWrapResponse.builder()
                            .generationId(generationId)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error generating wrap", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}