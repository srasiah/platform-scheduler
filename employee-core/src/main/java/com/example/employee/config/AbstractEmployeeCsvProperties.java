package com.example.employee.config;

import lombok.Data;
import java.util.Map;

/**
 * Abstract base class for employee CSV properties.
 */
@Data
public abstract class AbstractEmployeeCsvProperties {
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
    /** Preferred date format to try first when parsing date fields in the CSV. */
    private String preferredDateFormat;      

}
