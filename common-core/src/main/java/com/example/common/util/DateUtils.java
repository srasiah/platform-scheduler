package com.example.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for date parsing and formatting operations.
 */
public class DateUtils {
    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);
    
    /**
     * Common date formats supported for parsing.
     */
    private static final SimpleDateFormat[] COMMON_DATE_FORMATS = {
        new SimpleDateFormat("yyyy-MM-dd"),    // ISO format: 1990-05-15
        new SimpleDateFormat("MM/dd/yyyy"),    // US format: 05/15/1990  
        new SimpleDateFormat("M/d/yyyy"),      // US format with single digits: 5/15/1990
        new SimpleDateFormat("dd/MM/yyyy"),    // European format: 15/05/1990
        new SimpleDateFormat("d/M/yyyy"),      // European format with single digits: 15/5/1990
        new SimpleDateFormat("yyyy/MM/dd"),    // Alternative format: 1990/05/15
        new SimpleDateFormat("yyyy/M/d"),      // Alternative format with single digits: 1990/5/15
        new SimpleDateFormat("dd-MM-yyyy"),    // Dash European: 15-05-1990
        new SimpleDateFormat("d-M-yyyy"),      // Dash European with single digits: 15-5-1990
        new SimpleDateFormat("MM-dd-yyyy"),    // Dash US: 05-15-1990
        new SimpleDateFormat("M-d-yyyy")       // Dash US with single digits: 5-15-1990
    };
    
    /**
     * Parses a date string using multiple common date formats.
     * 
     * @param dateValue the date string to parse
     * @return parsed Date object, or null if parsing fails
     */
    public static Date parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        
        String trimmedValue = dateValue.trim();
        
        for (SimpleDateFormat formatter : COMMON_DATE_FORMATS) {
            try {
                return formatter.parse(trimmedValue);
            } catch (ParseException pe) {
                // Try next format
            }
        }
        
        log.warn("Could not parse date value '{}' using any of the supported formats", dateValue);
        return null;
    }
    
    /**
     * Parses a date string with detailed logging including context information.
     * 
     * @param dateValue the date string to parse
     * @param fieldName the name of the field being populated (for logging)
     * @param contextId optional context identifier for logging (e.g., batchId)
     * @return parsed Date object, or null if parsing fails
     */
    public static Date parseDate(String dateValue, String fieldName, String contextId) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        
        Date parsedDate = parseDate(dateValue);
        
        if (parsedDate == null) {
            if (contextId != null) {
                log.warn("Could not parse date value '{}' for field {} (context: {})", dateValue, fieldName, contextId);
            } else {
                log.warn("Could not parse date value '{}' for field {}", dateValue, fieldName);
            }
        }
        
        return parsedDate;
    }
    
    /**
     * Parses a date string using a specific date format.
     * 
     * @param dateValue the date string to parse
     * @param dateFormat the date format pattern (e.g., "yyyy-MM-dd", "MM/dd/yyyy")
     * @return parsed Date object, or null if parsing fails
     */
    public static Date parseDate(String dateValue, String dateFormat) {
        if (dateValue == null || dateValue.isBlank() || dateFormat == null || dateFormat.isBlank()) {
            return null;
        }
        
        String trimmedValue = dateValue.trim();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
            return formatter.parse(trimmedValue);
        } catch (ParseException | IllegalArgumentException e) {
            log.warn("Could not parse date value '{}' using format '{}'", dateValue, dateFormat);
            return null;
        }
    }
    
    /**
     * Parses a date string using a specific date format with detailed logging.
     * 
     * @param dateValue the date string to parse
     * @param dateFormat the date format pattern (e.g., "yyyy-MM-dd", "MM/dd/yyyy")
     * @param fieldName the name of the field being populated (for logging)
     * @param contextId optional context identifier for logging (e.g., batchId)
     * @return parsed Date object, or null if parsing fails
     */
    public static Date parseDate(String dateValue, String dateFormat, String fieldName, String contextId) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        
        Date parsedDate = parseDate(dateValue, dateFormat);
        
        if (parsedDate == null) {
            if (contextId != null) {
                log.warn("Could not parse date value '{}' using format '{}' for field {} (context: {})", 
                        dateValue, dateFormat, fieldName, contextId);
            } else {
                log.warn("Could not parse date value '{}' using format '{}' for field {}", 
                        dateValue, dateFormat, fieldName);
            }
        }
        
        return parsedDate;
    }
    
    /**
     * Parses a date string trying a preferred format first, then falling back to common formats.
     * 
     * @param dateValue the date string to parse
     * @param preferredFormat the preferred date format to try first
     * @return parsed Date object, or null if parsing fails with all formats
     */
    public static Date parseDateWithFallback(String dateValue, String preferredFormat) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        
        // Try preferred format first
        if (preferredFormat != null && !preferredFormat.isBlank()) {
            Date parsedDate = parseDate(dateValue, preferredFormat);
            if (parsedDate != null) {
                return parsedDate;
            }
        }
        
        // Fall back to common formats
        return parseDate(dateValue);
    }
    
    /**
     * Parses a date string trying a preferred format first, then falling back to common formats with logging.
     * 
     * @param dateValue the date string to parse
     * @param preferredFormat the preferred date format to try first
     * @param fieldName the name of the field being populated (for logging)
     * @param contextId optional context identifier for logging (e.g., batchId)
     * @return parsed Date object, or null if parsing fails with all formats
     */
    public static Date parseDateWithFallback(String dateValue, String preferredFormat, String fieldName, String contextId) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        
        Date parsedDate = parseDateWithFallback(dateValue, preferredFormat);
        
        if (parsedDate == null) {
            if (contextId != null) {
                log.warn("Could not parse date value '{}' using preferred format '{}' or any fallback formats for field {} (context: {})", 
                        dateValue, preferredFormat, fieldName, contextId);
            } else {
                log.warn("Could not parse date value '{}' using preferred format '{}' or any fallback formats for field {}", 
                        dateValue, preferredFormat, fieldName);
            }
        }
        
        return parsedDate;
    }
    
    /**
     * Gets a list of supported date format patterns.
     * 
     * @return array of supported date format patterns
     */
    public static String[] getSupportedDatePatterns() {
        String[] patterns = new String[COMMON_DATE_FORMATS.length];
        for (int i = 0; i < COMMON_DATE_FORMATS.length; i++) {
            patterns[i] = COMMON_DATE_FORMATS[i].toPattern();
        }
        return patterns;
    }
    
    /**
     * Formats a Date object to ISO format (yyyy-MM-dd).
     * 
     * @param date the date to format
     * @return formatted date string, or null if date is null
     */
    public static String formatToIsoDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
    
    /**
     * Formats a Date object using a specific format pattern.
     * 
     * @param date the date to format
     * @param formatPattern the format pattern (e.g., "yyyy-MM-dd", "MM/dd/yyyy")
     * @return formatted date string, or null if date is null or format is invalid
     */
    public static String formatDate(Date date, String formatPattern) {
        if (date == null || formatPattern == null || formatPattern.isBlank()) {
            return null;
        }
        
        try {
            return new SimpleDateFormat(formatPattern).format(date);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid date format pattern: '{}'", formatPattern);
            return null;
        }
    }
    
    /**
     * Validates if a date string can be parsed using a specific format.
     * 
     * @param dateValue the date string to validate
     * @param dateFormat the date format pattern
     * @return true if the date string can be parsed using the format, false otherwise
     */
    public static boolean isValidDate(String dateValue, String dateFormat) {
        if (dateValue == null || dateValue.isBlank() || dateFormat == null || dateFormat.isBlank()) {
            return false;
        }
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
            formatter.setLenient(false); // Strict parsing
            formatter.parse(dateValue.trim());
            return true;
        } catch (ParseException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Validates if a date string can be parsed using any of the common formats.
     * 
     * @param dateValue the date string to validate
     * @return true if the date string can be parsed, false otherwise
     */
    public static boolean isValidDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            return false;
        }
        
        return parseDate(dateValue) != null;
    }
}