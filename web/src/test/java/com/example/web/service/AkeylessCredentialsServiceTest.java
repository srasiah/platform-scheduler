package com.example.web.service;

import com.example.web.config.AkeylessProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AkeylessCredentialsServiceTest {

    private MockWebServer mockWebServer;
    private AkeylessCredentialsService akeylessCredentialsService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        
        // Create test properties
        AkeylessProperties.Database database = new AkeylessProperties.Database(
            "/database/postgresql/scheduler",
            3600
        );
        
        AkeylessProperties akeylessProperties = new AkeylessProperties(
            baseUrl,
            "test-api-key",
            "test-access-id",
            database
        );
        
        akeylessCredentialsService = new AkeylessCredentialsService(akeylessProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldSuccessfullyFetchDatabaseCredentials() throws Exception {
        // Given
        String authToken = "auth-token-12345";
        String authResponse = """
            {
                "token": "%s"
            }
            """.formatted(authToken);
        
        String secretResponse = """
            {
                "host": "postgresql.example.com",
                "port": 5432,
                "database": "scheduler",
                "username": "scheduler_user",
                "password": "secure-password-123"
            }
            """;
        
        // Mock auth endpoint
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(authResponse));
        
        // Mock get secret endpoint
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(secretResponse));
        
        // When
        AkeylessCredentialsService.DatabaseCredentials credentials = 
            akeylessCredentialsService.getDatabaseCredentials();
        
        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.host()).isEqualTo("postgresql.example.com");
        assertThat(credentials.port()).isEqualTo(5432);
        assertThat(credentials.database()).isEqualTo("scheduler");
        assertThat(credentials.username()).isEqualTo("scheduler_user");
        assertThat(credentials.password()).isEqualTo("secure-password-123");
        assertThat(credentials.getJdbcUrl()).isEqualTo("jdbc:postgresql://postgresql.example.com:5432/scheduler");
        
        // Verify auth request
        RecordedRequest authRequest = mockWebServer.takeRequest();
        assertThat(authRequest.getPath()).isEqualTo("/auth");
        assertThat(authRequest.getMethod()).isEqualTo("POST");
        
        String authBody = authRequest.getBody().readUtf8();
        assertThat(authBody).contains("test-api-key");
        assertThat(authBody).contains("test-access-id");
        
        // Verify secret request
        RecordedRequest secretRequest = mockWebServer.takeRequest();
        assertThat(secretRequest.getPath()).isEqualTo("/get-secret-value");
        assertThat(secretRequest.getMethod()).isEqualTo("POST");
        assertThat(secretRequest.getHeader("Authorization")).isEqualTo("Bearer " + authToken);
    }

    @Test
    void shouldThrowExceptionOnAuthenticationFailure() throws Exception {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\": \"Invalid credentials\"}"));
        
        // When & Then
        assertThatThrownBy(() -> akeylessCredentialsService.getDatabaseCredentials())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to fetch database credentials");
    }
}