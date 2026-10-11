package com.vinayak.secapp.auth.security;

import com.vinayak.secapp.common.config.properties.FrontendProperties;
import com.vinayak.secapp.auth.exception.DisallowedRequestOriginException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RefreshRequestOriginValidator {
    private final Set<String> allowedOrigins;

    public RefreshRequestOriginValidator(FrontendProperties frontendProperties) {
        this.allowedOrigins = Set.copyOf(frontendProperties.allowedOrigins());
    }

    public void requireAllowed(String origin) {
        // Use exact origin matching (scheme, host, and port) as the CSRF boundary for refresh-cookie requests.
        if (origin == null || !allowedOrigins.contains(origin)) {
            throw new DisallowedRequestOriginException("Refresh request origin is not allowed");
        }
    }
}
