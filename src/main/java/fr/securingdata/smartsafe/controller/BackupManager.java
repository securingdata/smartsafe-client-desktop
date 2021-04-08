package fr.securingdata.smartsafe.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import fr.securingdata.smartsafe.Messages;
import fr.securingdata.smartsafe.comm.SmartSafeAppli;
import fr.securingdata.smartsafe.model.Entry;
import fr.securingdata.smartsafe.model.Group;
import fr.securingdata.smartsafe.view.GlobalView;
import fr.securingdata.smartsafe.view.Images;
import fr.securingdata.smartsafe.view.ProgressDialog;
import fr.securingdata.util.Crypto;
import fr.securingdata.util.StringHex;
import javafx.scene.control.TreeItem;

public class BackupManager {
	public static final byte[] IV = "initvectorsmarts".getBytes();
	public static final StringHex BACKUP_HEADER = new StringHex("536d61727453616665");
	
	public static void backup(SmartSafeAppli appli, String file, String password) {
		ProgressDialog d = new ProgressDialog(Messages.get("BACKUP_INNER_DIALOG"), Images.BACKUP);
		d.show();
		new Thread((Runnable) () -> {
			d.setProgress(0, Messages.get("BACKUP_INFO_1"));
			int remaining = EntryReader.getRemaining();
			double delta;
			if (remaining != 0) {
				double total = remaining;
				do {
					d.setProgress(0.4 * ((total - remaining) / total));
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				} while ((remaining = EntryReader.getRemaining()) != 0);
				d.setProgress(0.4);
				delta = 0.4 / (appli.getGroups().size() + 1);
			}
			else
				delta = 0.8 / (appli.getGroups().size() + 1);
			
			String tmp = "SmartSafe\n";
			for (Group group : appli.getGroups()) {
				appli.selectGroup(group);
				tmp += group + "0000\n";//Note 0000 replaces the old NB but will be removed in a future version as it is not useful anymore
				List<Entry> entries = appli.getEntries(group, false);
				double delta2 = delta / (entries.size() + 1);
				for (Entry entry : entries) {
					d.addProgress(delta2);
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
				d.addProgress(delta2);
				tmp += "\n";
			}
			d.setText(Messages.get("BACKUP_INFO_2"));
			try {
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				byte[] keyValue = Crypto.keyFromPassword(password).get(0, 16).toBytes();
				cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyValue, "AES"), new IvParameterSpec(IV));
				tmp = (BACKUP_HEADER.toString() + new StringHex(cipher.doFinal(tmp.getBytes())).toString()).replace(" ", "");
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e1) {
				d.setProgress(1, Messages.get("BACKUP_ERROR_1"));
				d.setTextStyle(true);
				return;
			}
			d.setProgress(0.9);
			try (BufferedWriter bf = Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8)){
				bf.write(tmp);
			} catch (IOException e) {
				d.setProgress(1, Messages.get("BACKUP_ERROR_2"));
				d.setTextStyle(true);
				return;
			}
			d.setProgress(1, Messages.get("BACKUP_INFO_3"));
		
			d.closeDialog();
		}).start();
	}
	public static void restore(SmartSafeAppli appli, String file, String password) {
		ProgressDialog d = new ProgressDialog(Messages.get("BACKUP_INNER_DIALOG"), Images.BACKUP);
		d.showDialog();
		new Thread((Runnable) () -> {
			String tmp = "";
			//verifying data to restore
			try (BufferedReader br = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)){
				tmp = br.readLine();
				if (!tmp.startsWith(BACKUP_HEADER.toString().replace(" ", ""))) {
					d.setProgress(1, Messages.get("RESTORE_ERROR_5"));
					d.setTextStyle(true);
					return;
				}
				tmp = tmp.substring(BACKUP_HEADER.toString().replace(" ", "").length());
			} catch (IOException e) {
				d.setProgress(1, Messages.get("RESTORE_ERROR_6"));
				d.setTextStyle(true);
				return;
			}
			
			try {
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				byte[] keyValue = Crypto.keyFromPassword(password).get(0, 16).toBytes();
				cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyValue, "AES"), new IvParameterSpec(IV));
				tmp = new String(cipher.doFinal(new StringHex(tmp).toBytes()));
				if (!tmp.startsWith("SmartSafe\n")) {
					d.setProgress(1, Messages.get("RESTORE_ERROR_7"));
					d.setTextStyle(true);
					return;
				}
				tmp = tmp.substring("SmartSafe\n".length());
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e1) {
				d.setProgress(1, Messages.get("RESTORE_ERROR_8"));
				d.setTextStyle(true);
				return;
			}
			
			//Note NB is not used anymore and will be removed in a future version
			Matcher mGroup = Pattern.compile("(?<GROUP>.+)(?<NB>[0-9A-F]{2})\n").matcher(tmp);
			Matcher mEntry = Pattern.compile("(?<ID>.+)\t(?<USER>.+)\t(?<PASS>.+)\t(?<LAST>.+)\t(?<EXP>.*)\t(?<URL>.*)\t(?<NOTE>.*)\t\n?").matcher(tmp);
			d.setProgress(0.1, Messages.get("RESTORE_ERROR_9"));
			int it = 0;
			double delta = 0.9 / tmp.length();
			while (it + 1 < tmp.length()) {
				mGroup.find(it);
				appli.createGroup(mGroup.group("GROUP"), true);
				GlobalView.getGroups().getChildren().add(new TreeItem<String>(mGroup.group("GROUP")));
				it = mGroup.end();
				while (it < tmp.length() && tmp.charAt(it) != '\n') {
					mEntry.find(it);
					appli.addEntry(Entry.NB_PROPERTIES, new Entry(appli.getSelectedGroup(), mEntry.group("ID"), mEntry.group("USER")), true);
					appli.setData(Entry.INDEX_PASSWORD, mEntry.group("PASS"), true);
					appli.setData(Entry.INDEX_lAST_UPDATE, mEntry.group("LAST"), true);
					appli.setData(Entry.INDEX_EXP_DATE, mEntry.group("EXP"), true);
					appli.setData(Entry.INDEX_URL, mEntry.group("URL"), true);
					appli.setData(Entry.INDEX_NOTES, mEntry.group("NOTE"), true);
					appli.getSelectedEntry().maskPassword();
					appli.getSelectedEntry().validate();
					it = mEntry.end();
					d.setProgress(0.1 + delta * it);
				}
			}
			
			d.setProgress(1, Messages.get("RESTORE_ERROR_2"));
			d.closeDialog();
		}).start();
	}
}
