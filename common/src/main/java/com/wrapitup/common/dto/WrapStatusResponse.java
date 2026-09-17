package com.wrapitup.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET /wraps/{generationId}/status endpoint.
 * Contains the current status of the wrap generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrapStatusResponse {
    @JsonProperty("generationId")
    private String generationId;

    @JsonProperty("status")
    private String status; // PENDING, COMPLETED, FAILED

    @JsonProperty("message")
    private String message;
}