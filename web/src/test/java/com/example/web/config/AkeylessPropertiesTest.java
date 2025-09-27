package com.example.web.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AkeylessPropertiesTest {

    @Test
    void shouldReturnTrueWhenConfiguredWithValidCredentials() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            "valid-api-key",
            "valid-access-id",
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenApiKeyIsNull() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            null,
            "valid-access-id",
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenApiKeyIsEmpty() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            "",
            "valid-access-id",
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenApiKeyIsBlank() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            "   ",
            "valid-access-id",
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAccessIdIsNull() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            "valid-api-key",
            null,
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAccessIdIsEmpty() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            "valid-api-key",
            "",
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAccessIdIsBlank() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties properties = new AkeylessProperties(
            "https://api.akeyless.io",
            "valid-api-key",
            "   ",
            database
        );
        
        // When & Then
        assertThat(properties.isConfigured()).isFalse();
    }
}