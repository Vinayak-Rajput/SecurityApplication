package com.hdfclife.capstoneone.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "audit.datasource")
public record AuditDataSourceProperties(
        @NotBlank String url,
        @NotBlank String writerUsername,
        @NotBlank String writerPassword,
        @NotBlank String readerUsername,
        @NotBlank String readerPassword
) {
}
