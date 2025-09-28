package com.example.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Date;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DateUtils utility class.
 */
class DateUtilsTest {

    @Nested
    @DisplayName("parseDate(String) tests")
    class ParseDateBasicTests {

        @Test
        @DisplayName("Should parse valid ISO date format")
        void shouldParseIsoDateFormat() {
            Date result = DateUtils.parseDate("2023-05-15");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should parse ISO date format")
        void shouldParseIsoDateFormatParameterized() {
            Date result = DateUtils.parseDate("2023-05-15");
            assertNotNull(result);
            
            // Verify the date was parsed correctly
            String formatted = DateUtils.formatToIsoDate(result);
            assertEquals("2023-05-15", formatted);
        }

        @Test
        @DisplayName("Should parse US date format")
        void shouldParseUsDateFormat() {
            Date result = DateUtils.parseDate("05/15/2023");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should parse European date format with unambiguous dates")
        void shouldParseEuropeanDateFormat() {
            // Use a specific format test instead of relying on fallback behavior
            Date result = DateUtils.parseDate("25/12/2023", "dd/MM/yyyy"); 
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
            assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test 
        @DisplayName("Should parse various date formats correctly")
        void shouldParseVariousDateFormats() {
            // Test each format individually with specific format strings to avoid ambiguity
            
            // ISO format - this should always work
            Date iso = DateUtils.parseDate("2023-12-25");
            assertNotNull(iso);
            assertEquals("2023-12-25", DateUtils.formatToIsoDate(iso));
            
            // Test specific format parsing
            Date specific1 = DateUtils.parseDate("25/12/2023", "dd/MM/yyyy");
            assertNotNull(specific1);
            
            Date specific2 = DateUtils.parseDate("12/25/2023", "MM/dd/yyyy");  
            assertNotNull(specific2);
            
            // Both should represent December 25, 2023
            assertEquals("2023-12-25", DateUtils.formatToIsoDate(specific1));
            assertEquals("2023-12-25", DateUtils.formatToIsoDate(specific2));
        }

        @Test
        @DisplayName("Should parse single-digit month and day formats M/d/yyyy")
        void shouldParseSingleDigitMonthDayFormats() {
            // Test M/d/yyyy format with single digit month and day
            Date result1 = DateUtils.parseDate("5/7/2023");
            assertNotNull(result1);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result1);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
            
            // Test with mixed single and double digits
            Date result2 = DateUtils.parseDate("12/5/2023");
            assertNotNull(result2);
            
            cal.setTime(result2);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
            assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should parse European single-digit format d/M/yyyy")
        void shouldParseEuropeanSingleDigitFormat() {
            // Test d/M/yyyy format - use day > 12 to make it unambiguous
            Date result = DateUtils.parseDate("25/5/2023", "d/M/yyyy");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should parse alternative single-digit formats")
        void shouldParseAlternativeSingleDigitFormats() {
            // Test yyyy/M/d format
            Date result1 = DateUtils.parseDate("2023/5/15", "yyyy/M/d");
            assertNotNull(result1);
            assertEquals("2023-05-15", DateUtils.formatToIsoDate(result1));
            
            // Test M-d-yyyy format
            Date result2 = DateUtils.parseDate("5-15-2023", "M-d-yyyy");
            assertNotNull(result2);
            assertEquals("2023-05-15", DateUtils.formatToIsoDate(result2));
            
            // Test d-M-yyyy format
            Date result3 = DateUtils.parseDate("15-5-2023", "d-M-yyyy");
            assertNotNull(result3);
            assertEquals("2023-05-15", DateUtils.formatToIsoDate(result3));
        }

        @ParameterizedTest
        @DisplayName("Should return null for invalid inputs")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        void shouldReturnNullForInvalidInputs(String dateValue) {
            Date result = DateUtils.parseDate(dateValue);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for unparseable date")
        void shouldReturnNullForUnparseableDate() {
            Date result = DateUtils.parseDate("invalid-date");
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle date with leading/trailing whitespace")
        void shouldHandleWhitespace() {
            Date result = DateUtils.parseDate("  2023-05-15  ");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    @Nested
    @DisplayName("parseDate(String, String, String) tests")
    class ParseDateWithContextTests {

        @Test
        @DisplayName("Should parse date with context logging")
        void shouldParseDateWithContext() {
            Date result = DateUtils.parseDate("2023-05-15", "birthDate", "employee123");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
        }

        @Test
        @DisplayName("Should handle null context gracefully")
        void shouldHandleNullContext() {
            Date result = DateUtils.parseDate("2023-05-15", "birthDate", null);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return null for invalid date with context")
        void shouldReturnNullForInvalidDateWithContext() {
            Date result = DateUtils.parseDate("invalid-date", "birthDate", "context");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("parseDate(String, String) specific format tests")
    class ParseDateSpecificFormatTests {

        @Test
        @DisplayName("Should parse date with specific format")
        void shouldParseDateWithSpecificFormat() {
            Date result = DateUtils.parseDate("15-May-2023", "dd-MMM-yyyy");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should return null for wrong format")
        void shouldReturnNullForWrongFormat() {
            Date result = DateUtils.parseDate("2023-05-15", "MM/dd/yyyy");
            assertNull(result);
        }

        @ParameterizedTest
        @DisplayName("Should return null for null or blank inputs")
        @CsvSource({
            ", yyyy-MM-dd",
            "'', yyyy-MM-dd", 
            "2023-05-15, ",
            "2023-05-15, ''"
        })
        void shouldReturnNullForNullOrBlankInputs(String dateValue, String dateFormat) {
            Date result = DateUtils.parseDate(dateValue, dateFormat);
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle invalid format pattern")
        void shouldHandleInvalidFormatPattern() {
            // The DateUtils.parseDate method should handle IllegalArgumentException
            // and return null for invalid format patterns
            Date result = DateUtils.parseDate("2023-05-15", "invalid-format");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("parseDate(String, String, String, String) specific format with context tests")
    class ParseDateSpecificFormatWithContextTests {

        @Test
        @DisplayName("Should parse date with specific format and context")
        void shouldParseDateWithSpecificFormatAndContext() {
            Date result = DateUtils.parseDate("15-May-2023", "dd-MMM-yyyy", "startDate", "project456");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle null context in specific format parsing")
        void shouldHandleNullContextInSpecificFormatParsing() {
            Date result = DateUtils.parseDate("2023-05-15", "yyyy-MM-dd", "endDate", null);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("parseDateWithFallback tests")
    class ParseDateWithFallbackTests {

        @Test
        @DisplayName("Should use preferred format successfully")
        void shouldUsePreferredFormatSuccessfully() {
            Date result = DateUtils.parseDateWithFallback("15-May-2023", "dd-MMM-yyyy");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
        }

        @Test
        @DisplayName("Should fallback to common formats when preferred fails")
        void shouldFallbackToCommonFormats() {
            Date result = DateUtils.parseDateWithFallback("2023-05-15", "MM/dd/yyyy");
            assertNotNull(result, "Should fallback to ISO format");
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
        }

        @Test
        @DisplayName("Should handle null preferred format")
        void shouldHandleNullPreferredFormat() {
            Date result = DateUtils.parseDateWithFallback("2023-05-15", null);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return null when all formats fail")
        void shouldReturnNullWhenAllFormatsFail() {
            Date result = DateUtils.parseDateWithFallback("completely-invalid", "dd-MMM-yyyy");
            assertNull(result);
        }

        @Test
        @DisplayName("Should fallback to single-digit formats")
        void shouldFallbackToSingleDigitFormats() {
            // Should fallback from preferred format to single-digit formats
            Date result = DateUtils.parseDateWithFallback("5/7/2023", "yyyy-MM-dd");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
            assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    @Nested
    @DisplayName("getSupportedDatePatterns tests")
    class GetSupportedDatePatternsTests {

        @Test
        @DisplayName("Should return all supported date patterns")
        void shouldReturnAllSupportedDatePatterns() {
            String[] patterns = DateUtils.getSupportedDatePatterns();
            
            assertNotNull(patterns);
            assertEquals(11, patterns.length); // Updated count for new formats
            
            // Verify expected patterns are present
            assertArrayEquals(new String[]{
                "yyyy-MM-dd",
                "MM/dd/yyyy", 
                "M/d/yyyy",    // Added single-digit US format
                "dd/MM/yyyy",
                "d/M/yyyy",    // Added single-digit European format
                "yyyy/MM/dd",
                "yyyy/M/d",    // Added single-digit alternative format
                "dd-MM-yyyy",
                "d-M-yyyy",    // Added single-digit dash European format
                "MM-dd-yyyy",
                "M-d-yyyy"     // Added single-digit dash US format
            }, patterns);
        }
    }

    @Nested
    @DisplayName("formatToIsoDate tests")
    class FormatToIsoDateTests {

        @Test
        @DisplayName("Should format date to ISO format")
        void shouldFormatDateToIsoFormat() {
            Calendar cal = Calendar.getInstance();
            cal.set(2023, Calendar.MAY, 15, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date date = cal.getTime();
            
            String result = DateUtils.formatToIsoDate(date);
            assertEquals("2023-05-15", result);
        }

        @Test
        @DisplayName("Should return null for null date")
        void shouldReturnNullForNullDate() {
            String result = DateUtils.formatToIsoDate(null);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("formatDate tests")
    class FormatDateTests {

        @Test
        @DisplayName("Should format date with custom pattern")
        void shouldFormatDateWithCustomPattern() {
            Calendar cal = Calendar.getInstance();
            cal.set(2023, Calendar.MAY, 15, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date date = cal.getTime();
            
            String result = DateUtils.formatDate(date, "dd/MM/yyyy");
            assertEquals("15/05/2023", result);
        }

        @Test
        @DisplayName("Should return null for null date")
        void shouldReturnNullForNullDateInCustomFormat() {
            String result = DateUtils.formatDate(null, "yyyy-MM-dd");
            assertNull(result);
        }

        @ParameterizedTest
        @DisplayName("Should return null for invalid format pattern")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void shouldReturnNullForInvalidFormatPattern(String formatPattern) {
            Calendar cal = Calendar.getInstance();
            cal.set(2023, Calendar.MAY, 15);
            Date date = cal.getTime();
            
            String result = DateUtils.formatDate(date, formatPattern);
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle invalid pattern gracefully")
        void shouldHandleInvalidPatternGracefully() {
            Calendar cal = Calendar.getInstance();
            cal.set(2023, Calendar.MAY, 15);
            Date date = cal.getTime();
            
            String result = DateUtils.formatDate(date, "invalid-pattern");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("isValidDate tests")
    class IsValidDateTests {

        @Test
        @DisplayName("Should validate date with specific format")
        void shouldValidateDateWithSpecificFormat() {
            assertTrue(DateUtils.isValidDate("2023-05-15", "yyyy-MM-dd"));
            assertTrue(DateUtils.isValidDate("15/05/2023", "dd/MM/yyyy"));
            assertFalse(DateUtils.isValidDate("2023-05-15", "MM/dd/yyyy"));
        }

        @Test
        @DisplayName("Should validate strict date parsing")
        void shouldValidateStrictDateParsing() {
            // Invalid dates should return false with strict parsing
            assertFalse(DateUtils.isValidDate("2023-02-30", "yyyy-MM-dd")); // Invalid date
            assertFalse(DateUtils.isValidDate("2023-13-15", "yyyy-MM-dd")); // Invalid month
        }

        @ParameterizedTest
        @DisplayName("Should return false for null or blank inputs")
        @CsvSource({
            ", yyyy-MM-dd",
            "'', yyyy-MM-dd",
            "2023-05-15, ",
            "2023-05-15, ''"
        })
        void shouldReturnFalseForNullOrBlankInputsInValidation(String dateValue, String dateFormat) {
            assertFalse(DateUtils.isValidDate(dateValue, dateFormat));
        }

        @Test
        @DisplayName("Should validate date using any common format")
        void shouldValidateDateUsingAnyCommonFormat() {
            assertTrue(DateUtils.isValidDate("2023-05-15"));
            assertTrue(DateUtils.isValidDate("05/15/2023"));
            assertTrue(DateUtils.isValidDate("5/15/2023"));    // Single-digit month
            assertTrue(DateUtils.isValidDate("15/05/2023"));
            assertTrue(DateUtils.isValidDate("15/5/2023"));    // Single-digit month
            assertFalse(DateUtils.isValidDate("invalid-date"));
        }

        @Test
        @DisplayName("Should validate single-digit date formats")
        void shouldValidateSingleDigitDateFormats() {
            // M/d/yyyy format - can handle both single and double digits
            assertTrue(DateUtils.isValidDate("5/7/2023", "M/d/yyyy"));
            assertTrue(DateUtils.isValidDate("12/5/2023", "M/d/yyyy"));
            assertTrue(DateUtils.isValidDate("05/07/2023", "M/d/yyyy")); // Also works with double digits
            
            // d/M/yyyy format  
            assertTrue(DateUtils.isValidDate("7/5/2023", "d/M/yyyy"));
            assertTrue(DateUtils.isValidDate("25/12/2023", "d/M/yyyy"));
            
            // Mixed formats should work
            assertTrue(DateUtils.isValidDate("5/15/2023", "M/d/yyyy"));  // Single month, double day
            assertTrue(DateUtils.isValidDate("15/5/2023", "d/M/yyyy"));  // Double day, single month
            
            // Invalid dates should still fail
            assertFalse(DateUtils.isValidDate("13/7/2023", "M/d/yyyy"));  // Invalid month
            assertFalse(DateUtils.isValidDate("7/32/2023", "M/d/yyyy"));  // Invalid day
        }

        @ParameterizedTest
        @DisplayName("Should return false for null or blank date values")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void shouldReturnFalseForNullOrBlankDateValues(String dateValue) {
            assertFalse(DateUtils.isValidDate(dateValue));
        }
    }

    @Nested
    @DisplayName("Edge cases and boundary tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle leap year dates")
        void shouldHandleLeapYearDates() {
            Date result = DateUtils.parseDate("2024-02-29"); // Leap year
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2024, cal.get(Calendar.YEAR));
            assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
            assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should handle year boundaries")
        void shouldHandleYearBoundaries() {
            Date result1 = DateUtils.parseDate("1900-01-01");
            Date result2 = DateUtils.parseDate("2099-12-31");
            
            assertNotNull(result1);
            assertNotNull(result2);
        }

        @Test
        @DisplayName("Should handle single digit days and months")
        void shouldHandleSingleDigitDaysAndMonths() {
            Date result = DateUtils.parseDate("01/05/2023", "MM/dd/yyyy");
            assertNotNull(result);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
            assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        @DisplayName("Should handle various whitespace scenarios")
        void shouldHandleVariousWhitespaceScenarios() {
            assertNotNull(DateUtils.parseDate("\t2023-05-15\n"));
            assertNotNull(DateUtils.parseDate("   2023-05-15   "));
            assertNull(DateUtils.parseDate("   "));
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should parse, format, and validate round-trip")
        void shouldParseFormatAndValidateRoundTrip() {
            String originalDate = "2023-05-15";
            
            // Parse
            Date parsed = DateUtils.parseDate(originalDate);
            assertNotNull(parsed);
            
            // Format back
            String formatted = DateUtils.formatToIsoDate(parsed);
            assertEquals(originalDate, formatted);
            
            // Validate
            assertTrue(DateUtils.isValidDate(originalDate));
            assertTrue(DateUtils.isValidDate(formatted, "yyyy-MM-dd"));
        }

        @Test
        @DisplayName("Should work with fallback mechanism")
        void shouldWorkWithFallbackMechanism() {
            // Test with ISO format that should succeed with fallback
            String dateValue = "2023-05-25";
            
            // Should fail with wrong preferred format but succeed with fallback to ISO
            Date result = DateUtils.parseDateWithFallback(dateValue, "MM/dd/yyyy");
            assertNotNull(result);
            
            // Verify it's the correct date
            String formatted = DateUtils.formatToIsoDate(result);
            assertEquals("2023-05-25", formatted);
        }
    }
}