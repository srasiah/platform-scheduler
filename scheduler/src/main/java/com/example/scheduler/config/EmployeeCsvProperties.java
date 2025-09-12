package com.example.scheduler.config;

import lombok.Data;
import java.util.Map;

/**
 * Abstract base class for employee CSV properties.
 */
@Data
public abstract class EmployeeCsvProperties {
    /** Whether the operation is enabled. */
    private boolean enabled;
    /** Folder where the CSV files are stored. */
    private String fileFolder;
    /** Prefix for the CSV file names. */
    private String fileNamePrefix;
    /** Name of the table to use. */
    private String tableName;
    /** Mapping of CSV columns to database columns. */
    private Map<String, String> columnMapping;
    /** Folder where processed CSV files should be moved. */
    private String processedFolder;
    /** Status of the CSV processing. */
    private String status;
}
