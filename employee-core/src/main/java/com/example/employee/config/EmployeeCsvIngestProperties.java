package com.example.employee.config;

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
@ConfigurationProperties(prefix = "employee.ingest")
public class EmployeeCsvIngestProperties extends AbstractEmployeeCsvProperties {
    // Inherits all properties from AbstractEmployeeCsvProperties including preferredDateFormat
    /** Status of the CSV processing. */
    private String defaultStatus;
}
