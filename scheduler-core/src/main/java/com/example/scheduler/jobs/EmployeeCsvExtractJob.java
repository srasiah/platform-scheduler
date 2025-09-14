package com.example.scheduler.jobs;

import com.example.scheduler.config.EmployeeCsvExtractProperties;
import com.example.persistence.entity.Employee;
import com.example.persistence.repo.EmployeeRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component

public class EmployeeCsvExtractJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvExtractJob.class);
    @Autowired
    private EmployeeCsvExtractProperties props;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public void execute(JobExecutionContext context) {
        // Log job start and extract directory
        log.info("Starting EmployeeCsvExtractJob. Extract directory: {}", props.getFileFolder());

        // Only export employees with the desired status
        String statusToExtract = props.getExtractStatus();
        log.info("Extracting employees with status: {}", statusToExtract);
        List<Employee> employees = employeeRepository.findAll().stream()
            .filter(emp -> {
                try {
                    var statusField = emp.getClass().getDeclaredField("status");
                    statusField.setAccessible(true);
                    Object value = statusField.get(emp);
                    return value != null && value.toString().equalsIgnoreCase(statusToExtract);
                } catch (Exception ex) {
                    log.warn("Failed to read status field for filtering", ex);
                    return false;
                }
            })
            .toList();
        if (employees.isEmpty()) {
            log.info("No employee records with status '{}' found in database. Skipping export process.", statusToExtract);
            log.info("EmployeeCsvExtractJob completed for extract directory: {} (no employees)", props.getFileFolder());
            return;
        }

        String fileName = props.getFileNamePrefix() + System.currentTimeMillis() + ".csv";
        String filePath = Paths.get(props.getFileFolder(), fileName).toString();
        Path processedDir = Paths.get(props.getProcessedFolder());
        log.info("Exporting {} employees to file: {}", employees.size(), filePath);
        try {
            if (!Files.exists(processedDir)) {
                Files.createDirectories(processedDir);
            }
        } catch (Exception e) {
            log.error("Failed to create processed folder: {}", processedDir, e);
        }

        log.info("Starting file export process for: {}", filePath);
        try (FileWriter writer = new FileWriter(filePath)) {
            Map<String, String> mapping = props.getColumnMapping();
            // Write header
            String header = String.join(",", mapping.keySet());
            writer.write(header + "\n");
            // Write data
            for (Employee emp : employees) {
                StringBuilder row = new StringBuilder();
                for (String csvCol : mapping.keySet()) {
                    String fieldName = mapping.get(csvCol);
                    Object value = null;
                    try {
                        var field = emp.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        value = field.get(emp);
                    } catch (Exception ex) {
                        log.warn("Failed to get field {} for CSV export", fieldName, ex);
                    }
                    row.append(value != null ? value.toString() : "");
                    row.append(",");
                }
                if (row.length() > 0) row.setLength(row.length() - 1); // remove trailing comma
                writer.write(row.toString());
                writer.write("\n");
            }
            writer.flush();
            log.info("Exported {} employees to {}", employees.size(), filePath);

            // Update status of exported employees
            try {
                for (Employee emp : employees) {
                    var statusField = emp.getClass().getDeclaredField("status");
                    statusField.setAccessible(true);
                    // Use getStatusAfterExtract if available, else fallback to getStatus
                    String newStatus = null;
                    try {
                        newStatus = (String) EmployeeCsvExtractProperties.class.getMethod("getStatusAfterExtract").invoke(props);
                    } catch (NoSuchMethodException nsme) {
                        newStatus = props.getStatus();
                    }
                    statusField.set(emp, newStatus);
                }
                employeeRepository.saveAll(employees);
                log.info("Updated status for {} employees after export.", employees.size());
            } catch (Exception ex) {
                log.warn("Failed to update status after export", ex);
            }

            // Move file to processed folder
            Path target = Paths.get(processedDir.toString(), fileName);
            try {
                Path source = Paths.get(filePath);
                if (Files.exists(source)) {
                    Files.move(source, target);
                    log.info("Moved extracted file to {}", target);
                } else {
                    log.warn("Source file {} does not exist at move time. Skipping move.", source);
                }
            } catch (Exception moveEx) {
                log.error("Failed to move extracted file to {}", target, moveEx);
            }
            log.info("EmployeeCsvExtractJob completed for extract directory: {}", props.getFileFolder());
        } catch (Exception e) {
            log.error("Error during employee CSV extraction", e);
        }
    // ...existing code up to the first try/catch block...
    }
}
