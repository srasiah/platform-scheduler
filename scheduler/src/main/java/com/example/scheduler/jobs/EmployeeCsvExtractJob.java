package com.example.scheduler.jobs;

import com.example.scheduler.config.EmployeeCsvExtractProperties;
import com.example.persistence.entity.Employee;
import com.example.persistence.repo.EmployeeRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
public class EmployeeCsvExtractJob implements Job {
    @Autowired
    private EmployeeCsvExtractProperties props;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public void execute(JobExecutionContext context) {
        String filePath = Paths.get(props.getFileFolder(), props.getFileNamePrefix() + System.currentTimeMillis() + ".csv").toString();
        try (FileWriter writer = new FileWriter(filePath)) {
            Map<String, String> mapping = props.getColumnMapping();
            // Write header
            String header = String.join(",", mapping.keySet());
            writer.write(header + "\n");
            // Write data
            List<Employee> employees = employeeRepository.findAll();
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
                        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failed to get field {} for export", fieldName, ex);
                    }
                    if (value != null) {
                        row.append(value.toString().replaceAll(",", " "));
                    }
                    row.append(",");
                }
                // Remove trailing comma and add newline
                if (row.length() > 0) row.setLength(row.length() - 1);
                writer.write(row + "\n");
            }
            writer.flush();
            org.slf4j.LoggerFactory.getLogger(getClass()).info("Exported {} employees to {}", employees.size(), filePath);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error exporting employee CSV", e);
        }
    }
}
