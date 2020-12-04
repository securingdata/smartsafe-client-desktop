package smartsafe;

import java.util.Locale;
import java.util.prefs.Preferences;

import util.StringHex;

public interface Prefs {
	public Preferences myPrefs = Preferences.userRoot().node("smartsafe");
	
	public String KEY_LANGUAGE = "language";
	public String[] LANGUAGES_LIST = {"English", "Français"};
	public String DEFAULT_LANGUAGE = LANGUAGES_LIST[0];
	
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
		return new StringHex(Prefs.myPrefs.get(Prefs.KEY_PCKG_AID, Prefs.DEFAULT_PCKG_AID).getBytes());
	}
	static StringHex getAppAID() {
		return new StringHex((Prefs.myPrefs.get(Prefs.KEY_PCKG_AID, Prefs.DEFAULT_PCKG_AID) + 
				  Prefs.myPrefs.get(Prefs.KEY_APP_AID_SUFFIX, Prefs.DEFAULT_APP_AID_SUFFIX)).getBytes());
	}
}
