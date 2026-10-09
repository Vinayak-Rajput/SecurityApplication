package com.hdfclife.capstoneone.audit.controller;

import com.hdfclife.capstoneone.audit.dto.AuditLogResponse;
import com.hdfclife.capstoneone.common.dto.ErrorResponse;
import com.hdfclife.capstoneone.logging.ApplicationLogger;
import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.audit.service.AuditLogReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit/logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit", description = "Read-only audit log access")
public class AuditLogController {

    private final AuditLogReaderService auditLogReaderService;
    private final ApplicationLogger applicationLogger;

    @GetMapping
    @Operation(summary = "List recent audit logs", description = "Requires the AUDITOR role. Results are paginated.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Audit records retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuditLogResponse.class)))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination values",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized — missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden — AUDITOR role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Authenticated request limit exceeded",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Audit database unavailable or circuit breaker open",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected audit database operation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public List<AuditLogResponse> getRecentLogs(
        @Parameter(description = "Maximum number of rows to return (1–500)", example = "100")
        @RequestParam(defaultValue = "100") int limit,

        @Parameter(description = "Number of rows to skip (must be non-negative)", example = "0")
        @RequestParam(defaultValue = "0") int offset,

        Authentication authentication
    ) {
        List<AuditLogResponse> logs = auditLogReaderService.findRecentLogs(limit, offset);

        applicationLogger.log(
            AuditEvent.request(
                AuditEventType.AUDIT_LOG_ACCESS,
                authentication.getName(),
                null,
                "GET",
                "/audit/logs",
                200,
                "Auditor accessed audit logs: limit=%d, offset=%d, returned=%d".formatted(limit, offset, logs.size())
            )
        );

        return logs;
    }
}
