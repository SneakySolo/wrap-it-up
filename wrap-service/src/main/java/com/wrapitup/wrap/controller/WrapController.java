package com.wrapitup.wrap.controller;

import com.wrapitup.common.dto.GenerateWrapResponse;
import com.wrapitup.common.dto.WrapResponse;
import com.wrapitup.common.dto.WrapStatusResponse;
import com.wrapitup.wrap.cache.WrapCacheService;
import com.wrapitup.wrap.service.GenerationStateService;
import com.wrapitup.wrap.service.GenerationStateService.GenerationState;
import com.wrapitup.wrap.service.WrapGenerationRequestedProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for Wrap generation lifecycle.
 *
 * Phase 4 implements:
 * - POST /wraps → initiates async generation, returns generationId
 *
 * Phase 6 implements:
 * - GET /wraps/{generationId}/status → poll generation progress
 * - GET /wraps/{generationId} → retrieve completed wrap (or error)
 *
 * Endpoints support:
 * - Full async workflow without blocking
 * - Status polling with progress tracking
 * - Error details when generation fails
 * - Caching to prevent redundant processing
 */
@Slf4j
@RestController
@RequestMapping("/wraps")
public class WrapController {

    private final WrapGenerationRequestedProducer producer;
    private final GenerationStateService stateService;
    private final WrapCacheService cacheService;

    public WrapController(
            WrapGenerationRequestedProducer producer,
            GenerationStateService stateService,
            WrapCacheService cacheService
    ) {
        this.producer = producer;
        this.stateService = stateService;
        this.cacheService = cacheService;
    }

    /**
     * POST /wraps
     *
     * Initiates a new Wrap generation for the authenticated user.
     * Returns immediately with a generationId (202 Accepted).
     *
     * The actual generation happens asynchronously via Kafka:
     * - wrap-service publishes wrap.generation.requested
     * - spotify-service processes it and publishes spotify.snapshot.created
     * - analysis-service processes it and publishes wrap.analysis.completed
     * - wrap-service consumes the result and caches it in Redis
     *
     * Client should poll GET /wraps/{generationId}/status to track progress.
     *
     * @return generation ID for tracking
     */
    @PostMapping
    public ResponseEntity<GenerateWrapResponse> generateWrap() {
        try {
            // TODO: Extract spotifyAccountId from authenticated user context
            String spotifyAccountId = "placeholder-account-id";

            // Generate unique ID for this wrap generation
            String generationId = UUID.randomUUID().toString();

            // Track generation state as PENDING
            stateService.track(generationId, spotifyAccountId);

            // Publish event to Kafka to start the pipeline
            producer.publishGenerationRequested(generationId, spotifyAccountId);

            log.info("Initiated wrap generation: generation={}, accountId={}", generationId, spotifyAccountId);

            // Return immediately with generationId
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    GenerateWrapResponse.builder()
                            .generationId(generationId)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error initiating wrap generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /wraps/{generationId}/status
     *
     * Poll the status of a wrap generation.
     * Returns the current state and progress information.
     *
     * Response statuses:
     * - 200 OK: Generation tracked, status in response body
     * - 404 Not Found: Generation ID unknown (not yet initiated or expired)
     *
     * Status progression:
     * - PENDING (0-99% complete) → still processing
     * - COMPLETED (100%) → wrap ready at GET /wraps/{generationId}
     * - FAILED (100%) → error details in response
     *
     * @param generationId Unique generation ID from POST /wraps
     * @return Current generation status and progress
     */
    @GetMapping("/{generationId}/status")
    public ResponseEntity<WrapStatusResponse> getGenerationStatus(
            @PathVariable String generationId
    ) {
        try {
            GenerationState state = stateService.getState(generationId);

            if (state == null) {
                log.warn("Status queried for unknown generation: id={}", generationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String statusMessage = state.getStatus() == GenerationStateService.GenerationStatus.FAILED
                    ? "Error: " + state.getErrorMessage()
                    : buildStatusMessage(state);

            WrapStatusResponse response = WrapStatusResponse.builder()
                    .generationId(generationId)
                    .status(state.getStatus().toString())
                    .message(statusMessage)
                    .build();

            log.debug("Status query: generation={}, status={}, progress={}%",
                    generationId, state.getStatus(), state.getPercentComplete());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving generation status: id={}", generationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /wraps/{generationId}
     *
     * Retrieve a completed Wrap result.
     * Can only be called after generation reaches COMPLETED status.
     *
     * Response statuses:
     * - 200 OK: Wrap ready, full result in response body
     * - 202 Accepted: Wrap still generating (PENDING), client should retry later
     * - 409 Conflict: Generation FAILED, error details in response
     * - 404 Not Found: Generation ID unknown (not yet initiated or expired)
     *
     * The wrap is cached in Redis for 15 minutes, so repeated calls for the same
     * user within that window are served from cache without Kafka reprocessing.
     *
     * @param generationId Unique generation ID from POST /wraps
     * @return Complete WrapResponse if COMPLETED, error/status otherwise
     */
    @GetMapping("/{generationId}")
    public ResponseEntity<?> getWrap(
            @PathVariable String generationId
    ) {
        try {
            GenerationState state = stateService.getState(generationId);

            if (state == null) {
                log.warn("Wrap queried for unknown generation: id={}", generationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Still processing
            if (state.getStatus() == GenerationStateService.GenerationStatus.PENDING) {
                log.debug("Wrap requested while still PENDING: generation={}, progress={}%",
                        generationId, state.getPercentComplete());
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(WrapStatusResponse.builder()
                                .generationId(generationId)
                                .status("PENDING")
                                .message("Wrap generation in progress. Retry in a few seconds.")
                                .build());
            }

            // Failed
            if (state.getStatus() == GenerationStateService.GenerationStatus.FAILED) {
                log.warn("Wrap requested for FAILED generation: generation={}, error={}",
                        generationId, state.getErrorCode());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(WrapStatusResponse.builder()
                                .generationId(generationId)
                                .status("FAILED")
                                .message("Error: " + state.getErrorMessage())
                                .build());
            }

            // Completed - retrieve from cache or return cached result
            WrapResponse wrap = cacheService.get(state.getSpotifyAccountId());
            if (wrap != null) {
                log.info("Wrap retrieved from cache: generation={}, accountId={}",
                        generationId, state.getSpotifyAccountId());
                return ResponseEntity.ok(wrap);
            } else {
                // This shouldn't happen in normal flow (cache set by consumer)
                // but handle gracefully
                log.warn("Wrap marked COMPLETED but not in cache: generation={}, accountId={}",
                        generationId, state.getSpotifyAccountId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

        } catch (Exception e) {
            log.error("Error retrieving wrap: id={}", generationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Build a human-readable status message based on generation state.
     *
     * @param state Current generation state
     * @return Status message
     */
    private String buildStatusMessage(GenerationState state) {
        switch (state.getStatus()) {
            case PENDING:
                if (state.getPercentComplete() < 33) {
                    return "Fetching your Spotify data...";
                } else if (state.getPercentComplete() < 66) {
                    return "Analyzing your listening habits...";
                } else {
                    return "Computing your personality profile...";
                }
            case COMPLETED:
                return "Your wrap is ready! Retrieve it at GET /wraps/" + state.getGenerationId();
            case FAILED:
                return "Wrap generation failed: " + state.getErrorMessage();
            default:
                return "Status unknown";
        }
    }
}