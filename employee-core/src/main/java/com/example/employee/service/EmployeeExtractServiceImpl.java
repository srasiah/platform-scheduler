package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
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
        String batchId = generateBatchId();
        log.info("Starting EmployeeExtractServiceImpl. batchId={}, Extract directory: {}", batchId, extractDir);
        ensureDirectoryExists(extractDir);
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
                String value = getFieldValue(emp, fieldName);
                row[i] = value != null ? value : "";
            }
            rows.add(row);
            // Update status to extracted using abstract method
            updateEmployeeStatus(emp, props.getExtractedStatus());
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

    @Override
    public void extractToDirectory() {
        extractToDirectory(Path.of(props.getFileFolder()), props.getReadyToExtractStatus());
    }
}
