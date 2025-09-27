package com.example.web.integration;

import com.example.web.config.AkeylessProperties;
import com.example.web.config.DataSourceConfig;
import com.example.web.service.AkeylessCredentialsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test demonstrating Akeyless configuration behavior.
 */
@SpringBootTest(classes = {DataSourceConfig.class})
@TestPropertySource(properties = {
    "akeyless.api-url=https://api.akeyless.io",
    "akeyless.api-key=",  // Empty - should trigger fallback mode
    "akeyless.access-id=",
    "akeyless.database.secret-name=/database/postgresql/scheduler",
    "akeyless.database.refresh-interval=3600",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/test",
    "spring.datasource.username=test",
    "spring.datasource.password=test"
})
@ActiveProfiles("test")
class AkeylessIntegrationTest {

    @Test
    void shouldUseFallbackConfigurationWhenAkeylessNotConfigured() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties akeylessProperties = new AkeylessProperties(
            "https://api.akeyless.io",
            "",  // Empty API key
            "",  // Empty access ID
            database
        );
        
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl("jdbc:postgresql://localhost:5432/test");
        dataSourceProperties.setUsername("test");
        dataSourceProperties.setPassword("test");
        
        DataSourceConfig config = new DataSourceConfig();
        
        // When
        DataSource dataSource = config.dataSource(
            akeylessProperties, 
            null, // No service should be created when not configured
            dataSourceProperties
        );
        
        // Then
        assertThat(dataSource).isNotNull();
        assertThat(akeylessProperties.isConfigured()).isFalse();
    }

    @Test
    void shouldReturnTrueWhenAkeylessIsConfigured() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties akeylessProperties = new AkeylessProperties(
            "https://api.akeyless.io",
            "valid-api-key",
            "valid-access-id",
            database
        );
        
        // When & Then
        assertThat(akeylessProperties.isConfigured()).isTrue();
    }

    @Test
    void shouldCreateCredentialsServiceWhenProperlyConfigured() {
        // Given
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties akeylessProperties = new AkeylessProperties(
            "https://api.akeyless.io",
            "valid-api-key",
            "valid-access-id",
            database
        );
        
        // When
        AkeylessCredentialsService service = new AkeylessCredentialsService(akeylessProperties);
        
        // Then
        assertThat(service).isNotNull();
    }

    @Test 
    void shouldBuildValidJdbcUrl() {
        // Given
        AkeylessCredentialsService.DatabaseCredentials credentials = 
            new AkeylessCredentialsService.DatabaseCredentials(
                "scheduler_user",     // username
                "secure-password",    // password  
                "postgresql.example.com", // host
                5432,                 // port
                "scheduler"           // database
            );
        
        // When
        String jdbcUrl = credentials.getJdbcUrl();
        
        // Then
        assertThat(jdbcUrl).isEqualTo("jdbc:postgresql://postgresql.example.com:5432/scheduler");
    }
}