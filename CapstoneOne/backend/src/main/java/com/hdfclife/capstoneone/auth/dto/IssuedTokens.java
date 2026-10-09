package com.hdfclife.capstoneone.auth.dto;

public record IssuedTokens(
        String accessToken,
        String refreshToken
) {
}
