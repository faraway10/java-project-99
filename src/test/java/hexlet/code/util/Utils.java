package hexlet.code.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Utils {
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
    }
}
