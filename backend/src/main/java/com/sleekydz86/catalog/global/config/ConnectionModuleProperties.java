package com.sleekydz86.catalog.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "com.sleekydz86.catalog.connection")
public record ConnectionModuleProperties(
        String secretMasterKey,
        Duration connectTimeout,
        Duration socketTimeout
) {
}
