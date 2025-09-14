package com.example.scheduler.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for extracting employee CSV files.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties(prefix = "extract.csv.employees")
public class EmployeeCsvExtractProperties extends AbstractEmployeeCsvProperties {
    // Inherits all properties from EmployeeCsvProperties
    private String extractStatus; // Status to filter employees for extraction
}
