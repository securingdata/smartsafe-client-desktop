package smartsafe;

import java.util.ResourceBundle;

public class Messages {
	private static ResourceBundle messages;
	static {
		reloadMessages();
	}
	
	public static void reloadMessages() {
		messages = ResourceBundle.getBundle("MessagesBundle", Prefs.prefToLocale());
	}
	public static String get(String key) {
		return messages.getString(key);
	}
}
