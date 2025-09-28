package com.example.employee.service.impl;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.service.EmployeeExtractService;
import com.example.employee.service.EmployeeService;
import com.example.employee.service.base.AbstractEmployeeService;
import com.example.employee.config.EmployeeCsvExtractProperties;
import com.example.common.util.CsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeExtractServiceImpl extends AbstractEmployeeService implements EmployeeExtractService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeExtractServiceImpl.class);
    private final EmployeeRepository employeeRepository;
    private final EmployeeCsvExtractProperties props;

    public EmployeeExtractServiceImpl(EmployeeRepository employeeRepository, EmployeeCsvExtractProperties props) {
        this.employeeRepository = employeeRepository;
        this.props = props;
    }

    @Override
    public synchronized void extractToDirectory(Path extractDir, String readyToExtractStatus) {
        processFromDirectory(extractDir, readyToExtractStatus);
    }

    @Override
    public void extractToDirectory() {
        processFromDirectory();
    }

    @Override
    public void processFromDirectory(Path extractDir, Path targetDir) {
        // For extract service, the targetDir parameter is not used since we don't move files
        // Use the configured ready status for extraction
        extractToDirectory(extractDir, props.getReadyToExtractStatus());
    }

    @Override
    public void processFromDirectory() {
        extractToDirectory(Path.of(props.getFileFolder()), props.getReadyToExtractStatus());
    }

    @Override
    public boolean isReadyForProcessing() {
        try {
            // Check if properties are properly configured
            if (props == null || props.getFileFolder() == null) {
                log.warn("Employee extract service not properly configured - missing properties");
                return false;
            }
            
            // Check if required directory exists or can be created
            Path extractDir = Path.of(props.getFileFolder());
            EmployeeService.ensureDirectoryExists(extractDir);
            
            // Check repository connectivity
            if (employeeRepository == null) {
                log.warn("Employee extract service not ready - repository not available");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error checking if employee extract service is ready for processing", e);
            return false;
        }
    }

    @Override
    public String getServiceDescription() {
        return "Employee CSV Extract Service - Exports employee data to CSV files";
    }

    /**
     * Internal method that performs the actual extraction logic
     */
    private synchronized void processFromDirectory(Path extractDir, String readyToExtractStatus) {
        String batchId = EmployeeService.generateBatchId();
        log.info("Starting EmployeeExtractServiceImpl. batchId={}, Extract directory: {}", batchId, extractDir);
        EmployeeService.ensureDirectoryExists(extractDir);
        log.info("Extracting employees with status: {} (batchId={})", readyToExtractStatus, batchId);
        
        // Use repository query instead of filtering in memory
        List<Employee> employees = employeeRepository.findByStatus(readyToExtractStatus);
        
        if (employees.isEmpty()) {
            log.info("No employee records with status '{}' found in database. Skipping export process. (batchId={})", readyToExtractStatus, batchId);
            return;
        }
        
        // Prepare data for CSV
        Map<String, String> mapping = props.getColumnMapping();
        String[] header = mapping.keySet().toArray(new String[0]);
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(header);
        
        for (Employee emp : employees) {
            String[] row = new String[header.length];
            for (int i = 0; i < header.length; i++) {
                String fieldName = mapping.get(header[i]);
                String value = EmployeeService.getFieldValue(emp, fieldName);
                row[i] = value != null ? value : "";
            }
            rows.add(row);
            // Update status to extracted using utility method
            EmployeeService.updateEmployeeStatus(emp, props.getExtractedStatus());
        }
        
        Path outputFile = extractDir.resolve(props.getFileNamePrefix() + batchId + "-" + System.currentTimeMillis() + ".csv");
        try {
            CsvUtils.writeCsv(outputFile.toString(), rows);
            employeeRepository.saveAll(employees);
            log.info("Extracted {} employees to file: {} (batchId={})", employees.size(), outputFile, batchId);
        } catch (Exception e) {
            log.error("Failed to write extracted employees to file {} (batchId={})", outputFile, batchId, e);
        }
    }
}
