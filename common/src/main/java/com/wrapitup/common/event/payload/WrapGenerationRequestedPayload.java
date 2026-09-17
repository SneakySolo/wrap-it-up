package com.wrapitup.common.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for wrap.generation.requested event.
 * Empty payload — all context is in the event envelope.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrapGenerationRequestedPayload {
    // Empty payload
}