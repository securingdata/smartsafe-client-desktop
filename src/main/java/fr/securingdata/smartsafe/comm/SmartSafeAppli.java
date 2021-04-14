package fr.securingdata.smartsafe.comm;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javafx.beans.property.*;
import javax.smartcardio.CardTerminal;

import fr.securingdata.connection.APDUResponse;
import fr.securingdata.connection.Connection;
import fr.securingdata.connection.ConnectionException;
import fr.securingdata.globalplatform.SCP03;
import fr.securingdata.smartsafe.Prefs;
import fr.securingdata.smartsafe.controller.ConnectionTimer;
import fr.securingdata.smartsafe.model.Entry;
import fr.securingdata.smartsafe.model.Group;
import fr.securingdata.util.Crypto;
import fr.securingdata.util.StringHex;

public class SmartSafeAppli extends SCP03 {
	public static final short SW_NO_ERROR = (short) 0x9000;
	public static final short SW_FILE_FULL = (short) 0x6A84;
	public static final short SW_DATA_REMAINING = (short) 0x6310;
	
	private String version;
	private Group selectedGroup;
	private Entry selectedEntry;
	private List<Group> groups;
	
	public SmartSafeAppli(CardTerminal reader) {
		super(reader);
		
		/*StringProperty logListener = new SimpleStringProperty("");
		Connection.setLogListener(logListener);
		logListener.addListener((observable, oldValue, newValue) -> {
			System.out.print(newValue);
		});*/
	}
	
	public Group getGroup(String group) {
		for (Group g : groups)
			if (g.name.equals(group))
				return g;
		return null;
	}
	public Group getSelectedGroup() {
		return selectedGroup;
	}
	public Entry getSelectedEntry() {
		return selectedEntry;
	}
	public List<Group> getGroups() {
		if (groups == null) {
			groups = new LinkedList<>();
			APDUResponse resp;
			List<String> list = new LinkedList<>();
			byte p1 = 0;
			do {
				resp = listGroups(p1);
				for (String group : parseList(resp.getData())) {
					groups.add(new Group(group));
				}
				p1 = (byte) list.size();
			} while (resp.getStatusWord() == SW_DATA_REMAINING);
			
		}
		return groups;
	}
	public List<Entry> getEntries(Group group) {
		List<Entry> list = group.entries;
		selectGroup(group);
		APDUResponse resp;
		byte p1 = 0;
		do {
			resp = listEntries(p1);
			for (String entry : parseList(resp.getData())) {
				list.add(new Entry(group, entry.split(Entry.SEPARATOR)));
			}
			p1 = (byte) list.size();
		} while (resp.getStatusWord() == SW_DATA_REMAINING);
		return list;
	}
	public List<Entry> getEntries(Group group, boolean maskPassword) {
		if (!maskPassword) {//Passwords have to be read
			for (Entry e : group.entries) {
				selectGroup(group);
				selectEntry(e);
				getData(Entry.INDEX_PASSWORD);
			}
		}
		return group.entries;
	}
	
	private List<String> parseList(byte[] data) {
		List<String> list = new LinkedList<>();
		for (int i = 0; i < data.length; i += data[i] + 1) {
			list.add(new String(Arrays.copyOfRange(data, i + 1, i + 1 + data[i])));
		}
		return list;
	}
	
	public APDUResponse select() throws ConnectionException {
		APDUResponse resp = select(Prefs.getAppAID().toString());
		version = getVersion();
		return resp;
	}
	
	public APDUResponse send(String cmdName, String header, String data, String le) {
		ConnectionTimer.restart();
		try {
			if (header.startsWith("00"))//ISO7816 CLA, no SM
				return super.rawSend(cmdName, header, data, le);
			else
				return super.send(cmdName, header, data, le);
		} catch (ConnectionException e) {
			return null;
		}
	}
	
	public APDUResponse authenticate(String pin) {
		APDUResponse resp = null;
		StringHex keys = Crypto.keyFromPassword(pin);
		try {
			addKey(KeyName.DEFAULT_KENC, instanciateKey(keys.get(0, 16).toBytes()));
			addKey(KeyName.DEFAULT_KMAC, instanciateKey(keys.get(16, 16).toBytes()));
			addKey(KeyName.DEFAULT_KDEK, instanciateKey(keys.get(32, 16).toBytes()));
			initUpdate((byte) 0, (byte) 0).toString();
			resp = externalAuth((byte) (SEC_LEVEL_C_MAC | SEC_LEVEL_C_DEC | SEC_LEVEL_R_MAC | SEC_LEVEL_R_ENC), false);
		} catch (ConnectionException e) {
			resp = new APDUResponse("63C0");
		}
		if (resp.getStatusWord() == SW_NO_ERROR)
			resp = send("Authenticate", "84010000", new StringHex(pin.getBytes()).toString(), "");
		return resp;
	}
	public APDUResponse changePin(String newPin, boolean init) {
		StringHex keys = Crypto.keyFromPassword(newPin).get(0, 32);
		String pinLen = StringHex.byteToHex((byte) newPin.length());
		StringHex data = StringHex.concatenate(keys, new StringHex("0A" + pinLen), new StringHex(newPin.getBytes()));
		if (init)
			return send("Initialize PIN", "00020000", data.toString(), "");
		else
			return send("Change PIN", "84020000", data.toString(), "");
	}
	public int getAivailableMemory() {
		byte[] data = send("Available", "84030000", "", "").getData();
		return ((data[0] << 8) | data[1]) & 0xFFFF;
	}
	public String getVersion() {
		if (version == null)
			version = new String(send("Get Version", "00040000", "", "").getData());
		return version;
	}
	
	public APDUResponse createGroup(String identifier, boolean addInternal) {
		String data = new StringHex(identifier.getBytes()).toString();
		APDUResponse resp = send("Create Group", "00110000", data, "");
		if (resp.getStatusWord() == SW_NO_ERROR && addInternal) {
			groups.add(selectedGroup = new Group(identifier));
		}
		return resp;
	}
	public APDUResponse deleteGroup(Group group) {
		groups.remove(group);
		return send("Delete Group", "00120000", new StringHex(group.name.getBytes()).toString(), "");
	}
	public APDUResponse listGroups(byte p1) {
		String sp1 = new StringHex(p1).toString();
		return send("List Group", "0013" + sp1 + "00", "", "");
	}
	public APDUResponse selectGroup(Group group) {
		selectedGroup = group;
		return send("Select Group", "00140000", new StringHex(group.name.getBytes()).toString(), "");
	}
	public APDUResponse getStats() {
		return send("Get Stats", "00150000", "", "02");
	}
	public APDUResponse renameGroup(String identifier) {
		APDUResponse resp = send("Rename Group", "00160000", new StringHex(identifier.getBytes()).toString(), ""); 
		if (resp.getStatusWord() == SW_NO_ERROR)
			selectedGroup.name = identifier;
		return resp;
	}
	public APDUResponse moveGroup(boolean up) {
		String p1 = up ? "01" : "02";
		return send("Move Group", "0017" + p1 + "00", "", "00");
	}
	
	public APDUResponse addEntry(byte nbData, Entry entry, boolean addInternal) {
		String p1 = new StringHex(nbData).toString();
		String data = new StringHex(entry.getFullIdentifier().getBytes()).toString();
		APDUResponse resp = send("Add Entry", "8421" + p1 + "00", data, "");
		if (resp.getStatusWord() == SW_NO_ERROR && addInternal) {
			selectedGroup.entries.add(entry);
			selectedEntry = entry;
		}
		return resp;
	}
	public APDUResponse deleteEntry(Entry entry) {
		selectedGroup.entries.remove(entry);
		return send("Delete Entry", "84220000", new StringHex(entry.getFullIdentifier().getBytes()).toString(), "");
	}
	public APDUResponse listEntries(byte p1) {
		String sp1 = new StringHex(p1).toString();
		return send("List Entries", "8423" + sp1 + "00", "", "");
	}
	public APDUResponse selectEntry(Entry entry) {
		selectedEntry = entry;
		return send("Select Entry", "84240000", new StringHex(entry.getFullIdentifier().getBytes()).toString(), "");
	}
	public String getData(byte indexData) {
		String cla = indexData == Entry.INDEX_PASSWORD ? "84" : "00";
		String p1 = new StringHex(indexData).toString();
		String data = new String(send("Get Data", cla + "25" + p1 + "00", "", "").getData());
		if (data.isEmpty())
			return data;//to avoid errors in date parsing
		switch (indexData) {
			case Entry.INDEX_PASSWORD:
				selectedEntry.setPassword(data);
				break;
			case Entry.INDEX_lAST_UPDATE:
				selectedEntry.setLastUpdate(LocalDate.parse(data));
				break;
			case Entry.INDEX_EXP_DATE:
				selectedEntry.setExpiresDate(LocalDate.parse(data));
				break;
			case Entry.INDEX_URL:
				selectedEntry.setUrl(data);
				break;
			case Entry.INDEX_NOTES:
				selectedEntry.setNotes(data);
				break;
		}
		return data;
	}
	public APDUResponse setData(byte indexData, String data, boolean addInternal) {
		String cla = indexData == Entry.INDEX_PASSWORD ? "84" : "00";
		if (data == null)
			data = "";
		if (addInternal && !data.isEmpty()) {
			switch (indexData) {
				case Entry.INDEX_PASSWORD:
					selectedEntry.setPassword(data);
					break;
				case Entry.INDEX_lAST_UPDATE:
					selectedEntry.setLastUpdate(LocalDate.parse(data));
					break;
				case Entry.INDEX_EXP_DATE:
					selectedEntry.setExpiresDate(LocalDate.parse(data));
					break;
				case Entry.INDEX_URL:
					selectedEntry.setUrl(data);
					break;
				case Entry.INDEX_NOTES:
					selectedEntry.setNotes(data);
					break;
			}
		}
		String p1 = new StringHex(indexData).toString();
		return send("Set Data", cla + "26" + p1 + "00", new StringHex(data.getBytes()).toString(), "");
	}
	public APDUResponse renameEntry() {
		return send("Set Data (Rename)", "8426FF00", new StringHex(selectedEntry.getFullIdentifier().getBytes()).toString(), "");
	}
	public APDUResponse moveEntry(int index, boolean up) {
		Entry e = selectedGroup.entries.remove(index);
		if(up)
			selectedGroup.entries.add(index - 1, e);
		else
			selectedGroup.entries.add(index + 1, e);
		String p1 = up ? "01" : "02";
		return send("Move Entry", "0027" + p1 + "00", "", "00");
	}
	public APDUResponse moveEntryToGroup(String identifier) {
		selectedGroup.entries.remove(selectedEntry);
		selectedEntry.group = getGroup(identifier); 
		selectedEntry.group.entries.add(selectedEntry);
		selectedEntry = null;//Invalidate to avoid inconsistencies
		return send("Move Entry", "00270400", new StringHex(identifier.getBytes()).toString(), "00");
	}
}
