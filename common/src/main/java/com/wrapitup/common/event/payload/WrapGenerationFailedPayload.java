package com.wrapitup.common.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for wrap.generation.failed event.
 * Contains error information without stack traces.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrapGenerationFailedPayload {

    @JsonProperty("stage")
    private String stage;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("retryable")
    private boolean retryable;

    @JsonProperty("message")
    private String message;
}