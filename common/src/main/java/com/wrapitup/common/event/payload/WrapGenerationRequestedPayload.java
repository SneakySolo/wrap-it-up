package com.wrapitup.common.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Payload for wrap.generation.requested event.
 * Empty payload — all context is in the event envelope.
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class WrapGenerationRequestedPayload {
    // Intentionally empty - all context passed via event envelope
    // (generationId, spotifyAccountId, etc.)
}