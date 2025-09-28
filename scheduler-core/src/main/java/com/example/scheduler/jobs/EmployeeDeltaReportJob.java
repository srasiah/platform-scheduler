package com.example.scheduler.jobs;

import com.example.employee.config.EmployeeDeltaProperties;
import com.example.employee.entity.EmployeeDelta;
import com.example.employee.entity.EmployeeIngestBatch;
import com.example.employee.service.EmployeeDeltaService;
import com.example.common.util.CsvUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quartz job to generate delta reports and export them as CSV files.
 */
@Component
@DisallowConcurrentExecution
public class EmployeeDeltaReportJob implements Job {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeDeltaReportJob.class);
    private final EmployeeDeltaService deltaService;
    private final EmployeeDeltaProperties deltaProperties;
    
    public EmployeeDeltaReportJob(EmployeeDeltaService deltaService, EmployeeDeltaProperties deltaProperties) {
        this.deltaService = deltaService;
        this.deltaProperties = deltaProperties;
    }
    
    @Override
    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        
        // Check if delta detection and reporting are enabled
        if (!deltaProperties.isEnabled()) {
            log.info("Delta detection is disabled. Skipping Employee Delta Report Job: {} - {}", jobGroup, jobName);
            return;
        }
        
        if (!deltaProperties.getReporting().isEnabled()) {
            log.info("Delta reporting is disabled. Skipping Employee Delta Report Job: {} - {}", jobGroup, jobName);
            return;
        }
        
        log.info("Starting Employee Delta Report Job: {} - {}", jobGroup, jobName);
        
        try {
            generateDeltaReports();
            log.info("Completed Employee Delta Report Job: {} - {}", jobGroup, jobName);
        } catch (Exception e) {
            log.error("Error in Employee Delta Report Job: {} - {}", jobGroup, jobName, e);
        }
    }
    
    private void generateDeltaReports() throws Exception {
        // Get the most recent batch
        EmployeeIngestBatch recentBatch = deltaService.getMostRecentBatch();
        if (recentBatch == null) {
            log.info("No ingest batches found. Skipping delta report generation.");
            return;
        }
        
        String batchId = recentBatch.getBatchId();
        log.info("Generating delta reports for batch: {}", batchId);
        
        // Create reports directory using configuration
        String outputDirectory = deltaProperties.getReporting().getOutputDirectory();
        Path reportsDir = Paths.get(outputDirectory);
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
            log.info("Created reports directory: {}", reportsDir.toAbsolutePath());
        }
        
        // Generate timestamp for filenames
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Generate reports based on configuration
        if (deltaProperties.getReporting().isGenerateSummaryReports()) {
            generateSummaryReport(batchId, reportsDir, timestamp);
        }
        
        if (deltaProperties.getReporting().isGenerateDetailedReports()) {
            generateDetailedDeltaReports(batchId, reportsDir, timestamp);
        }
        
        log.info("Delta reports generated successfully for batch: {} in directory: {}", batchId, reportsDir.toAbsolutePath());
    }
    
    private void generateSummaryReport(String batchId, Path reportsDir, String timestamp) throws Exception {
        EmployeeDeltaService.DeltaSummary summary = deltaService.getDeltaSummary(batchId);
        
        // Create summary CSV
        List<Map<String, String>> summaryData = Arrays.asList(
            Map.of(
                "batch_id", batchId,
                "report_generated", LocalDateTime.now().toString(),
                "new_employees", String.valueOf(summary.getNewEmployees()),
                "updated_employees", String.valueOf(summary.getUpdatedEmployees()),
                "deleted_employees", String.valueOf(summary.getDeletedEmployees()),
                "total_deltas", String.valueOf(summary.getTotalDeltas())
            )
        );
        
        String filePrefix = deltaProperties.getReporting().getFileNamePrefix();
        String filename = String.format("%ssummary_%s_%s.csv", filePrefix, batchId, timestamp);
        Path summaryFile = reportsDir.resolve(filename);
        
        CsvUtils.writeCsvFile(summaryData, summaryFile, ',');
        log.info("Generated summary report: {}", summaryFile.toAbsolutePath());
    }
    
    private void generateDetailedDeltaReports(String batchId, Path reportsDir, String timestamp) throws Exception {
        // Generate report for NEW employees
        generateDeltaReport(batchId, EmployeeDelta.DeltaType.NEW, reportsDir, timestamp);
        
        // Generate report for UPDATED employees
        generateDeltaReport(batchId, EmployeeDelta.DeltaType.UPDATED, reportsDir, timestamp);
        
        // Generate report for DELETED employees
        generateDeltaReport(batchId, EmployeeDelta.DeltaType.DELETED, reportsDir, timestamp);
        
        // Generate combined report with all deltas
        generateCombinedDeltaReport(batchId, reportsDir, timestamp);
    }
    
    private void generateDeltaReport(String batchId, EmployeeDelta.DeltaType deltaType, Path reportsDir, String timestamp) throws Exception {
        List<EmployeeDelta> deltas = deltaService.getDeltasForBatch(batchId, deltaType);
        
        if (deltas.isEmpty()) {
            log.info("No {} employees found for batch: {}", deltaType, batchId);
            return;
        }
        
        // Check if we need to limit records per report
        int maxRecords = deltaProperties.getReporting().getMaxRecordsPerReport();
        if (maxRecords > 0 && deltas.size() > maxRecords) {
            log.warn("Delta report for {} employees exceeds max records per report ({} > {}). Truncating to {} records.", 
                    deltaType, deltas.size(), maxRecords, maxRecords);
            deltas = deltas.subList(0, maxRecords);
        }
        
        // Convert deltas to CSV format
        List<Map<String, String>> csvData = deltas.stream()
                .map(this::deltaToCsvRow)
                .collect(Collectors.toList());
        
        String filePrefix = deltaProperties.getReporting().getFileNamePrefix();
        String filename = String.format("%s%s_%s_%s.csv", 
                filePrefix, deltaType.name().toLowerCase(), batchId, timestamp);
        Path reportFile = reportsDir.resolve(filename);
        
        CsvUtils.writeCsvFile(csvData, reportFile, ',');
        log.info("Generated {} employees report: {} ({} records)", deltaType, reportFile.toAbsolutePath(), deltas.size());
    }
    
    private void generateCombinedDeltaReport(String batchId, Path reportsDir, String timestamp) throws Exception {
        List<EmployeeDelta> allDeltas = deltaService.getDeltasForBatch(batchId);
        
        if (allDeltas.isEmpty()) {
            log.info("No deltas found for batch: {}", batchId);
            return;
        }
        
        // Check if we need to limit records per report
        int maxRecords = deltaProperties.getReporting().getMaxRecordsPerReport();
        if (maxRecords > 0 && allDeltas.size() > maxRecords) {
            log.warn("Combined delta report exceeds max records per report ({} > {}). Truncating to {} records.", 
                    allDeltas.size(), maxRecords, maxRecords);
            allDeltas = allDeltas.subList(0, maxRecords);
        }
        
        // Convert all deltas to CSV format
        List<Map<String, String>> csvData = allDeltas.stream()
                .map(this::deltaToCsvRow)
                .collect(Collectors.toList());
        
        String filePrefix = deltaProperties.getReporting().getFileNamePrefix();
        String filename = String.format("%sall_%s_%s.csv", filePrefix, batchId, timestamp);
        Path reportFile = reportsDir.resolve(filename);
        
        CsvUtils.writeCsvFile(csvData, reportFile, ',');
        log.info("Generated combined deltas report: {} ({} records)", reportFile.toAbsolutePath(), allDeltas.size());
    }
    
    private Map<String, String> deltaToCsvRow(EmployeeDelta delta) {
        Map<String, String> row = new LinkedHashMap<>();
        boolean includeUnchanged = deltaProperties.getReporting().isIncludeUnchangedFields();
        
        // Basic information - always included
        row.put("employee_id", String.valueOf(delta.getEmployeeId()));
        row.put("batch_id", delta.getBatchId());
        row.put("previous_batch_id", delta.getPreviousBatchId() != null ? delta.getPreviousBatchId() : "");
        row.put("delta_type", delta.getDeltaType().name());
        row.put("detected_date", delta.getDetectedDate() != null ? delta.getDetectedDate().toString() : "");
        
        // Previous values - include based on configuration and delta type
        if (includeUnchanged || delta.getDeltaType() == EmployeeDelta.DeltaType.UPDATED || delta.getDeltaType() == EmployeeDelta.DeltaType.DELETED) {
            row.put("previous_name", delta.getPreviousName() != null ? delta.getPreviousName() : "");
            row.put("previous_age", delta.getPreviousAge() != null ? String.valueOf(delta.getPreviousAge()) : "");
            row.put("previous_status", delta.getPreviousStatus() != null ? delta.getPreviousStatus() : "");
            row.put("previous_dob", delta.getPreviousDob() != null ? delta.getPreviousDob().toString() : "");
        }
        
        // Current values - include based on configuration and delta type  
        if (includeUnchanged || delta.getDeltaType() == EmployeeDelta.DeltaType.NEW || delta.getDeltaType() == EmployeeDelta.DeltaType.UPDATED) {
            row.put("current_name", delta.getCurrentName() != null ? delta.getCurrentName() : "");
            row.put("current_age", delta.getCurrentAge() != null ? String.valueOf(delta.getCurrentAge()) : "");
            row.put("current_status", delta.getCurrentStatus() != null ? delta.getCurrentStatus() : "");
            row.put("current_dob", delta.getCurrentDob() != null ? delta.getCurrentDob().toString() : "");
        }
        
        // Change metadata - always included for UPDATED records
        if (delta.getDeltaType() == EmployeeDelta.DeltaType.UPDATED) {
            row.put("changed_fields", delta.getChangedFields() != null ? delta.getChangedFields() : "");
        }
        row.put("change_summary", delta.getChangeSummary() != null ? delta.getChangeSummary() : "");
        
        return row;
    }
}