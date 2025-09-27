package com.example.web.config;

import com.example.web.service.AkeylessCredentialsService;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration for DataSource with Akeyless integration.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * Creates the appropriate DataSource based on Akeyless configuration.
     * If Akeyless is properly configured, uses Akeyless credentials.
     * Otherwise, falls back to traditional configuration.
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(AkeylessProperties akeylessProperties,
                                AkeylessCredentialsService akeylessCredentialsService,
                                DataSourceProperties properties) {
        
        if (akeylessProperties.isConfigured()) {
            logger.info("Configuring DataSource with Akeyless credentials");
            
            try {
                // Fetch credentials from Akeyless
                AkeylessCredentialsService.DatabaseCredentials credentials = 
                    akeylessCredentialsService.getDatabaseCredentials();
                
                // Create HikariDataSource with Akeyless credentials
                HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
                
                // Set connection properties from Akeyless
                dataSource.setJdbcUrl(credentials.getJdbcUrl());
                dataSource.setUsername(credentials.username());
                dataSource.setPassword(credentials.password());
                
                logger.info("DataSource configured with Akeyless credentials for database: {}", credentials.database());
                return dataSource;
                
            } catch (Exception e) {
                logger.error("Failed to configure DataSource with Akeyless credentials, falling back to traditional configuration", e);
                // Fall through to traditional configuration
            }
        }
        
        logger.info("Configuring traditional DataSource using application properties");
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }
}