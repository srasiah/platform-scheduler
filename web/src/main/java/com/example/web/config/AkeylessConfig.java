package com.example.web.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Akeyless integration.
 */
@Configuration
@EnableConfigurationProperties(AkeylessProperties.class)
public class AkeylessConfig {

    private final AkeylessProperties akeylessProperties;

    public AkeylessConfig(AkeylessProperties akeylessProperties) {
        this.akeylessProperties = akeylessProperties;
    }
}