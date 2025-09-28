package com.example.employee.service;

import com.example.employee.entity.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

/**
 * Test for EmployeeService setFieldValue method with preferred date format.
 */
public class EmployeeServicePreferredDateFormatTest {

    @Test
    @DisplayName("Should use preferredDateFormat when parsing dates")
    void testSetFieldValueWithPreferredDateFormat() {
        Employee employee = new Employee();
        String batchId = "test-batch-001";
        
        // Test with M/d/yyyy preferred format
        String preferredDateFormat = "M/d/yyyy";
        String dateValue = "5/12/1967";
        
        EmployeeService.setFieldValue(employee, "dob", dateValue, batchId, preferredDateFormat);
        
        assertNotNull(employee.getDob());
        // Verify the date was parsed correctly (May 12, 1967)
        Date parsedDate = employee.getDob();
        assertNotNull(parsedDate);
        
        // Test with different format
        Employee employee2 = new Employee();
        String preferredDateFormat2 = "yyyy-MM-dd";
        String dateValue2 = "1967-05-12";
        
        EmployeeService.setFieldValue(employee2, "dob", dateValue2, batchId, preferredDateFormat2);
        
        assertNotNull(employee2.getDob());
        Date parsedDate2 = employee2.getDob();
        assertNotNull(parsedDate2);
        
        // Both dates should represent the same moment (May 12, 1967)
        assertEquals(parsedDate.getTime(), parsedDate2.getTime());
    }
    
    @Test
    @DisplayName("Should fallback to DateUtils default when preferredDateFormat fails")
    void testFallbackWhenPreferredFormatFails() {
        Employee employee = new Employee();
        String batchId = "test-batch-002";
        
        // Use M/d/yyyy as preferred but provide yyyy-MM-dd format data
        String preferredDateFormat = "M/d/yyyy";
        String dateValue = "1967-05-12"; // This won't match M/d/yyyy format
        
        EmployeeService.setFieldValue(employee, "dob", dateValue, batchId, preferredDateFormat);
        
        // Should still parse using DateUtils fallback mechanism
        assertNotNull(employee.getDob());
    }
    
    @Test
    @DisplayName("Should use default fallback when preferredDateFormat is null")
    void testNullPreferredDateFormat() {
        Employee employee = new Employee();
        String batchId = "test-batch-003";
        String dateValue = "1967-05-12";
        
        EmployeeService.setFieldValue(employee, "dob", dateValue, batchId, null);
        
        assertNotNull(employee.getDob());
    }
    
    @Test
    @DisplayName("Should use default fallback when preferredDateFormat is empty")
    void testEmptyPreferredDateFormat() {
        Employee employee = new Employee();
        String batchId = "test-batch-004";
        String dateValue = "1967-05-12";
        
        EmployeeService.setFieldValue(employee, "dob", dateValue, batchId, "");
        
        assertNotNull(employee.getDob());
    }
    
    @Test
    @DisplayName("Should maintain backward compatibility with original method")
    void testBackwardCompatibility() {
        Employee employee = new Employee();
        String batchId = "test-batch-005";
        String dateValue = "1967-05-12";
        
        // Test original method (without preferredDateFormat parameter)
        EmployeeService.setFieldValue(employee, "dob", dateValue, batchId);
        
        assertNotNull(employee.getDob());
        
        // Test that it gives same result as new method with null preferredDateFormat
        Employee employee2 = new Employee();
        EmployeeService.setFieldValue(employee2, "dob", dateValue, batchId, null);
        
        assertEquals(employee.getDob().getTime(), employee2.getDob().getTime());
    }
}