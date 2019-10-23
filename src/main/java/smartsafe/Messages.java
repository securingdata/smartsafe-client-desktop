package smartsafe;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {
	private static Locale locale;
	private static ResourceBundle messages;
	static {
		//locale = new Locale("en","US");
		locale = new Locale("fr","FR");
		messages = ResourceBundle.getBundle("MessagesBundle", locale);
	}
	
	public static String get(String key) {
		return messages.getString(key);
	}
}
