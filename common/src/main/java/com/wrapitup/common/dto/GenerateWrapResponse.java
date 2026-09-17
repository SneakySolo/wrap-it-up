package com.wrapitup.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /wraps endpoint.
 * Contains the generation ID for polling status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateWrapResponse {
    @JsonProperty("generationId")
    private String generationId;
}