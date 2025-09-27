package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.common.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class providing common helper methods for employee services.
 * This class contains static utility methods that can be used across different employee services.
 */
public final class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    
    // Private constructor to prevent instantiation
    private EmployeeService() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a unique batch ID using UUID.
     * 
     * @return a unique batch ID string
     */
    public static String generateBatchId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Ensures that the specified directory exists, creating it if necessary.
     * 
     * @param dir the directory path to ensure exists
     */
    public static void ensureDirectoryExists(Path dir) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            log.error("Failed to create directory: {}", dir, e);
        }
    }

    /**
     * Updates the status of an employee if both employee and status are not null.
     * 
     * @param emp the employee to update
     * @param status the new status to set
     */
    public static void updateEmployeeStatus(Employee emp, String status) {
        if (emp != null && status != null) {
            emp.setStatus(status);
        }
    }

    /**
     * Gets a field value from an Employee object using proper getter methods.
     * Avoids reflection for better performance and maintainability.
     * 
     * @param emp the Employee object
     * @param fieldName the field name to get
     * @return the string representation of the field value, or null if not found
     */
    public static String getFieldValue(Employee emp, String fieldName) {
        if (emp == null || fieldName == null) {
            return null;
        }
        
        try {
            switch (fieldName.toLowerCase()) {
                case "id":
                    return emp.getId() != null ? emp.getId().toString() : null;
                case "name":
                    return emp.getName();
                case "age":
                    return emp.getAge() != null ? emp.getAge().toString() : null;
                case "status":
                    return emp.getStatus();
                case "dob":
                    return emp.getDob() != null ? emp.getDob().toString() : null;
                case "batchid":
                case "batch_id":
                    return emp.getBatchId();
                case "transactionid":
                case "transaction_id":
                    return emp.getTransactionId() != null ? emp.getTransactionId().toString() : null;
                case "createddate":
                case "created_date":
                    return emp.getCreatedDate() != null ? emp.getCreatedDate().toString() : null;
                default:
                    log.warn("Unknown field name: {}", fieldName);
                    return null;
            }
        } catch (Exception e) {
            log.warn("Failed to get field value for field '{}' from employee {}", fieldName, emp.getId(), e);
            return null;
        }
    }

    /**
     * Sets a field value on an Employee object using reflection.
     * Handles type conversion for different field types.
     * 
     * @param emp the Employee object
     * @param fieldName the field name to set
     * @param cellValue the string value from CSV
     * @param batchId the batch ID (for logging)
     */
    public static void setFieldValue(Employee emp, String fieldName, String cellValue, String batchId) {
        if (cellValue == null || cellValue.trim().isEmpty()) {
            return;
        }
        
        try {
            var field = emp.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            
            Class<?> fieldType = field.getType();
            Object value = null;
            
            if (fieldType == String.class) {
                value = cellValue.trim();
            } else if (fieldType == Long.class || fieldType == long.class) {
                value = Long.parseLong(cellValue.trim());
            } else if (fieldType == Integer.class || fieldType == int.class) {
                value = Integer.parseInt(cellValue.trim());
            } else if (fieldType == Date.class) {
                value = DateUtils.parseDateWithFallback(cellValue.trim(), "yyyy-MM-dd");
                if (value != null) {
                    log.debug("Parsed date '{}' to '{}' for field '{}' (batchId={})", 
                              cellValue, value, fieldName, batchId);
                }
            } else {
                log.warn("Unsupported field type {} for field {} (batchId={})", 
                         fieldType.getSimpleName(), fieldName, batchId);
                return;
            }
            
            field.set(emp, value);
        } catch (NoSuchFieldException e) {
            log.warn("Field '{}' not found on Employee class (batchId={})", fieldName, batchId);
        } catch (NumberFormatException e) {
            log.warn("Invalid number format '{}' for field '{}' (batchId={})", cellValue, fieldName, batchId);
        } catch (Exception e) {
            log.warn("Failed to set field '{}' with value '{}' (batchId={})", fieldName, cellValue, batchId, e);
        }
    }

    /**
     * Moves a processed CSV file to the processed directory with a timestamp suffix.
     * 
     * @param sourceFile   the source file to move
     * @param processedDir the target directory for processed files
     */
    public static void moveProcessedFile(Path sourceFile, Path processedDir) {
        String originalName = sourceFile.getFileName().toString();
        int dotIdx = originalName.lastIndexOf('.');
        String base = (dotIdx > 0) ? originalName.substring(0, dotIdx) : originalName;
        String ext = (dotIdx > 0) ? originalName.substring(dotIdx) : "";
        String ts = "-" + System.currentTimeMillis();
        String processedName = base + ts + ext;
        Path target = processedDir.resolve(processedName);

        try {
            if (Files.exists(sourceFile)) {
                try {
                    Files.move(sourceFile, target);
                    log.info("Moved processed file to {}", target);
                } catch (java.nio.file.NoSuchFileException nsfe) {
                    log.warn("Source file {} disappeared before move. Skipping move.", sourceFile);
                }
            } else {
                log.warn("Source file {} does not exist at move time. Skipping move.", sourceFile);
            }
        } catch (Exception moveEx) {
            log.error("Failed to move processed file to {}", target, moveEx);
        }
    }
}