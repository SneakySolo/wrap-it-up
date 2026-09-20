package com.wrapitup.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Universal event envelope for all Kafka events.
 *
 * All events must follow this structure:
 * - eventId: unique identifier for this event
 * - eventType: type of event (e.g., wrap.generation.requested)
 * - eventVersion: version of the event schema
 * - occurredAt: timestamp when event occurred
 * - generationId: unique identifier for the entire wrap generation
 * - spotifyAccountId: the Spotify account this event belongs to
 * - payload: event-specific data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventEnvelope {
    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private String occurredAt;
    private String generationId;
    private String spotifyAccountId;
    private JsonNode payload;

    /**
     * Create a new builder with default values.
     */
    public static EventEnvelopeBuilder customBuilder() {
        return builder()
                .eventId(UUID.randomUUID().toString())
                .eventVersion(1)
                .occurredAt(Instant.now().toString());
    }
}