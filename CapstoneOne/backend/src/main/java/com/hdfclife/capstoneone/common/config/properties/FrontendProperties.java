package com.hdfclife.capstoneone.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(List<String> allowedOrigins) {

    public FrontendProperties {
        allowedOrigins = normalize(allowedOrigins);
    }

    private static List<String> normalize(List<String> allowedOrigins) {
        if (allowedOrigins == null) {
            return List.of("http://localhost:3000");
        }

        return allowedOrigins.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
}
