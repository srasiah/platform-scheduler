package com.example.scheduler.jobs;

import com.example.employee.entity.EmployeeDelta;
import com.example.employee.entity.EmployeeIngestBatch;
import com.example.employee.service.EmployeeDeltaService;
import com.example.common.util.CsvUtils;
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
public class EmployeeDeltaReportJob implements Job {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeDeltaReportJob.class);
    private final EmployeeDeltaService deltaService;
    
    // Configuration - these could be moved to properties
    private final String reportOutputDir = "./.data/reports/deltas";
    
    public EmployeeDeltaReportJob(EmployeeDeltaService deltaService) {
        this.deltaService = deltaService;
    }
    
    @Override
    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
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
        
        // Create reports directory
        Path reportsDir = Paths.get(reportOutputDir);
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }
        
        // Generate timestamp for filenames
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Generate summary report
        generateSummaryReport(batchId, reportsDir, timestamp);
        
        // Generate detailed delta reports
        generateDetailedDeltaReports(batchId, reportsDir, timestamp);
        
        log.info("Delta reports generated successfully for batch: {}", batchId);
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
        
        Path summaryFile = reportsDir.resolve(String.format("delta_summary_%s_%s.csv", batchId, timestamp));
        CsvUtils.writeCsvFile(summaryData, summaryFile, ',');
        log.info("Generated summary report: {}", summaryFile);
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
        
        // Convert deltas to CSV format
        List<Map<String, String>> csvData = deltas.stream()
                .map(this::deltaToCsvRow)
                .collect(Collectors.toList());
        
        String filename = String.format("deltas_%s_%s_%s.csv", 
                deltaType.name().toLowerCase(), batchId, timestamp);
        Path reportFile = reportsDir.resolve(filename);
        
        CsvUtils.writeCsvFile(csvData, reportFile, ',');
        log.info("Generated {} employees report: {} ({} records)", deltaType, reportFile, deltas.size());
    }
    
    private void generateCombinedDeltaReport(String batchId, Path reportsDir, String timestamp) throws Exception {
        List<EmployeeDelta> allDeltas = deltaService.getDeltasForBatch(batchId);
        
        if (allDeltas.isEmpty()) {
            log.info("No deltas found for batch: {}", batchId);
            return;
        }
        
        // Convert all deltas to CSV format
        List<Map<String, String>> csvData = allDeltas.stream()
                .map(this::deltaToCsvRow)
                .collect(Collectors.toList());
        
        String filename = String.format("deltas_all_%s_%s.csv", batchId, timestamp);
        Path reportFile = reportsDir.resolve(filename);
        
        CsvUtils.writeCsvFile(csvData, reportFile, ',');
        log.info("Generated combined deltas report: {} ({} records)", reportFile, allDeltas.size());
    }
    
    private Map<String, String> deltaToCsvRow(EmployeeDelta delta) {
        Map<String, String> row = new LinkedHashMap<>();
        
        // Basic information
        row.put("employee_id", String.valueOf(delta.getEmployeeId()));
        row.put("batch_id", delta.getBatchId());
        row.put("previous_batch_id", delta.getPreviousBatchId() != null ? delta.getPreviousBatchId() : "");
        row.put("delta_type", delta.getDeltaType().name());
        row.put("detected_date", delta.getDetectedDate() != null ? delta.getDetectedDate().toString() : "");
        
        // Previous values
        row.put("previous_name", delta.getPreviousName() != null ? delta.getPreviousName() : "");
        row.put("previous_age", delta.getPreviousAge() != null ? String.valueOf(delta.getPreviousAge()) : "");
        row.put("previous_status", delta.getPreviousStatus() != null ? delta.getPreviousStatus() : "");
        row.put("previous_dob", delta.getPreviousDob() != null ? delta.getPreviousDob().toString() : "");
        
        // Current values
        row.put("current_name", delta.getCurrentName() != null ? delta.getCurrentName() : "");
        row.put("current_age", delta.getCurrentAge() != null ? String.valueOf(delta.getCurrentAge()) : "");
        row.put("current_status", delta.getCurrentStatus() != null ? delta.getCurrentStatus() : "");
        row.put("current_dob", delta.getCurrentDob() != null ? delta.getCurrentDob().toString() : "");
        
        // Change metadata
        row.put("changed_fields", delta.getChangedFields() != null ? delta.getChangedFields() : "");
        row.put("change_summary", delta.getChangeSummary() != null ? delta.getChangeSummary() : "");
        
        return row;
    }
}