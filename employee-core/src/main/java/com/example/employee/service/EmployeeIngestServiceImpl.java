package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.config.EmployeeCsvIngestProperties;
import com.example.common.util.CsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeIngestServiceImpl extends AbstractEmployeeService implements EmployeeIngestService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeIngestServiceImpl.class);
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeeCsvIngestProperties props;

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
                        List<Employee> employees = records.stream().map(arr -> {
                            Employee emp = new Employee();
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
                                    log.warn("Failed to set field {} from column {} (batchId={})", fieldName, csvCol, batchId, ex);
                                }
                            });
                            // Set status from EmployeeCsvIngestProperties using abstract method
                            updateEmployeeStatus(emp, props.getDefaultStatus());
                            // Set batchId for this ingestion
                            emp.setBatchId(batchId);
                            return emp;
                        }).toList();
                        employeeRepository.saveAll(employees);
                        log.info("Ingested {} employees from file: {}", employees.size(), file);
                        // Move processed file
                        String originalName = file.getFileName().toString();
                        int dotIdx = originalName.lastIndexOf('.');
                        String base = (dotIdx > 0) ? originalName.substring(0, dotIdx) : originalName;
                        String ext = (dotIdx > 0) ? originalName.substring(dotIdx) : "";
                        String ts = "-" + System.currentTimeMillis();
                        String processedName = base + ts + ext;
                        Path target = processedDir.resolve(processedName);
                        try {
                            if (Files.exists(file)) {
                                try {
                                    Files.move(file, target);
                                    log.info("Moved processed file to {}", target);
                                } catch (java.nio.file.NoSuchFileException nsfe) {
                                    log.warn("Source file {} disappeared before move. Skipping move.", file);
                                }
                            } else {
                                log.warn("Source file {} does not exist at move time. Skipping move.", file);
                            }
                        } catch (Exception moveEx) {
                            log.error("Failed to move processed file to {}", target, moveEx);
                        }
                    } catch (Exception e) {
                        log.error("Error ingesting employee CSV from file {}", file, e);
                    }
                });
            log.info("EmployeeCsvIngestServiceImpl.ingestFromDirectory completed for ingest directory: {}", ingestDir);
        } catch (Exception e) {
            log.error("Error listing files in ingest directory {}", ingestDir, e);
        }
    }

    @Override
    public void ingestFromDirectory() {
        Path ingestDir = Path.of(props.getFileFolder());
        Path processedDir = Path.of(props.getProcessedFolder());
        ingestFromDirectory(ingestDir, processedDir);
    }
}
