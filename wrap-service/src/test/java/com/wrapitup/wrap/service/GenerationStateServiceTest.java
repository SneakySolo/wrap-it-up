package com.wrapitup.wrap.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GenerationStateServiceTest {

    private GenerationStateService service;

    @BeforeEach
    void setUp() {
        service = new GenerationStateService();
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void testTrack_CreatesGenerationWithPendingStatus() {
        String generationId = "gen-123";
        String accountId = "user-456";

        service.track(generationId, accountId);
        GenerationStateService.GenerationState state = service.getState(generationId);

        assertNotNull(state);
        assertEquals(generationId, state.getGenerationId());
        assertEquals(accountId, state.getSpotifyAccountId());
        assertEquals(GenerationStateService.GenerationStatus.PENDING, state.getStatus());
        assertEquals(0, state.getPercentComplete());
        assertNotNull(state.getCreatedAt());
    }

    @Test
    void testMarkCompleted_TransitionsToCompleted() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");

        service.markCompleted(generationId);
        GenerationStateService.GenerationState state = service.getState(generationId);

        assertEquals(GenerationStateService.GenerationStatus.COMPLETED, state.getStatus());
        assertEquals(100, state.getPercentComplete());
        assertNotNull(state.getCompletedAt());
    }

    @Test
    void testMarkFailed_TransitionsToFailedWithErrorDetails() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");

        service.markFailed(generationId, "SPOTIFY_RATE_LIMIT", "Rate limit exceeded");
        GenerationStateService.GenerationState state = service.getState(generationId);

        assertEquals(GenerationStateService.GenerationStatus.FAILED, state.getStatus());
        assertEquals("SPOTIFY_RATE_LIMIT", state.getErrorCode());
        assertEquals("Rate limit exceeded", state.getErrorMessage());
        assertNotNull(state.getCompletedAt());
    }

    @Test
    void testUpdateProgress_UpdatesPercentComplete() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");

        service.updateProgress(generationId, 50);
        GenerationStateService.GenerationState state = service.getState(generationId);

        assertEquals(50, state.getPercentComplete());
    }

    @Test
    void testUpdateProgress_CapsAtMaximum() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");

        service.updateProgress(generationId, 150);
        GenerationStateService.GenerationState state = service.getState(generationId);

        assertEquals(99, state.getPercentComplete());
    }

    @Test
    void testUpdateProgress_ClampsToMinimum() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");

        service.updateProgress(generationId, -10);
        GenerationStateService.GenerationState state = service.getState(generationId);

        assertEquals(0, state.getPercentComplete());
    }

    @Test
    void testUpdateProgress_IgnoresWhenCompleted() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");
        service.markCompleted(generationId);

        service.updateProgress(generationId, 50);
        GenerationStateService.GenerationState state = service.getState(generationId);

        // Should remain at 100, not change to 50
        assertEquals(100, state.getPercentComplete());
    }

    @Test
    void testGetState_ReturnsNullForUnknownGeneration() {
        GenerationStateService.GenerationState state = service.getState("unknown-id");

        assertNull(state);
    }

    @Test
    void testExists_ReturnsTrueForTrackedGeneration() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");

        assertTrue(service.exists(generationId));
    }

    @Test
    void testExists_ReturnsFalseForUnknownGeneration() {
        assertFalse(service.exists("unknown-id"));
    }

    @Test
    void testDelete_RemovesGenerationState() {
        String generationId = "gen-123";
        service.track(generationId, "user-456");
        assertTrue(service.exists(generationId));

        service.delete(generationId);

        assertFalse(service.exists(generationId));
        assertNull(service.getState(generationId));
    }

    @Test
    void testMultipleConcurrentGenerations() {
        service.track("gen-1", "user-1");
        service.track("gen-2", "user-2");
        service.track("gen-3", "user-3");

        service.markCompleted("gen-1");
        service.markFailed("gen-2", "ERROR", "Failed");
        service.updateProgress("gen-3", 50);

        assertEquals(GenerationStateService.GenerationStatus.COMPLETED,
                service.getState("gen-1").getStatus());
        assertEquals(GenerationStateService.GenerationStatus.FAILED,
                service.getState("gen-2").getStatus());
        assertEquals(GenerationStateService.GenerationStatus.PENDING,
                service.getState("gen-3").getStatus());
        assertEquals(50, service.getState("gen-3").getPercentComplete());
    }

    @Test
    void testGetTrackedCount_ReturnsCorrectCount() {
        service.track("gen-1", "user-1");
        service.track("gen-2", "user-2");
        service.track("gen-3", "user-3");

        assertEquals(3, service.getTrackedCount());

        service.delete("gen-1");

        assertEquals(2, service.getTrackedCount());
    }

    @Test
    void testMarkCompleted_IgnoresUnknownGeneration() {
        // Should not throw
        assertDoesNotThrow(() -> service.markCompleted("unknown-id"));
    }

    @Test
    void testMarkFailed_IgnoresUnknownGeneration() {
        // Should not throw
        assertDoesNotThrow(() -> service.markFailed("unknown-id", "ERROR", "Message"));
    }

    @Test
    void testStateHasTimestamps() {
        String generationId = "gen-123";
        Instant beforeTrack = Instant.now();
        service.track(generationId, "user-456");
        Instant afterTrack = Instant.now();

        GenerationStateService.GenerationState state = service.getState(generationId);

        assertNotNull(state.getCreatedAt());
        assertTrue(state.getCreatedAt().isAfter(beforeTrack) || state.getCreatedAt().equals(beforeTrack));
        assertTrue(state.getCreatedAt().isBefore(afterTrack) || state.getCreatedAt().equals(afterTrack));
        assertNull(state.getCompletedAt());

        service.markCompleted(generationId);
        state = service.getState(generationId);

        assertNotNull(state.getCompletedAt());
        assertTrue(state.getCompletedAt().isAfter(state.getCreatedAt()));
    }
}