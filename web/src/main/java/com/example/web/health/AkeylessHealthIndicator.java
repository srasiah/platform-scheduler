package com.example.web.health;

import com.example.web.config.AkeylessProperties;
import com.example.web.service.AkeylessCredentialsService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Akeyless connectivity.
 * Only enabled when Akeyless is properly configured.
 */
@Component
@ConditionalOnBean(AkeylessCredentialsService.class)
public class AkeylessHealthIndicator implements HealthIndicator {

    private final AkeylessCredentialsService akeylessCredentialsService;
    private final AkeylessProperties akeylessProperties;

    public AkeylessHealthIndicator(AkeylessCredentialsService akeylessCredentialsService,
                                   AkeylessProperties akeylessProperties) {
        this.akeylessCredentialsService = akeylessCredentialsService;
        this.akeylessProperties = akeylessProperties;
    }

    @Override
    public Health health() {
        // Only check health if Akeyless is configured
        if (!akeylessProperties.isConfigured()) {
            return Health.unknown()
                .withDetail("status", "Akeyless not configured")
                .build();
        }
        
        try {
            // Try to fetch database credentials to verify Akeyless connectivity
            AkeylessCredentialsService.DatabaseCredentials credentials = 
                akeylessCredentialsService.getDatabaseCredentials();
            
            return Health.up()
                .withDetail("status", "Successfully connected to Akeyless")
                .withDetail("database", credentials.database())
                .withDetail("host", credentials.host())
                .withDetail("port", credentials.port())
                .withDetail("username", credentials.username())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "Failed to connect to Akeyless")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}