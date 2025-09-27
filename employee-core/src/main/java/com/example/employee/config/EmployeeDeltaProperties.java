package com.example.employee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Configuration properties for employee delta detection and processing.
 */
@Data
@Component
@ConfigurationProperties(prefix = "employee.delta")
public class EmployeeDeltaProperties {
    
    /**
     * Whether delta detection is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Maximum number of batches to keep in the database for delta comparison.
     * Older batches will be archived or cleaned up.
     */
    private int maxBatchesRetention = 100;
    
    /**
     * Maximum age of batches to keep for delta comparison.
     * Older batches will be archived or cleaned up.
     */
    private Duration batchRetentionPeriod = Duration.ofDays(90);
    
    /**
     * Whether to automatically clean up old batches based on retention settings.
     */
    private boolean autoCleanupEnabled = true;
    
    /**
     * Fields to ignore during delta comparison.
     * Changes to these fields will not trigger delta records.
     */
    private Set<String> ignoredFields = Set.of("transactionId", "createdDate");
    
    /**
     * Whether to detect new employee records.
     */
    private boolean detectNew = true;
    
    /**
     * Whether to detect updated employee records.
     */
    private boolean detectUpdated = true;
    
    /**
     * Whether to detect deleted employee records.
     */
    private boolean detectDeleted = true;
    
    /**
     * Whether to create detailed change logs for updated records.
     * When true, stores before/after values for changed fields.
     */
    private boolean detailedChangeLogging = true;
    
    /**
     * Reporting configuration for delta summaries.
     */
    private Reporting reporting = new Reporting();
    
    @Data
    public static class Reporting {
        /**
         * Whether to generate delta summary reports.
         */
        private boolean enabled = true;
        
        /**
         * Directory where delta reports will be saved.
         */
        private String outputDirectory = "./.data/reports/delta";
        
        /**
         * File name prefix for delta reports.
         */
        private String fileNamePrefix = "employee-delta-report-";
        
        /**
         * Whether to generate detailed CSV reports with all delta records.
         */
        private boolean generateDetailedReports = true;
        
        /**
         * Whether to generate summary reports with counts only.
         */
        private boolean generateSummaryReports = true;
        
        /**
         * Maximum number of delta records to include in a single detailed report.
         * Large batches will be split into multiple files.
         */
        private int maxRecordsPerReport = 10000;
        
        /**
         * Whether to include unchanged fields in detailed reports.
         */
        private boolean includeUnchangedFields = false;
    }
    
    /**
     * Performance tuning configuration.
     */
    private Performance performance = new Performance();
    
    @Data
    public static class Performance {
        /**
         * Batch size for processing delta comparisons.
         * Larger batches use more memory but may be faster.
         */
        private int batchSize = 1000;
        
        /**
         * Whether to use parallel processing for delta detection.
         */
        private boolean parallelProcessing = true;
        
        /**
         * Number of threads to use for parallel delta processing.
         * If 0, uses number of available processors.
         */
        private int threadPoolSize = 0;
        
        /**
         * Whether to cache employee snapshots in memory for faster comparison.
         */
        private boolean enableSnapshotCaching = true;
        
        /**
         * Maximum size of snapshot cache (number of employee records).
         */
        private int maxCacheSize = 50000;
    }
    
    /**
     * Notification configuration for delta events.
     */
    private Notifications notifications = new Notifications();
    
    @Data
    public static class Notifications {
        /**
         * Whether to send notifications for delta events.
         */
        private boolean enabled = false;
        
        /**
         * Minimum number of new records to trigger a notification.
         */
        private int newRecordThreshold = 100;
        
        /**
         * Minimum number of updated records to trigger a notification.
         */
        private int updatedRecordThreshold = 50;
        
        /**
         * Minimum number of deleted records to trigger a notification.
         */
        private int deletedRecordThreshold = 10;
        
        /**
         * Email addresses to notify for significant delta events.
         */
        private Set<String> recipients = Set.of();
        
        /**
         * Whether to include detailed delta information in notifications.
         */
        private boolean includeDetails = true;
    }
}