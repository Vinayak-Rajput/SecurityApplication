package com.vinayak.secapp.auth.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record RefreshCookieProperties(
        boolean secure,
        @NotBlank @Pattern(regexp = "(?i)Strict|Lax|None") String sameSite
) {
    @AssertTrue(message = "SameSite=None requires the refresh cookie to be Secure")
    public boolean isCrossSiteCookieSecure() {
        return !"None".equalsIgnoreCase(sameSite) || secure;
    }
}
