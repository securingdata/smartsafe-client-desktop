package smartsafe.comm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardTerminal;

import connection.APDUResponse;
import connection.Application;
import connection.loader.GPException;
import smartsafe.Prefs;
import smartsafe.model.Entry;
import util.Crypto;
import util.StringHex;

public class SmartSafeAppli extends Application {
	private static final byte[] IV = "initvectorsmarts".getBytes();
	
	public static final short SW_NO_ERROR = (short) 0x9000;
	public static final short SW_FILE_FULL = (short) 0x6A84;
	public static final short SW_DATA_REMAINING = (short) 0x6310;
	
	private String selectedGroup;
	private Entry selectedEntry;
	private Map<String, List<Entry>> groups;
	
	public SmartSafeAppli(CardTerminal reader) {
		super(reader);
	}
	
	public boolean backupData(String file, String password) {
		String tmp = "SmartSafe\n";
		for (String group : getGroups()) {
			tmp += group + "\n";
			for (Entry entry : getEntries(group, false)) {
				tmp += entry.getIdentifier().get() + "\t";
				tmp += entry.getUserName().get() + "\t";
				tmp += entry.getPassword().get() + "\t";
				tmp += entry.getLastUpdate().get().toString() + "\t";
				if (entry.getExpiresDate().get() != null)
					tmp += entry.getExpiresDate().get().toString() + "\t";
				else
					tmp += "\t";
				if (entry.getUrl().get() != null)
					tmp += entry.getUrl().get() + "\t";
				else
					tmp += "\t";
				if (entry.getNotes().get() != null)
					tmp += entry.getNotes().get() + "\t\n";
				else
					tmp += "\t\n";
				entry.maskPassword();//Mask password after reading
			}
			tmp += "\n";
		}
		
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] keyValue = Crypto.keyFromPassword(password).get(0, 16).toBytes();
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyValue, "AES"), new IvParameterSpec(IV));
			tmp = new StringHex("536d61727453616665").toString() + new StringHex(cipher.doFinal(tmp.getBytes())).toString();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e1) {
			return false;
		}
		
		try (BufferedWriter bf = Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8)){
			bf.write(tmp);
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	public boolean restoreData(String file, String password) {
		String tmp;
		try (BufferedReader br = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)){
			tmp = br.readLine();
		} catch (IOException e) {
			return false;
		}
		
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] keyValue = Crypto.keyFromPassword(password).get(0, 16).toBytes();
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyValue, "AES"), new IvParameterSpec(IV));
			tmp = new String(cipher.doFinal(new StringHex(tmp).toBytes()));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e1) {
			return false;
		}
		
		System.out.println(tmp);
		
		return true;
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
					Entry e = new Entry(entry.split(Entry.SEPARATOR));
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
		return list;
	}
	
	private List<String> parseList(byte[] data) {
		List<String> list = new LinkedList<>();
		for (int i = 0; i < data.length; i += data[i] + 1) {
			list.add(new String(Arrays.copyOfRange(data, i + 1, i + 1 + data[i])));
		}
		return list;
	}
	
	public APDUResponse select() throws GPException {
		return select(Prefs.getAppAID().toString());
	}
	
	public APDUResponse send(String cmdName, String header, String data, String le) {
		try {
			return super.send(cmdName, header, data, le);
		} catch (GPException e) {
			return null;
		}
	}
	
	public APDUResponse authenticate(String pin) {
		return send("Authenticate", "00010000", new StringHex(pin.getBytes()).toString(), "");
	}
	public APDUResponse changePin(String newPin) {
		return send("Change PIN", "80020000", new StringHex(newPin.getBytes()).toString(), "");
	}
	public APDUResponse getAivailableMemory() {
		return send("Available", "80030000", "", "");
	}
	public String getVersion() {
		return new String(send("Get Version", "00040000", "", "").getData());
	}
	
	public APDUResponse createGroup(byte nbEntries, String identifier) {
		String p1 = new StringHex(nbEntries).toString();
		String data = new StringHex(identifier.getBytes()).toString();
		APDUResponse resp = send("Create Group", "8011" + p1 + "00", data, "");
		if (resp.getStatusWord() == SW_NO_ERROR) {
			groups.put(identifier, new LinkedList<Entry>());
			selectedGroup = identifier;
		}
		return resp;
	}
	public APDUResponse deleteGroup(String identifier) {
		groups.remove(identifier);
		return send("Delete Group", "80120000", new StringHex(identifier.getBytes()).toString(), "");
	}
	public APDUResponse listGroups(byte p1) {
		String sp1 = new StringHex(p1).toString();
		return send("List Group", "8013" + sp1 + "00", "", "");
	}
	public APDUResponse selectGroup(String identifier) {
		selectedGroup = identifier;
		return send("Select Group", "80140000", new StringHex(identifier.getBytes()).toString(), "");
	}
	
	public APDUResponse addEntry(byte nbData, Entry entry) {
		String p1 = new StringHex(nbData).toString();
		String data = new StringHex(entry.getFullIdentifier().getBytes()).toString();
		APDUResponse resp = send("Add Entry", "8021" + p1 + "00", data, "");
		if (resp.getStatusWord() == SW_NO_ERROR) {
			groups.get(selectedGroup).add(entry);
			selectedEntry = entry;
		}
		return resp;
	}
	public APDUResponse deleteEntry(Entry entry) {
		groups.get(selectedGroup).remove(entry);
		return send("Delete Entry", "80220000", new StringHex(entry.getFullIdentifier().getBytes()).toString(), "");
	}
	public APDUResponse listEntries(byte p1) {
		String sp1 = new StringHex(p1).toString();
		return send("List Entries", "8023" + sp1 + "00", "", "");
	}
	public APDUResponse selectEntry(Entry entry) {
		selectedEntry = entry;
		return send("Select Entry", "80240000", new StringHex(entry.getFullIdentifier().getBytes()).toString(), "");
	}
	public String getData(byte indexData) {
		String p1 = new StringHex(indexData).toString();
		String data = new String(send("Get Data", "8025" + p1 + "00", "", "").getData());
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
	public APDUResponse setData(byte indexData, String data) {
		if (data == null)
			data = "";
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
		String p1 = new StringHex(indexData).toString();
		return send("Set Data", "8026" + p1 + "00", new StringHex(data.getBytes()).toString(), "");
	}
}
