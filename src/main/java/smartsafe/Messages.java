package smartsafe;

import java.util.ResourceBundle;

public class Messages {
	private static ResourceBundle messages;
	static {
		messages = ResourceBundle.getBundle("MessagesBundle", Prefs.prefToLocale());
	}
	
	public static String get(String key) {
		return messages.getString(key);
	}
}
