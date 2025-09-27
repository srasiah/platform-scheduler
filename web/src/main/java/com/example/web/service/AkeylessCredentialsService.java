package com.example.web.service;

import com.example.web.config.AkeylessProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for fetching database credentials from Akeyless using REST API.
 * Only created when Akeyless API key is provided.
 */
@Service
@ConditionalOnProperty(name = "akeyless.api-key")
public class AkeylessCredentialsService {

    private static final Logger logger = LoggerFactory.getLogger(AkeylessCredentialsService.class);

    private final AkeylessProperties akeylessProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Cached authentication token and expiry
    private String authToken;
    private Instant tokenExpiry;
    
    // Cached credentials and refresh time
    private DatabaseCredentials cachedCredentials;
    private Instant lastRefresh;

    public AkeylessCredentialsService(AkeylessProperties akeylessProperties) {
        this.akeylessProperties = akeylessProperties;
        this.webClient = WebClient.builder()
            .baseUrl(akeylessProperties.apiUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gets database credentials from Akeyless, using cache if still valid.
     */
    public DatabaseCredentials getDatabaseCredentials() {
        // Check if cached credentials are still valid
        if (cachedCredentials != null && lastRefresh != null) {
            long secondsSinceRefresh = ChronoUnit.SECONDS.between(lastRefresh, Instant.now());
            if (secondsSinceRefresh < akeylessProperties.database().refreshInterval()) {
                logger.debug("Using cached database credentials (refreshed {} seconds ago)", secondsSinceRefresh);
                return cachedCredentials;
            }
        }

        logger.info("Fetching fresh database credentials from Akeyless");
        try {
            // Ensure we have a valid authentication token
            String token = getValidAuthToken();
            
            // Fetch the secret from Akeyless
            DatabaseCredentials credentials = fetchSecretFromAkeyless(token);
            
            // Cache the credentials
            this.cachedCredentials = credentials;
            this.lastRefresh = Instant.now();
            
            logger.info("Successfully fetched and cached database credentials from Akeyless");
            return credentials;
            
        } catch (Exception e) {
            logger.error("Failed to fetch database credentials from Akeyless", e);
            
            // If we have cached credentials, return them as fallback
            if (cachedCredentials != null) {
                logger.warn("Using cached credentials as fallback due to fetch failure");
                return cachedCredentials;
            }
            
            throw new RuntimeException("Failed to fetch database credentials and no cached credentials available", e);
        }
    }

    /**
     * Gets a valid authentication token, refreshing if necessary.
     */
    private String getValidAuthToken() throws Exception {
        tokenLock.lock();
        try {
            // Check if current token is still valid (with 5-minute buffer)
            if (authToken != null && tokenExpiry != null && 
                Instant.now().plus(5, ChronoUnit.MINUTES).isBefore(tokenExpiry)) {
                return authToken;
            }

            logger.debug("Authenticating with Akeyless");
            
            // Create authentication request
            Map<String, Object> authRequest = Map.of(
                "access-id", akeylessProperties.accessId(),
                "access-key", akeylessProperties.apiKey()
            );
            
            // Call Akeyless auth endpoint
            String authResponse = webClient.post()
                .uri("/auth")
                .body(Mono.just(authRequest), Map.class)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            // Parse authentication response
            JsonNode responseJson = objectMapper.readTree(authResponse);
            this.authToken = responseJson.path("token").asText();
            
            if (this.authToken.isEmpty()) {
                throw new RuntimeException("Failed to get authentication token from Akeyless");
            }
            
            // Assume token is valid for 1 hour (adjust based on your Akeyless configuration)
            this.tokenExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
            
            logger.debug("Successfully authenticated with Akeyless");
            return authToken;
            
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Fetches secret from Akeyless using the authenticated token.
     */
    private DatabaseCredentials fetchSecretFromAkeyless(String token) throws JsonProcessingException {
        // Create get-secret-value request
        Map<String, Object> secretRequest = Map.of(
            "names", new String[]{akeylessProperties.database().secretName()},
            "token", token
        );
        
        // Call Akeyless get-secret-value endpoint
        String secretResponse = webClient.post()
            .uri("/get-secret-value")
            .body(Mono.just(secretRequest), Map.class)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .block();
        
        // Parse the response to extract credentials
        JsonNode responseJson = objectMapper.readTree(secretResponse);
        
        // The secret value should be in the response
        JsonNode secretValueNode = responseJson.path(akeylessProperties.database().secretName());
        if (secretValueNode.isMissingNode()) {
            throw new RuntimeException("Secret not found in Akeyless response: " + akeylessProperties.database().secretName());
        }
        
        // Parse the secret value (assuming it's JSON with database credentials)
        String secretValue = secretValueNode.asText();
        JsonNode secretJson = objectMapper.readTree(secretValue);
        
        String username = secretJson.path("username").asText();
        String password = secretJson.path("password").asText();
        String host = secretJson.path("host").asText();
        int port = secretJson.path("port").asInt(5432); // Default PostgreSQL port
        String database = secretJson.path("database").asText();
        
        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("Invalid database credentials received from Akeyless: missing username or password");
        }
        
        return new DatabaseCredentials(username, password, host, port, database);
    }

    /**
     * Record class for database credentials.
     */
    public record DatabaseCredentials(
        String username,
        String password,
        String host,
        int port,
        String database
    ) {
        public String getJdbcUrl() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        }
    }
}