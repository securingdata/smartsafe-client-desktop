package smartsafe;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {
	private static ResourceBundle messages;
	static {
		messages = ResourceBundle.getBundle("MessagesBundle", Locale.ENGLISH);
	}
	
	public static String get(String key) {
		return messages.getString(key);
	}
}
