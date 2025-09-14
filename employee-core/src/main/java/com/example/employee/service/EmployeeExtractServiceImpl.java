package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.config.EmployeeCsvExtractProperties;
import com.example.common.util.CsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeExtractServiceImpl implements EmployeeExtractService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeExtractServiceImpl.class);
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeeCsvExtractProperties props;

    @Override
    public void extractToDirectory(Path extractDir, String readyToExtractStatus) {
        log.info("Starting EmployeeExtractServiceImpl. Extract directory: {}", extractDir);
        try {
            if (!Files.exists(extractDir)) {
                Files.createDirectories(extractDir);
            }
        } catch (Exception e) {
            log.error("Failed to create extract folder: {}", extractDir, e);
        }
        log.info("Extracting employees with status: {}", readyToExtractStatus);
        List<Employee> employees = employeeRepository.findAll().stream()
            .filter(emp -> {
                try {
                    var statusField = emp.getClass().getDeclaredField("status");
                    statusField.setAccessible(true);
                    Object value = statusField.get(emp);
                    return value != null && value.toString().equalsIgnoreCase(readyToExtractStatus);
                } catch (Exception ex) {
                    log.warn("Failed to read status field for filtering", ex);
                    return false;
                }
            })
            .toList();
        if (employees.isEmpty()) {
            log.info("No employee records with status '{}' found in database. Skipping export process.", readyToExtractStatus);
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
                try {
                    var field = emp.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(emp);
                    row[i] = value != null ? value.toString() : "";
                } catch (Exception ex) {
                    log.warn("Failed to extract field {} for column {}", fieldName, header[i], ex);
                    row[i] = "";
                }
            }
            rows.add(row);
            // Update status to extracted
            try {
                var statusField = emp.getClass().getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(emp, props.getExtractedStatus());
            } catch (Exception ex) {
                log.warn("Failed to update status field for employee {}", emp.getId(), ex);
            }
        }
        Path outputFile = extractDir.resolve(props.getFileNamePrefix() + System.currentTimeMillis() + ".csv");
        try {
            CsvUtils.writeCsv(outputFile.toString(), rows);
            employeeRepository.saveAll(employees);
            log.info("Extracted {} employees to file: {}", employees.size(), outputFile);
        } catch (Exception e) {
            log.error("Failed to write extracted employees to file {}", outputFile, e);
        }
    }

    @Override
    public void extractToDirectory() {
        extractToDirectory(Path.of(props.getFileFolder()), props.getReadyToExtarctStatus());
    }
}
