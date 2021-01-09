package smartsafe.comm;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.smartcardio.CardTerminal;

import connection.APDUResponse;
import connection.loader.GPException;
import connection.loader.SCP03;
import smartsafe.Prefs;
import smartsafe.model.Entry;
import util.Crypto;
import util.StringHex;

public class SmartSafeAppli extends SCP03 {
	public static final short SW_NO_ERROR = (short) 0x9000;
	public static final short SW_FILE_FULL = (short) 0x6A84;
	public static final short SW_DATA_REMAINING = (short) 0x6310;
	
	private String version;
	private String selectedGroup;
	private Entry selectedEntry;
	private Map<String, List<Entry>> groups;
	
	public SmartSafeAppli(CardTerminal reader) {
		super(reader);
	}
	
	public String getSelectedGroup() {
		return selectedGroup;
	}
	public Set<String> getGroups() {
		if (groups == null) {
			groups = new HashMap<String, List<Entry>>();
			APDUResponse resp;
			List<String> list = new LinkedList<>();
			byte p1 = 0;
			do {
				resp = listGroups(p1);
				for (String group : parseList(resp.getData()))
					groups.put(group, new LinkedList<Entry>());
				p1 = (byte) list.size();
			} while (resp.getStatusWord() == SW_DATA_REMAINING);
			
		}
		return groups.keySet();
	}
	public List<Entry> getEntries(String group, boolean maskPassword) {
		selectGroup(group);
		List<Entry> list = groups.get(group);
		if (list.isEmpty()) {
			APDUResponse resp;
			byte p1 = 0;
			do {
				resp = listEntries(p1);
				for (String entry : parseList(resp.getData())) {
					Entry e = new Entry(group, entry.split(Entry.SEPARATOR));
					selectEntry(e);
					getData(Entry.INDEX_PASSWORD);
					getData(Entry.INDEX_URL);
					getData(Entry.INDEX_lAST_UPDATE);
					getData(Entry.INDEX_EXP_DATE);
					getData(Entry.INDEX_NOTES);
					if (maskPassword)
						e.maskPassword();
					list.add(e);
				}
				p1 = (byte) list.size();
			} while (resp.getStatusWord() == SW_DATA_REMAINING);
		}
		else if (!maskPassword) {//Passwords have to be read
			for (Entry e : list) {
				selectEntry(e);
				getData(Entry.INDEX_PASSWORD);
			}
		}
		return list;
	}
	
	private List<String> parseList(byte[] data) {
		List<String> list = new LinkedList<>();
		for (int i = 0; i < data.length; i += data[i] + 1) {
			list.add(new String(Arrays.copyOfRange(data, i + 1, i + 1 + data[i])));
		}
		return list;
	}
	private String getCla() {
		if (version.startsWith("1."))
			return "84";
		return "80";
	}
	
	public APDUResponse select() throws GPException {
		APDUResponse resp = select(Prefs.getAppAID().toString());
		version = getVersion();
		return resp;
	}
	
	public APDUResponse send(String cmdName, String header, String data, String le) {
		try {
			return super.send(cmdName, header, data, le);
		} catch (GPException e) {
			return null;
		}
	}
	
	public APDUResponse authenticate(String pin) {
		APDUResponse resp = null;
		if (version.startsWith("1.")) {
			StringHex keys = Crypto.keyFromPassword(pin);
			try {
				addKey((short) 0, instanciateKey(keys.get(0, 16).toBytes()));
				addKey((short) 1, instanciateKey(keys.get(16, 16).toBytes()));
				addKey((short) 2, instanciateKey(keys.get(32, 16).toBytes()));
				initUpdate((byte) 0, (byte) 0).toString();
				resp = externalAuth((byte) (SEC_LEVEL_C_MAC | SEC_LEVEL_C_DEC | SEC_LEVEL_R_MAC | SEC_LEVEL_R_ENC));
			} catch (GPException e) {
				resp = new APDUResponse("63C0");
			}
			if (resp.getStatusWord() == SW_NO_ERROR)
				resp = send("Authenticate", "84010000", new StringHex(pin.getBytes()).toString(), "");
		}
		else
			resp = send("Authenticate", "00010000", new StringHex(pin.getBytes()).toString(), "");
		return resp;
	}
	public APDUResponse changePin(String newPin) {
		if (version.startsWith("1.")) {
			StringHex keys = Crypto.keyFromPassword(newPin).get(0, 32);
			String pinLen = StringHex.byteToHex((byte) newPin.length());
			StringHex data = StringHex.concatenate(keys, new StringHex("0A" + pinLen), new StringHex(newPin.getBytes()));
			return send("Change PIN", getCla() + "020000", data.toString(), "");
		}
		return send("Change PIN", "80020000", new StringHex(newPin.getBytes()).toString(), "");
	}
	public APDUResponse getAivailableMemory() {
		return send("Available", getCla() + "030000", "", "");
	}
	public String getVersion() {
		if (version == null)
			version = new String(send("Get Version", "00040000", "", "").getData());
		return version;
	}
	
	public APDUResponse createGroup(byte nbEntries, String identifier, boolean addInternal) {
		String p1 = new StringHex(nbEntries).toString();
		String data = new StringHex(identifier.getBytes()).toString();
		APDUResponse resp = send("Create Group", getCla() + "11" + p1 + "00", data, "");
		if (resp.getStatusWord() == SW_NO_ERROR && addInternal) {
			groups.put(identifier, new LinkedList<Entry>());
			selectedGroup = identifier;
		}
		return resp;
	}
	public APDUResponse deleteGroup(String identifier) {
		groups.remove(identifier);
		return send("Delete Group", getCla() + "120000", new StringHex(identifier.getBytes()).toString(), "");
	}
	public APDUResponse listGroups(byte p1) {
		String sp1 = new StringHex(p1).toString();
		return send("List Group", getCla() + "13" + sp1 + "00", "", "");
	}
	public APDUResponse selectGroup(String identifier) {
		selectedGroup = identifier;
		return send("Select Group", getCla() + "140000", new StringHex(identifier.getBytes()).toString(), "");
	}
	public APDUResponse getStats() {
		return send("Get Stats", getCla() + "150000", "", "04");
	}
	
	public APDUResponse addEntry(byte nbData, Entry entry, boolean addInternal) {
		String p1 = new StringHex(nbData).toString();
		String data = new StringHex(entry.getFullIdentifier().getBytes()).toString();
		APDUResponse resp = send("Add Entry", getCla() + "21" + p1 + "00", data, "");
		if (resp.getStatusWord() == SW_NO_ERROR && addInternal) {
			groups.get(selectedGroup).add(entry);
			selectedEntry = entry;
		}
		return resp;
	}
	public APDUResponse deleteEntry(Entry entry) {
		groups.get(selectedGroup).remove(entry);
		return send("Delete Entry", getCla() + "220000", new StringHex(entry.getFullIdentifier().getBytes()).toString(), "");
	}
	public APDUResponse listEntries(byte p1) {
		String sp1 = new StringHex(p1).toString();
		return send("List Entries", getCla() + "23" + sp1 + "00", "", "");
	}
	public APDUResponse selectEntry(Entry entry) {
		selectedEntry = entry;
		return send("Select Entry", getCla() + "240000", new StringHex(entry.getFullIdentifier().getBytes()).toString(), "");
	}
	public String getData(byte indexData) {
		String p1 = new StringHex(indexData).toString();
		String data = new String(send("Get Data", getCla() + "25" + p1 + "00", "", "").getData());
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
		if (data == null)
			data = "";
		if (addInternal) {
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
		return send("Set Data", getCla() + "26" + p1 + "00", new StringHex(data.getBytes()).toString(), "");
	}
}
