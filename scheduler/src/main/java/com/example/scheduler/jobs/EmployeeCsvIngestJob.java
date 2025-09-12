package com.example.scheduler.jobs;

import com.example.scheduler.config.EmployeeCsvIngestProperties;
import com.example.common.util.CsvUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;   
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@DisallowConcurrentExecution
public class EmployeeCsvIngestJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvIngestJob.class);
    @Autowired
    private EmployeeCsvIngestProperties props;
    @Autowired
    private com.example.persistence.repo.EmployeeRepository employeeRepository;

    @Override
    public void execute(JobExecutionContext context) {
        Path ingestDir = Paths.get(props.getFileFolder());
        Path processedDir = Paths.get(props.getProcessedFolder());
        log.info("Starting EmployeeCsvIngestJob. Ingest directory: {}", ingestDir);
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
                .forEach(file -> {
                    try {
                        log.info("Processing file: {}", file);
                        List<String[]> records = CsvUtils.readCsv(file.toString());
                        if (records.isEmpty()) {
                            log.info("No records found in file: {}. Skipping.", file);
                            return;
                        }
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
                                    log.warn("Failed to set field {} from column {}", fieldName, csvCol, ex);
                                }
                            });
                            // Set status from EmployeeCsvIngestProperties
                            try {
                                var statusField = emp.getClass().getDeclaredField("status");
                                statusField.setAccessible(true);
                                statusField.set(emp, props.getStatus());
                            } catch (Exception ex) {
                                log.warn("Failed to set status field from EmployeeCsvIngestProperties", ex);
                            }
                            return emp;
                        }).toList();
                        employeeRepository.saveAll(employees);
                        log.info("Ingested {} employees from file: {}", employees.size(), file);
                        // Move processed file
                        Path source = file;
                        String originalName = source.getFileName().toString();
                        int dotIdx = originalName.lastIndexOf('.');
                        String base = (dotIdx > 0) ? originalName.substring(0, dotIdx) : originalName;
                        String ext = (dotIdx > 0) ? originalName.substring(dotIdx) : "";
                        String ts = "-" + System.currentTimeMillis();
                        String processedName = base + ts + ext;
                        Path target = processedDir.resolve(processedName);
                        try {
                            if (Files.exists(source)) {
                                try {
                                    Files.move(source, target);
                                    log.info("Moved processed file to {}", target);
                                } catch (java.nio.file.NoSuchFileException nsfe) {
                                    log.warn("Source file {} disappeared before move. Skipping move.", source);
                                }
                            } else {
                                log.warn("Source file {} does not exist at move time. Skipping move.", source);
                            }
                        } catch (Exception moveEx) {
                            log.error("Failed to move processed file to {}", target, moveEx);
                        }
                    } catch (Exception e) {
                        // log error
                        log.error("Error ingesting employee CSV from file {}", file, e);
                    }
                });
            log.info("EmployeeCsvIngestJob completed for ingest directory: {}", ingestDir);
        } catch (Exception e) {
            log.error("Error listing files in ingest directory {}", ingestDir, e);
        }
    }
}
