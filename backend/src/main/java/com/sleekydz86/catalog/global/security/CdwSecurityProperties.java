package com.sleekydz86.catalog.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "cdw.catalog.security")
public record CdwSecurityProperties(
        @NotBlank String jwtSecret,
        @NotBlank String workerToken,
        boolean swaggerEnabled
) {
    public CdwSecurityProperties {
        if (jwtSecret != null && jwtSecret.length() < 32) {
            throw new IllegalArgumentException("cdw.catalog.security.jwt-secret must be at least 32 characters");
        }
    }
}
