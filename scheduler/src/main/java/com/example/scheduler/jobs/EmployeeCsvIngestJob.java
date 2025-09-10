package com.example.scheduler.jobs;

import com.example.scheduler.config.EmployeeCsvIngestProperties;
import com.example.common.util.CsvUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmployeeCsvIngestJob implements Job {
    @Autowired
    private EmployeeCsvIngestProperties props;
    @Autowired
    private com.example.persistence.repo.EmployeeRepository employeeRepository;

    @Override
    public void execute(JobExecutionContext context) {
        String filePath = Paths.get(props.getFileFolder(), props.getFileNamePrefix()).toString();
        try {
            List<String[]> records = CsvUtils.readCsv(filePath);
            if (records.isEmpty()) return;
            String[] header = records.get(0);
            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colIdx.put(header[i], i);
            }
            Map<String, String> mapping = props.getColumnMapping();
            // Skip header row
            records = records.subList(1, records.size());
            List<com.example.persistence.entity.Employee> employees = records.stream().map(arr -> {
                com.example.persistence.entity.Employee emp = new com.example.persistence.entity.Employee();
                mapping.forEach((csvCol, fieldName) -> {
                    Integer idx = colIdx.get(csvCol);
                    if (idx == null || idx >= arr.length) return;
                    String value = arr[idx];
                    try {
                        var field = emp.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        if (field.getType().equals(Long.class)) {
                            field.set(emp, value == null || value.isBlank() ? null : Long.valueOf(value));
                        } else if (field.getType().equals(Integer.class)) {
                            field.set(emp, value == null || value.isBlank() ? null : Integer.valueOf(value));
                        } else {
                            field.set(emp, value);
                        }
                    } catch (Exception ex) {
                        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failed to set field {} from column {}", fieldName, csvCol, ex);
                    }
                });
                return emp;
            }).toList();
            employeeRepository.saveAll(employees);
        } catch (Exception e) {
            // log error
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error ingesting employee CSV", e);
        }
    }
}
