package smartsafe.model;

import java.time.LocalDate;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Entry {
	public static final String SEPARATOR = "\n";
	public static final byte INDEX_PASSWORD    = (byte) 0;
	public static final byte INDEX_lAST_UPDATE = (byte) 1;
	public static final byte INDEX_EXP_DATE    = (byte) 2;
	public static final byte INDEX_URL         = (byte) 3;
	public static final byte INDEX_NOTES       = (byte) 4;
	public static final byte NB_PROPERTIES = (byte) 5;
	
	private StringProperty identifier;
	private StringProperty userName;
	private StringProperty password;
	private ObjectProperty<LocalDate> lastUpdate;
	private ObjectProperty<LocalDate> expiresDate;
	private StringProperty url;
	private StringProperty notes;
	
	public Entry() {
		this.identifier = new SimpleStringProperty();
		this.userName = new SimpleStringProperty();
		this.password = new SimpleStringProperty();
		this.lastUpdate = new SimpleObjectProperty<LocalDate>();
		this.expiresDate = new SimpleObjectProperty<LocalDate>();
		this.url = new SimpleStringProperty();
		this.notes = new SimpleStringProperty();
	}
	public Entry(String identifier) {
		this();
		setIdentifier(identifier);
	}
	public Entry(String identifier, String userName) {
		this(identifier);
		setUserName(userName);
	}
	public Entry(String[] identifierAndUserName) {
		this(identifierAndUserName[0], identifierAndUserName[1]);
	}

	public String getFullIdentifier() {
		return identifier.get() + SEPARATOR + userName.get();
	}
	
	public StringProperty getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier.set(identifier);
	}

	public StringProperty getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName.set(userName);
	}

	public StringProperty getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password.set(password);
	}
	public void maskPassword() {
		password.set(password.get().replaceAll(".", "\\*"));
	}

	public ObjectProperty<LocalDate> getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(LocalDate lastUpdate) {
		this.lastUpdate.set(lastUpdate);
	}

	public ObjectProperty<LocalDate> getExpiresDate() {
		return expiresDate;
	}
	public void setExpiresDate(LocalDate expiresDate) {
		this.expiresDate.set(expiresDate);
	}

	public StringProperty getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url.set(url);
	}

	public StringProperty getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes.set(notes);
	}
}
