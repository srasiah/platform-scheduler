package com.example.web.controller;

import com.example.employee.config.EmployeeDeltaProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for delta configuration management.
 * Provides endpoints to view and validate delta configuration settings.
 */
@RestController
@RequestMapping("/api/delta-config")
public class DeltaConfigController {

    private final EmployeeDeltaProperties deltaProperties;

    public DeltaConfigController(EmployeeDeltaProperties deltaProperties) {
        this.deltaProperties = deltaProperties;
    }

    /**
     * Get the current delta configuration settings.
     * 
     * @return Current delta configuration
     */
    @GetMapping
    public ResponseEntity<EmployeeDeltaProperties> getDeltaConfiguration() {
        return ResponseEntity.ok(deltaProperties);
    }

    /**
     * Get a summary of key delta configuration settings.
     * 
     * @return Configuration summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DeltaConfigSummary> getDeltaConfigSummary() {
        DeltaConfigSummary summary = new DeltaConfigSummary(
            deltaProperties.isEnabled(),
            deltaProperties.getMaxBatchesRetention(),
            deltaProperties.getBatchRetentionPeriod().toDays(),
            deltaProperties.isDetectNew(),
            deltaProperties.isDetectUpdated(),
            deltaProperties.isDetectDeleted(),
            deltaProperties.getReporting().isEnabled(),
            deltaProperties.getPerformance().getBatchSize(),
            deltaProperties.getPerformance().isParallelProcessing(),
            deltaProperties.getNotifications().isEnabled()
        );
        return ResponseEntity.ok(summary);
    }

    /**
     * Validate the current delta configuration.
     * 
     * @return Validation results
     */
    @GetMapping("/validate")
    public ResponseEntity<ConfigValidationResult> validateDeltaConfiguration() {
        ConfigValidationResult result = new ConfigValidationResult();
        
        // Check if delta detection is enabled
        if (!deltaProperties.isEnabled()) {
            result.addWarning("Delta detection is disabled");
        }
        
        // Check batch retention settings
        if (deltaProperties.getMaxBatchesRetention() < 2) {
            result.addError("maxBatchesRetention must be at least 2 for delta comparison");
        }
        
        if (deltaProperties.getBatchRetentionPeriod().toDays() < 1) {
            result.addWarning("batchRetentionPeriod is less than 1 day, may cause data loss");
        }
        
        // Check performance settings
        if (deltaProperties.getPerformance().getBatchSize() < 1) {
            result.addError("Performance batch size must be positive");
        }
        
        if (deltaProperties.getPerformance().getBatchSize() > 10000) {
            result.addWarning("Large batch size may cause memory issues");
        }
        
        // Check if any detection types are enabled
        if (!deltaProperties.isDetectNew() && !deltaProperties.isDetectUpdated() && !deltaProperties.isDetectDeleted()) {
            result.addWarning("No delta detection types are enabled");
        }
        
        // Check notification settings
        if (deltaProperties.getNotifications().isEnabled() && deltaProperties.getNotifications().getRecipients().isEmpty()) {
            result.addWarning("Notifications are enabled but no recipients are configured");
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Summary of key delta configuration settings.
     */
    public static class DeltaConfigSummary {
        private final boolean enabled;
        private final int maxBatchesRetention;
        private final long batchRetentionDays;
        private final boolean detectNew;
        private final boolean detectUpdated;
        private final boolean detectDeleted;
        private final boolean reportingEnabled;
        private final int performanceBatchSize;
        private final boolean parallelProcessing;
        private final boolean notificationsEnabled;

        public DeltaConfigSummary(boolean enabled, int maxBatchesRetention, long batchRetentionDays,
                                 boolean detectNew, boolean detectUpdated, boolean detectDeleted,
                                 boolean reportingEnabled, int performanceBatchSize, boolean parallelProcessing,
                                 boolean notificationsEnabled) {
            this.enabled = enabled;
            this.maxBatchesRetention = maxBatchesRetention;
            this.batchRetentionDays = batchRetentionDays;
            this.detectNew = detectNew;
            this.detectUpdated = detectUpdated;
            this.detectDeleted = detectDeleted;
            this.reportingEnabled = reportingEnabled;
            this.performanceBatchSize = performanceBatchSize;
            this.parallelProcessing = parallelProcessing;
            this.notificationsEnabled = notificationsEnabled;
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public int getMaxBatchesRetention() { return maxBatchesRetention; }
        public long getBatchRetentionDays() { return batchRetentionDays; }
        public boolean isDetectNew() { return detectNew; }
        public boolean isDetectUpdated() { return detectUpdated; }
        public boolean isDetectDeleted() { return detectDeleted; }
        public boolean isReportingEnabled() { return reportingEnabled; }
        public int getPerformanceBatchSize() { return performanceBatchSize; }
        public boolean isParallelProcessing() { return parallelProcessing; }
        public boolean isNotificationsEnabled() { return notificationsEnabled; }
    }

    /**
     * Configuration validation result.
     */
    public static class ConfigValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public java.util.List<String> getWarnings() {
            return warnings;
        }
    }
}