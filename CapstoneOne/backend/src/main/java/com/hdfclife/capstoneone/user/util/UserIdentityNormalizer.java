package com.hdfclife.capstoneone.user.util;

import java.util.Locale;

public final class UserIdentityNormalizer {

    private UserIdentityNormalizer() {
    }

    public static String username(String value) {
        return value.trim();
    }

    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
