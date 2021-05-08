package fr.securingdata.smartsafe;

import java.util.Locale;
import java.util.prefs.Preferences;

public interface Prefs {
	public Preferences myPrefs = Preferences.userRoot().node("smartsafe");
	
	public String KEY_LANGUAGE = "language";
	public String[] LANGUAGES_LIST = {"English", "Français"};
	public String DEFAULT_LANGUAGE = LANGUAGES_LIST[0];
	
	public String KEY_THEME = "theme";
	public String[] THEMES_LIST = {"Classic", "Dark"};
	public String DEFAULT_THEME = THEMES_LIST[0];
	
	public String KEY_CHARS = "chars";
	public String DEFAULT_CHARS = "#$%?!/*=";
	
	public String KEY_READER = "reader";
	public String DEFAULT_READER = "";
	
	public String KEY_TIMER = "timer";
	public String DEFAULT_TIMER = "300";
	
	static Locale prefToLocale() {
		switch(get(Prefs.KEY_LANGUAGE)) {
			default:
			case "English":
				return Locale.ENGLISH;
			case "Français":
				return Locale.FRENCH;
		}
	}
	
	static String get(String key) {
		String def = null;
		switch (key) {
			case KEY_LANGUAGE:
				def = DEFAULT_LANGUAGE;
				break;
			case KEY_THEME:
				def = DEFAULT_THEME;
				break;
			case KEY_CHARS:
				def = DEFAULT_CHARS;
				break;
			case KEY_READER:
				def = DEFAULT_READER;
				break;
			case KEY_TIMER:
				def = DEFAULT_TIMER;
				break;
			default:
				return null;
		}
		return myPrefs.get(key, def);
	}
	static void put(String key, String value) {
		myPrefs.put(key, value);
	}
}
