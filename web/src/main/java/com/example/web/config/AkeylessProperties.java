package com.example.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for Akeyless integration.
 */
@ConfigurationProperties(prefix = "akeyless")
@Validated
public record AkeylessProperties(
    @NotBlank String apiUrl,
    String apiKey,  // Remove validation - allow blank for conditional validation
    String accessId, // Remove validation - allow blank for conditional validation
    @NotNull Database database
) {
    
    /**
     * Check if Akeyless is properly configured (has non-blank API key and access ID)
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() 
            && accessId != null && !accessId.trim().isEmpty();
    }
    
    /**
     * Database-specific Akeyless configuration.
     */
    public record Database(
        @NotBlank String secretName,
        @Positive int refreshInterval
    ) {}
}