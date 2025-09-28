import com.example.common.util.DateUtils;
import java.util.Date;

public class TestTimestamp {
    public static void main(String[] args) {
        // Test various timestamp formats with 5/12/1967
        String[] testDates = {
            "5/12/1967",                    // Plain date (baseline)
            "5/12/1967 10:30:00",          // With time
            "5/12/1967 10:30:00 AM",       // With AM/PM
            "5/12/1967 22:30:00",          // 24-hour format
            "5/12/1967T10:30:00",          // ISO-like format
            "5/12/1967 10:30",             // Without seconds
            "5/12/1967 10:30:00.123",      // With milliseconds
        };
        
        System.out.println("Testing DateUtils.parseDate() with timestamps:");
        System.out.println("=".repeat(60));
        
        for (String dateStr : testDates) {
            Date parsed = DateUtils.parseDate(dateStr);
            if (parsed != null) {
                System.out.println("✅ '" + dateStr + "' -> " + parsed);
            } else {
                System.out.println("❌ '" + dateStr + "' -> FAILED TO PARSE");
            }
        }
        
        System.out.println("\nTesting DateUtils.isValidDate() with timestamps:");
        System.out.println("=".repeat(60));
        
        for (String dateStr : testDates) {
            boolean isValid = DateUtils.isValidDate(dateStr);
            System.out.println((isValid ? "✅" : "❌") + " '" + dateStr + "' -> " + (isValid ? "VALID" : "INVALID"));
        }
        
        System.out.println("\nSupported date formats in DateUtils:");
        System.out.println("=".repeat(60));
        String[] formats = DateUtils.getSupportedDatePatterns();
        for (int i = 0; i < formats.length; i++) {
            System.out.println((i + 1) + ". " + formats[i]);
        }
    }
}