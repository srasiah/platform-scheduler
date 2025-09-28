import java.text.SimpleDateFormat;
import java.util.Date;

public class TestSingleDigitFormat {
    public static void main(String[] args) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
            sdf.setLenient(false);
            
            // Test various M/d/yyyy format dates
            String[] testDates = {
                "1/1/2023",    // Single digits
                "12/31/2023",  // Double digits  
                "5/7/2023",    // Mixed single
                "05/07/2023",  // Double digits with leading zeros
                "13/1/2023",   // Invalid month
                "1/32/2023"    // Invalid day
            };
            
            for (String dateStr : testDates) {
                try {
                    Date parsed = sdf.parse(dateStr);
                    System.out.println(dateStr + " -> SUCCESS: " + parsed);
                } catch (Exception e) {
                    System.out.println(dateStr + " -> FAILED: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}