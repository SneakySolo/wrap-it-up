package com.wrapitup.wrap.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking the state of wrap generations in-memory.
 *
 * Lifecycle:
 * 1. PENDING   → Created when wrap generation is initiated
 * 2. COMPLETED → Set when analysis finishes and wrap is cached
 * 3. FAILED    → Set when an error occurs at any stage
 *
 * States auto-expire after 30 minutes.
 * This is a simple in-memory implementation suitable for Phase 6.
 *
 * Future: Move to Redis for distributed state management.
 */
@Slf4j
@Service
public class GenerationStateService {

    public enum GenerationStatus {
        PENDING, COMPLETED, FAILED
    }

    @Data
    @Builder
    public static class GenerationState {
        private String generationId;
        private String spotifyAccountId;
        private GenerationStatus status;
        private String errorCode;
        private String errorMessage;
        private int percentComplete;
        private Instant createdAt;
        private Instant completedAt;
    }

    private final ConcurrentHashMap<String, GenerationState> states;
    private final ScheduledExecutorService cleanupExecutor;
    private static final long EXPIRATION_MINUTES = 30;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;

    public GenerationStateService() {
        this.states = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "GenerationState-Cleanup");
            thread.setDaemon(true);
            return thread;
        });

        // Start background cleanup task
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredStates,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    /**
     * Track a new wrap generation as PENDING.
     *
     * @param generationId Unique generation ID (UUID)
     * @param spotifyAccountId Spotify user account ID
     */
    public void track(String generationId, String spotifyAccountId) {
        GenerationState state = GenerationState.builder()
                .generationId(generationId)
                .spotifyAccountId(spotifyAccountId)
                .status(GenerationStatus.PENDING)
                .percentComplete(0)
                .createdAt(Instant.now())
                .build();

        states.put(generationId, state);
        log.info("Tracking generation: id={}, accountId={}, status=PENDING", generationId, spotifyAccountId);
    }

    /**
     * Mark a generation as COMPLETED.
     *
     * @param generationId The generation ID
     */
    public void markCompleted(String generationId) {
        GenerationState state = states.get(generationId);
        if (state != null) {
            state.setStatus(GenerationStatus.COMPLETED);
            state.setPercentComplete(100);
            state.setCompletedAt(Instant.now());
            log.info("Generation completed: id={}, accountId={}", generationId, state.spotifyAccountId);
        } else {
            log.warn("Attempted to mark unknown generation as completed: id={}", generationId);
        }
    }

    /**
     * Mark a generation as FAILED.
     *
     * @param generationId The generation ID
     * @param errorCode Error code (e.g., SPOTIFY_RATE_LIMIT, ANALYSIS_ERROR)
     * @param errorMessage Human-readable error message
     */
    public void markFailed(String generationId, String errorCode, String errorMessage) {
        GenerationState state = states.get(generationId);
        if (state != null) {
            state.setStatus(GenerationStatus.FAILED);
            state.setErrorCode(errorCode);
            state.setErrorMessage(errorMessage);
            state.setCompletedAt(Instant.now());
            log.warn("Generation failed: id={}, accountId={}, errorCode={}, errorMessage={}",
                    generationId, state.spotifyAccountId, errorCode, errorMessage);
        } else {
            log.warn("Attempted to mark unknown generation as failed: id={}", generationId);
        }
    }

    /**
     * Update progress percentage (0-100).
     *
     * @param generationId The generation ID
     * @param percentComplete Progress percentage (0-100)
     */
    public void updateProgress(String generationId, int percentComplete) {
        GenerationState state = states.get(generationId);
        if (state != null && state.status == GenerationStatus.PENDING) {
            state.setPercentComplete(Math.min(99, Math.max(0, percentComplete)));
        }
    }

    /**
     * Get the current state of a generation.
     *
     * @param generationId The generation ID
     * @return GenerationState if found, null otherwise
     */
    public GenerationState getState(String generationId) {
        return states.get(generationId);
    }

    /**
     * Check if a generation exists (has been tracked).
     *
     * @param generationId The generation ID
     * @return true if exists, false otherwise
     */
    public boolean exists(String generationId) {
        return states.containsKey(generationId);
    }

    /**
     * Delete/forget a generation state.
     *
     * @param generationId The generation ID
     */
    public void delete(String generationId) {
        GenerationState removed = states.remove(generationId);
        if (removed != null) {
            log.debug("Deleted generation state: id={}", generationId);
        }
    }

    /**
     * Clean up expired generation states (older than 30 minutes).
     * This runs automatically on a schedule.
     */
    private void cleanupExpiredStates() {
        Instant expirationThreshold = Instant.now().minusSeconds(EXPIRATION_MINUTES * 60);
        int removed = 0;

        for (String generationId : states.keySet()) {
            GenerationState state = states.get(generationId);
            if (state != null && state.createdAt.isBefore(expirationThreshold)) {
                states.remove(generationId);
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Cleaned up {} expired generation states", removed);
        }
    }

    /**
     * Get the total number of tracked generations.
     * Useful for monitoring/debugging.
     *
     * @return Count of tracked generations
     */
    public int getTrackedCount() {
        return states.size();
    }

    /**
     * Shutdown the background cleanup executor.
     * Call this on application shutdown.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}