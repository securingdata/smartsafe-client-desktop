package smartsafe.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import connection.APDUResponse;
import connection.loader.GPException;
import smartsafe.Messages;
import smartsafe.comm.SmartSafeAppli;
import smartsafe.model.Entry;
import smartsafe.view.Images;
import smartsafe.view.ProgressDialog;
import util.Crypto;
import util.StringHex;

public class BackupManager {
	public static void backup(SmartSafeAppli appli, String file, String password) {
		ProgressDialog d = new ProgressDialog(Messages.get("BACKUP_INNER_DIALOG"), Images.BACKUP);
		d.showDialog();
		new Thread((Runnable) () -> {//FIXME is a thread necessary ?
			d.setProgress(0.1, Messages.get("BACKUP_INFO_1"));
			String tmp = "SmartSafe\n";
			double delta = 0.8 / (appli.getGroups().size() + 1);
			for (String group : appli.getGroups()) {
				appli.selectGroup(group);
				tmp += group + appli.getStats().get(3, 1).toString() + "\n";
				double delta2 = delta / (appli.getEntries(group, false).size() + 1);
				for (Entry entry : appli.getEntries(group, false)) {
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
			d.addProgress(delta);
			d.setText(Messages.get("BACKUP_INFO_2"));
			try {
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				byte[] keyValue = Crypto.keyFromPassword(password).get(0, 16).toBytes();
				cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyValue, "AES"), new IvParameterSpec(Crypto.IV));
				tmp = (Crypto.BACKUP_HEADER.toString() + new StringHex(cipher.doFinal(tmp.getBytes())).toString()).replace(" ", "");
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e1) {
				d.setProgress(1, Messages.get("BACKUP_ERROR_1"));
				d.setTextStyle(true);
				return;
			}
			
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
			try {
				appli.coldReset();
				APDUResponse resp = appli.select();
				if (resp.getStatusWord() != SmartSafeAppli.SW_NO_ERROR) {
					d.setProgress(1, Messages.get("RESTORE_ERROR_1"));
					d.setTextStyle(true);
					return;
				}
				else {
					if (new StringHex(resp.getData()).toString().equals("DE CA")) {
						String tmp = "";
						//verifying data to restore
						try (BufferedReader br = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)){
							tmp = br.readLine();
							if (!tmp.startsWith(Crypto.BACKUP_HEADER.toString().replace(" ", ""))) {
								d.setProgress(1, Messages.get("RESTORE_ERROR_5"));
								d.setTextStyle(true);
								return;
							}
							tmp = tmp.substring(Crypto.BACKUP_HEADER.toString().replace(" ", "").length());
						} catch (IOException e) {
							d.setProgress(1, Messages.get("RESTORE_ERROR_6"));
							d.setTextStyle(true);
							return;
						}
						
						try {
							Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
							byte[] keyValue = Crypto.keyFromPassword(password).get(0, 16).toBytes();
							cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyValue, "AES"), new IvParameterSpec(Crypto.IV));
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
						
						
						Matcher mGroup = Pattern.compile("(?<GROUP>.+)(?<NB>[0-9A-F]{2})\n").matcher(tmp);
						Matcher mEntry = Pattern.compile("(?<ID>.+)\t(?<USER>.+)\t(?<PASS>.+)\t(?<LAST>.+)\t(?<EXP>.*)\t(?<URL>.*)\t(?<NOTE>.*)\t\n?").matcher(tmp);
						d.setProgress(0.1, Messages.get("RESTORE_ERROR_9"));
						int it = 0;
						double delta = 0.8 / tmp.length();
						while (it + 1 < tmp.length()) {
							mGroup.find(it);
							appli.createGroup(Byte.valueOf(mGroup.group("NB"), 16), mGroup.group("GROUP"), false);
							it = mGroup.end();
							while (it < tmp.length() && tmp.charAt(it) != '\n') {
								mEntry.find(it);
								appli.addEntry(Entry.NB_PROPERTIES, new Entry(appli.getSelectedGroup(), mEntry.group("ID"), mEntry.group("USER")), false);
								appli.setData(Entry.INDEX_PASSWORD, mEntry.group("PASS"), false);
								appli.setData(Entry.INDEX_lAST_UPDATE, mEntry.group("LAST"), false);
								appli.setData(Entry.INDEX_EXP_DATE, mEntry.group("EXP"), false);
								appli.setData(Entry.INDEX_URL, mEntry.group("URL"), false);
								appli.setData(Entry.INDEX_NOTES, mEntry.group("NOTE"), false);
								it = mEntry.end();
								d.setProgress(delta * it);
							}
							d.setProgress(delta * it);
						}
						
						d.setProgress(1, Messages.get("RESTORE_ERROR_2"));
					}
					else {
						d.setProgress(1, Messages.get("RESTORE_ERROR_3"));
						d.setTextStyle(true);
						return;//TODO
					}
				}
			} catch (GPException e) {
				d.setProgress(1, Messages.get("RESTORE_ERROR_4"));
				return;
			}
			
			d.closeDialog();
		}).start();
	}
}
