package com.hdfclife.capstoneone.auth.util;

import java.util.UUID;

public final class TokenFormats {

    private TokenFormats() {
    }

    public static boolean isTokenId(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }
}
