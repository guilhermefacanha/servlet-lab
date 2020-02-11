package util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	public static String parseDateToString(Date date) {
		return parseDateToString(date, "dd MMM yyyy, HH:mm:ss");
	}
	public static String parseDateToString(Date date, String format) {
		try {
			return new SimpleDateFormat(format).format(date);
		} catch (Exception e) {
			return "";
		}
	}
}
