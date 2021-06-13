package fr.securingdata.smartsafe.comm;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.smartcardio.CardTerminal;

import fr.securingdata.connection.APDUResponse;
import fr.securingdata.connection.Connection;
import fr.securingdata.connection.ConnectionException;
import fr.securingdata.globalplatform.GPCommands;
import fr.securingdata.globalplatform.SCP03;
import fr.securingdata.smartsafe.model.Entry;
import fr.securingdata.smartsafe.model.Group;
import fr.securingdata.util.Crypto;
import fr.securingdata.util.StringHex;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SmartSafeAppli extends SCP03 {
	public static final short SW_NO_ERROR = (short) 0x9000;
	public static final short SW_FILE_FULL = (short) 0x6A84;
	public static final short SW_DATA_REMAINING = (short) 0x6310;
	
	public static final StringHex PCKG_AID = new StringHex("SmartSafe".getBytes());
	public static final StringHex APP_AID = new StringHex("SmartSafeApp".getBytes());
	public static final StringHex SSD_AID = new StringHex("SecuringDataUser".getBytes());
	
	private String version;
	private Group selectedGroup;
	private Entry selectedEntry;
	private List<Group> groups;
	
	public SmartSafeAppli(CardTerminal reader) {
		this(reader, false);
	}
	public SmartSafeAppli(CardTerminal reader, boolean debug) {
		super(reader);
		
		if (debug) {
			StringProperty logListener = new SimpleStringProperty("");
			Connection.setLogListener(logListener);
			logListener.addListener((observable, oldValue, newValue) -> {
				System.out.print(newValue);
			});
		}
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
		APDUResponse resp = select(APP_AID.toString());
		version = getVersion();
		return resp;
	}
	
	public APDUResponse send(String cmdName, String header, String data, String le) {
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
		APDUResponse error = new APDUResponse("63CF");
		APDUResponse resp = null;
		StringHex keys = Crypto.keyFromPassword(pin);
		try {
			addKey(KeyName.DEFAULT_KENC, instanciateKey(keys.get(0, 16).toBytes()));
			addKey(KeyName.DEFAULT_KMAC, instanciateKey(keys.get(16, 16).toBytes()));
			addKey(KeyName.DEFAULT_KDEK, instanciateKey(keys.get(32, 16).toBytes()));
			initUpdate((byte) 0, (byte) 0);
			resp = externalAuth((byte) (SEC_LEVEL_C_MAC | SEC_LEVEL_C_DEC | SEC_LEVEL_R_MAC | SEC_LEVEL_R_ENC), false);
		} catch (ConnectionException e) {
			resp = error;
		}
		
		/*In versions < 2.1.x, isTransactionOpened() will always return false -> no need to perform compatibility case*/
		
		if (resp.getStatusWord() == SW_NO_ERROR) {
			if (isTransactionOpened()) {//Auth OK but transaction opened
				if (authSSD() == null) {//SSD auth NOK, user should authenticate with new keys in order to commit
					return new APDUResponse("63CE");
				}
				else {//SSD auth OK, meaning SSD keys have not been updated, abort on going transaction
					try {
						this.coldReset();
						select();
					} catch (ConnectionException e1) {/*Should not happen*/}
					abortTransaction();
					return authenticate(pin);//This time, authentication will go right
				}
			}
			else {//Auth OK, no transaction, standard OK case
				return send("Authenticate", "84010000", new StringHex(pin.getBytes()).toString(), "");
			}
		}
		else {
			if (isTransactionOpened()) {//Auth NOK but transaction opened
				if (authSSD() == null) {//SSD auth NOK, very bad
					return resp;
				}
				else {//SSD auth OK, meaning commit has to be done
					try {
						this.coldReset();
						select();
					} catch (ConnectionException e1) {/*Should not happen*/}
					commitTransaction();
					return authenticate(pin);//This time, authentication will go right
				}
			}
			else {//Auth NOK, no transaction, standard NOK case
				return resp;
			}
		}
	}
	public APDUResponse changePin(String newPin, String hexPtl, boolean init) {
		StringHex keys = Crypto.keyFromPassword(newPin);
		String pinLen = StringHex.byteToHex((byte) newPin.length());
		StringHex data = StringHex.concatenate(keys.get(0, 32), new StringHex(hexPtl + pinLen), new StringHex(newPin.getBytes()));
		if (init)
			return send("Initialize PIN", "00020000", data.toString(), "");
		else {
			APDUResponse resp = send("Change PIN", "84020000", data.toString(), "");
			if (getVersion().startsWith("2.0."))
				return resp;//Backward compatibility for older server versions
			
			try {
				resp = new GPCommands(authSSD()).putAESKeys(false, keys.get(0, 16), keys.get(16, 16), keys.get(32, 16));
				if (resp.getStatusWord() != SW_NO_ERROR)
					throw new ConnectionException("Put key failed.");
			} catch (NullPointerException /*If authSSD() fails*/ | ConnectionException | GeneralSecurityException e) {
				e.printStackTrace();
				try {
					this.coldReset();
					select();
				} catch (ConnectionException e1) {/*Should not happen*/}
				abortTransaction();
				return null;
			}
			
			try {
				this.coldReset();
				select();
			} catch (ConnectionException e1) {/*Should not happen*/}
			commitTransaction();
			return authenticate(newPin);
		}
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
	private boolean isTransactionOpened() {
		APDUResponse resp = send("Get Transaction Status", "00050000", "", "");
		return resp.getStatusWord() == SW_NO_ERROR && resp.get(0) == 1;
	}
	private APDUResponse abortTransaction() {
		return send("Abort Transaction", "00050100", "", "");
	}
	private APDUResponse commitTransaction() {
		return send("Commit Transaction", "00050200", "", "");
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
	
	private SCP03 authSSD() {
		SCP03 scp = new SCP03(this.selectedReader);
		scp.addKey(KeyName.DEFAULT_KENC, this.getKey(KeyName.DEFAULT_KENC.kvnAndKid));
		scp.addKey(KeyName.DEFAULT_KMAC, this.getKey(KeyName.DEFAULT_KMAC.kvnAndKid));
		scp.addKey(KeyName.DEFAULT_KDEK, this.getKey(KeyName.DEFAULT_KDEK.kvnAndKid));
		try {
			scp.coldReset();
			scp.select(SSD_AID.toString());
			scp.initUpdate((byte) 0, (byte) 0);
			scp.externalAuth((byte) (SEC_LEVEL_C_MAC | SEC_LEVEL_C_DEC));
		} catch (ConnectionException e) {
			e.printStackTrace();
			return null;
		}
		return scp;
	}
}
