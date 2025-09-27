package com.example.employee.service.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Abstract base class for employee services that defines the contract for batch-based
 * file processing operations. This class provides common logging functionality and
 * defines abstract methods that implementing classes must provide.
 * 
 * <p>Implementing classes should handle specific operations like CSV ingestion or
 * extraction, while this abstract class ensures a consistent interface for batch
 * processing operations.
 */
public abstract class AbstractEmployeeService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Processes files from a source directory with batch tracking.
     * This method should handle the core business logic for processing files,
     * including error handling and batch status tracking.
     * 
     * @param sourceDir the source directory containing files to process
     * @param targetDir the target directory for processed files
     */
    public abstract void processFromDirectory(Path sourceDir, Path targetDir);

    /**
     * Processes files using default directory configuration.
     * This method should use configuration properties to determine source
     * and target directories, then delegate to processFromDirectory.
     */
    public abstract void processFromDirectory();

    /**
     * Validates that the service is properly configured and ready to process files.
     * This method should check configuration properties, database connectivity,
     * and any other prerequisites.
     * 
     * @return true if the service is ready to process files, false otherwise
     */
    public abstract boolean isReadyForProcessing();

    /**
     * Gets a human-readable description of what this service does.
     * This is useful for logging and monitoring purposes.
     * 
     * @return a description of the service's purpose
     */
    public abstract String getServiceDescription();
}
