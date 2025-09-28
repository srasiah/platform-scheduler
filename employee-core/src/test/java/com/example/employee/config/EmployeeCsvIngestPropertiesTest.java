package com.example.employee.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for EmployeeCsvIngestProperties to verify preferred date format functionality.
 */
public class EmployeeCsvIngestPropertiesTest {

    @Test
    @DisplayName("Should set and get preferredDateFormat property")
    void testPreferredDateFormatProperty() {
        EmployeeCsvIngestProperties properties = new EmployeeCsvIngestProperties();
        
        // Test default value (should be null)
        assertNull(properties.getPreferredDateFormat());
        
        // Test setting various date formats
        String format1 = "M/d/yyyy";
        properties.setPreferredDateFormat(format1);
        assertEquals(format1, properties.getPreferredDateFormat());
        
        String format2 = "yyyy-MM-dd";
        properties.setPreferredDateFormat(format2);
        assertEquals(format2, properties.getPreferredDateFormat());
        
        String format3 = "dd/MM/yyyy";
        properties.setPreferredDateFormat(format3);
        assertEquals(format3, properties.getPreferredDateFormat());
        
        // Test setting null
        properties.setPreferredDateFormat(null);
        assertNull(properties.getPreferredDateFormat());
        
        // Test setting empty string
        properties.setPreferredDateFormat("");
        assertEquals("", properties.getPreferredDateFormat());
    }
    
    @Test
    @DisplayName("Should inherit all properties from AbstractEmployeeCsvProperties")
    void testInheritance() {
        EmployeeCsvIngestProperties properties = new EmployeeCsvIngestProperties();
        
        // Test that it has inherited properties
        properties.setEnabled(true);
        assertTrue(properties.isEnabled());
        
        properties.setFileFolder("/test/folder");
        assertEquals("/test/folder", properties.getFileFolder());
        
        properties.setPreferredDateFormat("inherited-format");
        assertEquals("inherited-format", properties.getPreferredDateFormat());
        
        // Test that it has its own properties
        properties.setDefaultStatus("PROCESSING");
        assertEquals("PROCESSING", properties.getDefaultStatus());
        
        properties.setPreferredDateFormat("M/d/yyyy");
        assertEquals("M/d/yyyy", properties.getPreferredDateFormat());
    }
}