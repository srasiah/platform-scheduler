package com.example.employee.service.impl;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeIngestBatch;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.service.EmployeeDeltaService;
import com.example.employee.service.EmployeeIngestService;
import com.example.employee.service.EmployeeService;
import com.example.employee.service.base.AbstractEmployeeService;
import com.example.employee.config.EmployeeCsvIngestProperties;
import com.example.common.util.CsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeIngestServiceImpl extends AbstractEmployeeService implements EmployeeIngestService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeIngestServiceImpl.class);
    private final EmployeeRepository employeeRepository;
    private final EmployeeCsvIngestProperties props;
    private final EmployeeDeltaService deltaService;

    public EmployeeIngestServiceImpl(EmployeeRepository employeeRepository, 
                                   EmployeeCsvIngestProperties props,
                                   EmployeeDeltaService deltaService) {
        this.employeeRepository = employeeRepository;
        this.props = props;
        this.deltaService = deltaService;
    }

    @Override
    public void ingestFromDirectory(Path ingestDir, Path processedDir) {
        processFromDirectory(ingestDir, processedDir);
    }

    @Override
    public void ingestFromDirectory() {
        processFromDirectory();
    }

    @Override
    public void processFromDirectory(Path ingestDir, Path processedDir) {
        log.info("Starting EmployeeCsvIngestServiceImpl.ingestFromDirectory. Ingest directory: {}", ingestDir);
        String batchId = EmployeeService.generateBatchId();
        
        // Create ingest batch tracking
        deltaService.createIngestBatch(batchId, "batch-" + System.currentTimeMillis());
        
        try {
            if (!Files.exists(processedDir)) {
                Files.createDirectories(processedDir);
            }
        } catch (Exception e) {
            log.error("Failed to create processed folder: {}", processedDir, e);
            deltaService.updateIngestBatch(batchId, EmployeeIngestBatch.IngestStatus.FAILED, 
                                         0, 0, 0, "Failed to create processed directory: " + e.getMessage());
            return;
        }
        
        try {
            List<Path> csvFiles = Files.list(ingestDir)
                .filter(p -> p.getFileName().toString().startsWith(props.getFileNamePrefix()) && p.getFileName().toString().endsWith(".csv"))
                .toList();
            
            int totalProcessed = 0;
            int newRecordsCount = 0;
            
            for (Path file : csvFiles) {
                IngestResult result = processCSVFileWithDelta(file, processedDir, batchId);
                totalProcessed += result.totalRecords;
                newRecordsCount += result.newRecords;
            }
            
            // After processing all CSV files, perform delta detection
            performDeltaDetection(batchId, totalProcessed, newRecordsCount);
            
            log.info("EmployeeCsvIngestServiceImpl.ingestFromDirectory completed for ingest directory: {}", ingestDir);
        } catch (Exception e) {
            log.error("Error processing files in ingest directory {}", ingestDir, e);
            deltaService.updateIngestBatch(batchId, EmployeeIngestBatch.IngestStatus.FAILED, 
                                         0, 0, 0, "Error processing files: " + e.getMessage());
        }
    }

    @Override
    public void processFromDirectory() {
        Path ingestDir = Path.of(props.getFileFolder());
        Path processedDir = Path.of(props.getProcessedFolder());
        processFromDirectory(ingestDir, processedDir);
    }

    @Override
    public boolean isReadyForProcessing() {
        try {
            // Check if properties are properly configured
            if (props == null || props.getFileFolder() == null || props.getProcessedFolder() == null) {
                log.warn("Employee ingest service not properly configured - missing properties");
                return false;
            }
            
            // Check if required directories exist or can be created
            Path ingestDir = Path.of(props.getFileFolder());
            Path processedDir = Path.of(props.getProcessedFolder());
            
            EmployeeService.ensureDirectoryExists(ingestDir);
            EmployeeService.ensureDirectoryExists(processedDir);
            
            // Check repository connectivity
            if (employeeRepository == null) {
                log.warn("Employee ingest service not ready - repository not available");
                return false;
            }
            
            // Check delta service availability
            if (deltaService == null) {
                log.warn("Employee ingest service not ready - delta service not available");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error checking if employee ingest service is ready for processing", e);
            return false;
        }
    }

    @Override
    public String getServiceDescription() {
        return "Employee CSV Ingest Service - Processes employee data from CSV files with delta detection";
    }

    /**
     * Helper class to track ingest results
     */
    private static class IngestResult {
        final int totalRecords;
        final int newRecords;
        
        IngestResult(int totalRecords, int newRecords) {
            this.totalRecords = totalRecords;
            this.newRecords = newRecords;
        }
    }
    
    /**
     * Processes a single CSV file and ingests employee records with delta tracking.
     * 
     * @param file the CSV file to process
     * @param processedDir the directory to move processed files to
     * @param batchId the batch ID for this processing session
     * @return IngestResult containing counts of processed records
     */
    protected synchronized IngestResult processCSVFileWithDelta(Path file, Path processedDir, String batchId) {
        try {
            log.info("Processing file: {}", file);
            List<Map<String, String>> csvData = CsvUtils.readCsvFile(file, ',');
            
            List<Employee> employees = csvData.stream().map(csvRow -> {
                Employee emp = new Employee();
                props.getColumnMapping().forEach((csvCol, fieldName) -> {
                    String cellValue = csvRow.get(csvCol);
                    try {
                        EmployeeService.setFieldValue(emp, fieldName, cellValue, batchId);
                    } catch (Exception ex) {
                        log.warn("Failed to set field {} from column {} (batchId={})", fieldName, csvCol, batchId, ex);
                    }
                });
                // Set status from EmployeeCsvIngestProperties using utility method
                EmployeeService.updateEmployeeStatus(emp, props.getDefaultStatus());
                // Set batchId for this ingestion
                emp.setBatchId(batchId);
                return emp;
            }).toList();
            
            // Check for existing employees to avoid duplicate key constraint violations
            List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
            List<Long> existingIds = employeeRepository.findAllById(employeeIds).stream()
                    .map(Employee::getId).toList();
            
            // Filter out employees with existing IDs
            List<Employee> newEmployees = employees.stream()
                    .filter(emp -> !existingIds.contains(emp.getId()))
                    .toList();
            
            int newRecordsCount = 0;
            if (!newEmployees.isEmpty()) {
                employeeRepository.saveAll(newEmployees);
                newRecordsCount = newEmployees.size();
                log.info("Ingested {} new employees from file: {}", newRecordsCount, file);
            } else {
                log.info("No new employees to ingest from file: {}", file);
            }
            
            if (existingIds.size() > 0) {
                log.info("Skipped {} existing employees with IDs: {} from file: {}", 
                        existingIds.size(), existingIds, file);
            }
            
            // Create snapshots of all employees from this file (including existing ones for delta comparison)
            deltaService.createEmployeeSnapshots(employees, batchId);
            
            // Move the processed file
            EmployeeService.moveProcessedFile(file, processedDir);
            
            return new IngestResult(employees.size(), newRecordsCount);
            
        } catch (Exception ex) {
            log.error("Error ingesting employee CSV from file {}", file, ex);
            return new IngestResult(0, 0);
        }
    }
    
    /**
     * Performs delta detection and updates batch status.
     */
    private void performDeltaDetection(String batchId, int totalProcessed, int newRecordsCount) {
        try {
            log.info("Starting delta detection for batch: {}", batchId);
            
            // Detect and record deltas
            deltaService.detectAndRecordDeltas(batchId);
            
            // Get delta summary
            var summary = deltaService.getDeltaSummary(batchId);
            
            // Update batch with final results
            deltaService.updateIngestBatch(batchId, EmployeeIngestBatch.IngestStatus.COMPLETED,
                                         totalProcessed, newRecordsCount, summary.getUpdatedEmployees(), null);
            
            log.info("Delta detection completed for batch: {} - NEW: {}, UPDATED: {}, DELETED: {}", 
                    batchId, summary.getNewEmployees(), summary.getUpdatedEmployees(), summary.getDeletedEmployees());
            
        } catch (Exception e) {
            log.error("Error during delta detection for batch: {}", batchId, e);
            deltaService.updateIngestBatch(batchId, EmployeeIngestBatch.IngestStatus.FAILED,
                                         totalProcessed, newRecordsCount, 0, 
                                         "Delta detection failed: " + e.getMessage());
        }
    }

    /**
     * Legacy method for processing CSV files (deprecated - use processCSVFileWithDelta instead)
     * Processes a single CSV file and ingests employee records.
     * 
     * @param file the CSV file to process
     * @param processedDir the directory to move processed files to
     * @param batchId the batch ID for this processing session
     * @deprecated Use processCSVFileWithDelta for delta tracking support
     */
    @Deprecated
    protected synchronized void processCSVFile(Path file, Path processedDir, String batchId) {
        processCSVFileWithDelta(file, processedDir, batchId);
    }
}
