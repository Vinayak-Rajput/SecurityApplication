package com.hdfclife.capstoneone.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LogoutResponse", description = "Simple message payload")
public class LogoutResponse {

    @Schema(description = "Human-readable message", example = "Logout successful")
    private String message;
}
