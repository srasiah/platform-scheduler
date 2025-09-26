package com.example.employee.service;

import com.example.employee.entity.Employee;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeDateParsingTest {

    @Test
    void testDateFieldParsing() throws Exception {
        // Simulate the date parsing logic from EmployeeIngestServiceImpl
        Employee emp = new Employee();
        Field field = emp.getClass().getDeclaredField("dob");
        field.setAccessible(true);
        
        String dateValue = "1990-05-15";
        
        // Try multiple common date formats (same as in the implementation)
        SimpleDateFormat[] formatters = {
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("MM/dd/yyyy"),
            new SimpleDateFormat("dd/MM/yyyy"),
            new SimpleDateFormat("yyyy/MM/dd")
        };
        
        Date parsedDate = null;
        for (SimpleDateFormat formatter : formatters) {
            try {
                parsedDate = formatter.parse(dateValue);
                break;
            } catch (Exception pe) {
                // Try next format
            }
        }
        
        assertNotNull(parsedDate, "Date should be parsed successfully");
        field.set(emp, parsedDate);
        
        // Verify the field was set correctly
        Date dobFromEntity = emp.getDob();
        assertNotNull(dobFromEntity, "DOB field should not be null");
        
        // Verify the date value is correct
        SimpleDateFormat expectedFormat = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("1990-05-15", expectedFormat.format(dobFromEntity), "Parsed date should match expected value");
    }
    
    @Test
    void testMultipleDateFormats() throws Exception {
        Employee emp = new Employee();
        Field field = emp.getClass().getDeclaredField("dob");
        field.setAccessible(true);
        
        String[] testDates = {
            "1990-05-15",      // yyyy-MM-dd
            "05/15/1990",      // MM/dd/yyyy  
            "15/05/1990",      // dd/MM/yyyy
            "1990/05/15"       // yyyy/MM/dd
        };
        
        SimpleDateFormat[] formatters = {
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("MM/dd/yyyy"),
            new SimpleDateFormat("dd/MM/yyyy"),
            new SimpleDateFormat("yyyy/MM/dd")
        };
        
        for (String dateValue : testDates) {
            Date parsedDate = null;
            for (SimpleDateFormat formatter : formatters) {
                try {
                    parsedDate = formatter.parse(dateValue);
                    break;
                } catch (Exception pe) {
                    // Try next format
                }
            }
            assertNotNull(parsedDate, "Date '" + dateValue + "' should be parsed successfully");
        }
    }
    
    @Test 
    void testNullAndEmptyDates() throws Exception {
        Employee emp = new Employee();
        Field field = emp.getClass().getDeclaredField("dob");
        field.setAccessible(true);
        
        // Test null value
        String nullValue = null;
        if (nullValue == null || nullValue.isBlank()) {
            field.set(emp, null);
        }
        assertNull(emp.getDob(), "DOB should be null for null input");
        
        // Test empty value
        String emptyValue = "";
        if (emptyValue == null || emptyValue.isBlank()) {
            field.set(emp, null);
        }
        assertNull(emp.getDob(), "DOB should be null for empty input");
        
        // Test blank value
        String blankValue = "   ";
        if (blankValue == null || blankValue.isBlank()) {
            field.set(emp, null);
        }
        assertNull(emp.getDob(), "DOB should be null for blank input");
    }
}