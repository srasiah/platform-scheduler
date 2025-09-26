package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
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

    public EmployeeIngestServiceImpl(EmployeeRepository employeeRepository, EmployeeCsvIngestProperties props) {
        this.employeeRepository = employeeRepository;
        this.props = props;
    }

    @Override
    public void ingestFromDirectory(Path ingestDir, Path processedDir) {
        log.info("Starting EmployeeCsvIngestServiceImpl.ingestFromDirectory. Ingest directory: {}", ingestDir);
        String batchId = generateBatchId();
        try {
            if (!Files.exists(processedDir)) {
                Files.createDirectories(processedDir);
            }
        } catch (Exception e) {
            log.error("Failed to create processed folder: {}", processedDir, e);
        }
        try {
            Files.list(ingestDir)
                .filter(p -> p.getFileName().toString().startsWith(props.getFileNamePrefix()) && p.getFileName().toString().endsWith(".csv"))
                .forEach(file -> processCSVFile(file, processedDir, batchId));
            log.info("EmployeeCsvIngestServiceImpl.ingestFromDirectory completed for ingest directory: {}", ingestDir);
        } catch (Exception e) {
            log.error("Error listing files in ingest directory {}", ingestDir, e);
        }
    }

    /**
     * Processes a single CSV file and ingests employee records.
     * 
     * @param file the CSV file to process
     * @param processedDir the directory to move processed files to
     * @param batchId the batch ID for this processing session
     */
    protected synchronized void processCSVFile(Path file, Path processedDir, String batchId) {
        try {
            log.info("Processing file: {}", file);
            List<Map<String, String>> csvData = CsvUtils.readCsvFile(file, ',');
            
            List<Employee> employees = csvData.stream().map(csvRow -> {
                Employee emp = new Employee();
                props.getColumnMapping().forEach((csvCol, fieldName) -> {
                    String cellValue = csvRow.get(csvCol);
                    try {
                        setFieldValue(emp, fieldName, cellValue, batchId);
                    } catch (Exception ex) {
                        log.warn("Failed to set field {} from column {} (batchId={})", fieldName, csvCol, batchId, ex);
                    }
                });
                // Set status from EmployeeCsvIngestProperties using abstract method
                updateEmployeeStatus(emp, props.getDefaultStatus());
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
            
            if (!newEmployees.isEmpty()) {
                employeeRepository.saveAll(newEmployees);
                log.info("Ingested {} new employees from file: {}", newEmployees.size(), file);
            } else {
                log.info("No new employees to ingest from file: {}", file);
            }
            
            if (existingIds.size() > 0) {
                log.info("Skipped {} existing employees with IDs: {} from file: {}", 
                        existingIds.size(), existingIds, file);
            }
            
            // Move the processed file
            moveProcessedFile(file, processedDir);
        } catch (Exception ex) {
            log.error("Error ingesting employee CSV from file {}", file, ex);
        }
    }

    
    @Override
    public void ingestFromDirectory() {
        Path ingestDir = Path.of(props.getFileFolder());
        Path processedDir = Path.of(props.getProcessedFolder());
        ingestFromDirectory(ingestDir, processedDir);
    }
}
