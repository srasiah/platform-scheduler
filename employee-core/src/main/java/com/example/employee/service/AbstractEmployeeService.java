package com.example.employee.service;

import com.example.employee.entity.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public abstract class AbstractEmployeeService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected String generateBatchId() {
        return UUID.randomUUID().toString();
    }

    protected void ensureDirectoryExists(Path dir) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            log.error("Failed to create directory: {}", dir, e);
        }
    }

    protected void updateEmployeeStatus(Employee emp, String status) {
        try {
            var statusField = emp.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(emp, status);
        } catch (Exception ex) {
            log.warn("Failed to update status field for employee {}", emp.getId(), ex);
        }
    }
}

