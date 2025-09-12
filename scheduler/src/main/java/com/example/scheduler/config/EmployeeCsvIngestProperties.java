package com.example.scheduler.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for ingesting employee CSV files.
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties(prefix = "ingest.csv.employees")
public class EmployeeCsvIngestProperties extends EmployeeCsvProperties {
    // Inherits all properties from EmployeeCsvProperties
}
