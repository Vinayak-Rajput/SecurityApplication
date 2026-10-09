package com.hdfclife.capstoneone.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
@Schema(
        name = "ErrorResponse",
        description = "Standard API error body returned when a request fails"
)
public class ErrorResponse {

    @Schema(
            description = "Time the error was generated",
            example = "2026-09-24T05:49:00Z"
    )
    private final Instant timestamp = Instant.now();

    @Schema(
            description = "HTTP status code",
            example = "400"
    )
    private final int status;

    @Schema(
            description = "HTTP status reason phrase",
            example = "Bad Request"
    )
    private final String error;

    @Schema(
            description = "Safe error message for the client",
            example = "Invalid request"
    )
    private final String message;

    @Schema(
            description = "Request path that produced the error",
            example = "/auth"
    )
    private final String path;

    @Schema(
            description = "Request correlation ID for support and log tracing",
            example = "a8e5a593-c4d9-4b39-8f60-6bdab662e7fc"
    )
    private final String correlationId;
}
