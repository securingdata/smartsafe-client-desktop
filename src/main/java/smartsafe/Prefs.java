package smartsafe;

import java.util.Locale;
import java.util.prefs.Preferences;

import util.StringHex;

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
	
	public String KEY_PCKG_AID = "pckg_aid";
	public String DEFAULT_PCKG_AID = "SmartSafe";
	
	public String KEY_APP_AID_SUFFIX = "app_aid";
	public String DEFAULT_APP_AID_SUFFIX = "App";
	
	public String KEY_READER = "reader";
	public String DEFAULT_READER = "";
	
	public String KEY_TIMER = "timer";
	public String DEFAULT_TIMER = "300";
	
	public String KEY_ADM = "adm";
	public String[] ADM_LIST = {"No", "Yes"};
	public String DEFAULT_ADM = ADM_LIST[0];
	
	
	static Locale prefToLocale() {
		switch(myPrefs.get(Prefs.KEY_LANGUAGE, Prefs.DEFAULT_LANGUAGE)) {
			default:
			case "English":
				return Locale.ENGLISH;
			case "Français":
				return Locale.FRENCH;
		}
	}
	static StringHex getPckgAID() {
		return new StringHex(get(Prefs.KEY_PCKG_AID).getBytes());
	}
	static StringHex getAppAID() {
		return new StringHex((get(Prefs.KEY_PCKG_AID) + get(Prefs.KEY_APP_AID_SUFFIX)).getBytes());
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
			case KEY_PCKG_AID:
				def = DEFAULT_PCKG_AID;
				break;
			case KEY_APP_AID_SUFFIX:
				def = DEFAULT_APP_AID_SUFFIX;
				break;
			case KEY_READER:
				def = DEFAULT_READER;
				break;
			case KEY_TIMER:
				def = DEFAULT_TIMER;
				break;
			case KEY_ADM:
				def = DEFAULT_ADM;
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
