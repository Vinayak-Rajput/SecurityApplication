package com.hdfclife.capstoneone.user.controller;

import com.hdfclife.capstoneone.user.dto.AdminCreateUserRequest;
import com.hdfclife.capstoneone.common.dto.ErrorResponse;
import com.hdfclife.capstoneone.user.dto.UserSummaryResponse;
import com.hdfclife.capstoneone.user.entity.User;
import com.hdfclife.capstoneone.user.mapper.UserMapper;
import com.hdfclife.capstoneone.logging.ApplicationLogger;
import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.user.service.UserRegistrationService;
import com.hdfclife.capstoneone.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Users",
        description = "Self-service and administrator user account APIs"
)
public class UserController {

    private final UserService userService;
    private final UserRegistrationService userRegistrationService;
    private final ApplicationLogger applicationLogger;
    private final UserMapper userMapper;

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns safe account data. Requires an ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated user summaries retrieved successfully"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Application database unavailable or circuit breaker open",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected database operation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Page<UserSummaryResponse>> getAllUsers(
            Authentication authentication,
            @ParameterObject
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<UserSummaryResponse> users = userService.getAllUsers(pageable)
                .map(userMapper::toSummaryResponse);

        applicationLogger.log(AuditEvent.request(AuditEventType.USER_LIST_VIEWED,
                authentication.getName(), null, "GET", "/users",
                200, "Administrator listed user accounts"));

        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    @Operation(
            summary = "View my account",
            description = "Returns the authenticated user's own account data, without credentials."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account retrieved",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Application database unavailable or circuit breaker open",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected database operation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserSummaryResponse> getMyAccount(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.getUserByUsername(principal.getUsername());

        applicationLogger.log(AuditEvent.request(AuditEventType.USER_SELF_VIEWED,
                principal.getUsername(), principal.getUsername(), "GET", "/users/me",
                200, "User viewed their own account"));

        return ResponseEntity.status(HttpStatus.OK)
                .body(userMapper.toSummaryResponse(user));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "View a user account",
            description = "Requires an ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account retrieved",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Application database unavailable or circuit breaker open",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected database operation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserSummaryResponse> getUser(@PathVariable long id, Authentication authentication) {
        User user = userService.getUser(id);

        applicationLogger.log(AuditEvent.request(AuditEventType.USER_VIEWED,
                authentication.getName(), user.getUsername(), "GET", "/users/{id}",
                200, "Administrator viewed a user account"));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userMapper.toSummaryResponse(user));
    }

    @PostMapping
    @Operation(
            summary = "Add a user",
            description = "Creates a USER, ADMIN, or AUDITOR account. Requires an ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User created",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or unsupported role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username or email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Application database unavailable or circuit breaker open",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected database operation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserSummaryResponse> addUser(@Valid @RequestBody AdminCreateUserRequest request, Authentication authentication) {
        User user = userRegistrationService.createAdminManagedUser(request.username(), request.email(), request.password(), request.role());

        applicationLogger.log(AuditEvent.request(AuditEventType.USER_CREATED,
                authentication.getName(), user.getUsername(), "POST", "/users",
                201, "Administrator created a user account"));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userMapper.toSummaryResponse(user));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a user",
            description = "Revokes the user's active sessions. Requires an ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted; no response body"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot delete own account or the last administrator",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Application database unavailable or circuit breaker open",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected database operation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteUser(@PathVariable long id, Authentication authentication) {
        String deletedUsername = userService.deleteUser(id, authentication.getName());

        applicationLogger.log(AuditEvent.request(AuditEventType.USER_DELETED, authentication.getName(),
                deletedUsername, "DELETE", "/users/{id}", 204,
                "Administrator deleted a user account"));

        return ResponseEntity
                .noContent()
                .build();
    }

}
