# Employee Delta Configuration Guide

This guide explains how to configure the employee delta detection system using application properties.

## Overview

The employee delta detection system can be configured through YAML properties in `application.yml` or environment-specific configuration files. All properties support environment variable overrides and are organized under the `employee` configuration hierarchy.

## Configuration Structure

All employee-related configuration is now organized under the `employee` root:

```yaml
employee:
  delta:      # Delta detection settings
  ingest:     # CSV ingestion settings  
  extract:    # CSV extraction settings
  transfer:   # Transfer settings (REST/SFTP)
```

## Core Delta Configuration

### `employee.delta.enabled`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_ENABLED`
- **Description:** Master switch to enable/disable delta detection entirely.

### `employee.delta.maxBatchesRetention`
- **Type:** Integer
- **Default:** `100`
- **Environment Variable:** `EMPLOYEE_DELTA_MAX_BATCHES`
- **Description:** Maximum number of historical batches to keep for comparison.

### `employee.delta.batchRetentionPeriod`
- **Type:** Duration
- **Default:** `90d`
- **Environment Variable:** `EMPLOYEE_DELTA_RETENTION_DAYS`
- **Description:** Maximum age of batches to keep (e.g., "30d", "180d", "1y").

### `employee.delta.autoCleanupEnabled`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_AUTO_CLEANUP`
- **Description:** Whether to automatically clean up old batches.

## Delta Detection Configuration

### `employee.delta.ignoredFields`
- **Type:** Comma-separated list
- **Default:** `transactionId,createdDate`
- **Environment Variable:** `EMPLOYEE_DELTA_IGNORED_FIELDS`
- **Description:** Fields to ignore during comparison.

### `employee.delta.detectNew`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_DETECT_NEW`
- **Description:** Whether to detect new employee records.

### `employee.delta.detectUpdated`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_DETECT_UPDATED`
- **Description:** Whether to detect updated employee records.

### `employee.delta.detectDeleted`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_DETECT_DELETED`
- **Description:** Whether to detect deleted employee records.

### `employee.delta.detailedChangeLogging`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_DETAILED_LOGGING`
- **Description:** Whether to log detailed before/after values for changes.

## Reporting Configuration

### `employee.delta.reporting.enabled`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_REPORTING_ENABLED`
- **Description:** Whether to generate delta reports.

### `employee.delta.reporting.outputDirectory`
- **Type:** String
- **Default:** `${csv.baseFolder}/reports/delta`
- **Environment Variable:** `EMPLOYEE_DELTA_REPORT_DIR`
- **Description:** Directory where delta reports will be saved.

### `employee.delta.reporting.fileNamePrefix`
- **Type:** String
- **Default:** `employee-delta-report-`
- **Environment Variable:** `EMPLOYEE_DELTA_REPORT_PREFIX`
- **Description:** Prefix for delta report filenames.

### `employee.delta.reporting.generateDetailedReports`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_DETAILED_REPORTS`
- **Description:** Whether to generate detailed CSV reports with all delta records.

### `employee.delta.reporting.generateSummaryReports`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_SUMMARY_REPORTS`
- **Description:** Whether to generate summary reports with counts only.

### `employee.delta.reporting.maxRecordsPerReport`
- **Type:** Integer
- **Default:** `10000`
- **Environment Variable:** `EMPLOYEE_DELTA_MAX_RECORDS_PER_REPORT`
- **Description:** Maximum records per report file (splits large reports).

### `employee.delta.reporting.includeUnchangedFields`
- **Type:** Boolean
- **Default:** `false`
- **Environment Variable:** `EMPLOYEE_DELTA_INCLUDE_UNCHANGED`
- **Description:** Whether to include unchanged fields in detailed reports.

## Performance Configuration

### `employee.delta.performance.batchSize`
- **Type:** Integer
- **Default:** `1000`
- **Environment Variable:** `EMPLOYEE_DELTA_BATCH_SIZE`
- **Description:** Number of records to process in each batch during comparison.

### `employee.delta.performance.parallelProcessing`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_PARALLEL`
- **Description:** Whether to use parallel processing for delta detection.

### `employee.delta.performance.threadPoolSize`
- **Type:** Integer
- **Default:** `0` (uses available processors)
- **Environment Variable:** `EMPLOYEE_DELTA_THREAD_POOL`
- **Description:** Number of threads for parallel processing (0 = auto).

### `employee.delta.performance.enableSnapshotCaching`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_ENABLE_CACHE`
- **Description:** Whether to cache employee snapshots in memory.

### `employee.delta.performance.maxCacheSize`
- **Type:** Integer
- **Default:** `50000`
- **Environment Variable:** `EMPLOYEE_DELTA_MAX_CACHE`
- **Description:** Maximum number of employee records to cache.

## Notification Configuration

### `employee.delta.notifications.enabled`
- **Type:** Boolean
- **Default:** `false`
- **Environment Variable:** `EMPLOYEE_DELTA_NOTIFICATIONS_ENABLED`
- **Description:** Whether to send notifications for significant delta events.

### `employee.delta.notifications.newRecordThreshold`
- **Type:** Integer
- **Default:** `100`
- **Environment Variable:** `EMPLOYEE_DELTA_NEW_THRESHOLD`
- **Description:** Minimum new records to trigger notification.

### `employee.delta.notifications.updatedRecordThreshold`
- **Type:** Integer
- **Default:** `50`
- **Environment Variable:** `EMPLOYEE_DELTA_UPDATED_THRESHOLD`
- **Description:** Minimum updated records to trigger notification.

### `employee.delta.notifications.deletedRecordThreshold`
- **Type:** Integer
- **Default:** `10`
- **Environment Variable:** `EMPLOYEE_DELTA_DELETED_THRESHOLD`
- **Description:** Minimum deleted records to trigger notification.

### `employee.delta.notifications.recipients`
- **Type:** Comma-separated list
- **Default:** (empty)
- **Environment Variable:** `EMPLOYEE_DELTA_NOTIFICATION_EMAILS`
- **Description:** Email addresses to notify for delta events.

### `employee.delta.notifications.includeDetails`
- **Type:** Boolean
- **Default:** `true`
- **Environment Variable:** `EMPLOYEE_DELTA_NOTIFICATION_DETAILS`
- **Description:** Whether to include detailed delta info in notifications.

## Environment-Specific Configuration

### Development (application-dev.yml)
```yaml
employee:
  delta:
    maxBatchesRetention: 20
    batchRetentionPeriod: 30d
    performance:
      batchSize: 500
      parallelProcessing: false
    reporting:
      includeUnchangedFields: true
```

### Production (application-prod.yml)
```yaml
employee:
  delta:
    maxBatchesRetention: 200
    batchRetentionPeriod: 180d
    performance:
      batchSize: 2000
      threadPoolSize: 4
    notifications:
      enabled: true
      recipients: admin@company.com
```

## Environment Variables Example

```bash
# Core settings
export EMPLOYEE_DELTA_ENABLED=true
export EMPLOYEE_DELTA_MAX_BATCHES=150
export EMPLOYEE_DELTA_RETENTION_DAYS=120d

# Performance settings
export EMPLOYEE_DELTA_BATCH_SIZE=1500
export EMPLOYEE_DELTA_PARALLEL=true
export EMPLOYEE_DELTA_THREAD_POOL=2

# Notification settings
export EMPLOYEE_DELTA_NOTIFICATIONS_ENABLED=true
export EMPLOYEE_DELTA_NOTIFICATION_EMAILS=ops@company.com,admin@company.com
export EMPLOYEE_DELTA_NEW_THRESHOLD=200
```

## Configuration Validation

The system will validate configuration on startup and log warnings for:
- Invalid duration formats
- Non-existent output directories (will attempt to create)
- Invalid email addresses in recipients
- Performance settings that may cause issues

## Monitoring and Logging

To enable detailed delta logging, add to your configuration:

```yaml
logging:
  level:
    com.example.employee.service.impl.EmployeeDeltaServiceImpl: DEBUG
    com.example.employee.delta: DEBUG
```

This will provide detailed information about delta detection processes, performance metrics, and configuration values being used.