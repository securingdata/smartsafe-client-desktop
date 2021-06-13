package fr.securingdata.smartsafe.controller;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.smartcardio.CardTerminal;

import fr.securingdata.connection.APDUResponse;
import fr.securingdata.connection.ConnectionException;
import fr.securingdata.smartsafe.Messages;
import fr.securingdata.smartsafe.Prefs;
import fr.securingdata.smartsafe.comm.SmartSafeAppli;
import fr.securingdata.smartsafe.model.Entry;
import fr.securingdata.smartsafe.model.Group;
import fr.securingdata.smartsafe.view.GlobalView;
import fr.securingdata.smartsafe.view.Help;
import fr.securingdata.smartsafe.view.Images;
import fr.securingdata.smartsafe.view.ProgressDialog;
import fr.securingdata.smartsafe.view.ViewUtils;
import fr.securingdata.util.StringHex;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;

public class Controls {
	private static class SmartSafeAppliProxy extends SmartSafeAppli {
		public SmartSafeAppliProxy(CardTerminal reader) {
			super(reader);
		}
		
		@Override
		public APDUResponse send(String cmdName, String header, String data, String le) {
			ConnectionTimer.restart();
			try {
				if (header.startsWith("00"))//ISO7816 CLA, no SM
					return super.rawSend(cmdName, header, data, le);
				else
					return super.send(cmdName, header, data, le);
			} catch (ConnectionException e) {
				if (appli != null)
					ACTION_CONNECT.run();
				throw new RuntimeException(e.getMessage());
			}
		}
	}
	private static ToggleButton connection;
	private static MenuItem connectionMenu;
	private static SmartSafeAppli appli;
	
	public static SmartSafeAppli getAppli() {
		return appli;
	}
	
	
	//==================== ACTIONS ====================\\
	public static final String CONNECT = Messages.get("CONNECT");
	public static final String DISCONNECT = Messages.get("DISCONNECT");
	public static final Action ACTION_CONNECT = new Action(CONNECT, false, null, params -> {
		if (appli == null) {
			Object[] values = GlobalView.connectDialog();
			
			if (values != null) {//User has validated its entry
				ProgressDialog d = new ProgressDialog(Messages.get("CONNECT_LOADING"), Images.CONNECT);
				d.show();
				
				appli = new SmartSafeAppliProxy((CardTerminal) values[0]);
				try {
					appli.coldReset();
					APDUResponse resp = appli.select();
					if (resp.getStatusWord() != (short) SmartSafeAppli.SW_NO_ERROR) {
						d.closeNow();
						ViewUtils.errorDialog(Messages.get("CONNECT_NO_APP"));
						appli = null;
					}
					else {
						if (!appli.getVersion().startsWith("2.")) {
							d.closeNow();
							ViewUtils.errorDialog(Messages.get("CONNECT_VER_NOT_SUPP"));
							appli = null;
						}
						else {
							resp = appli.authenticate((String) values[1]);
							if (resp.getStatusWord() != (short) SmartSafeAppli.SW_NO_ERROR) {
								d.closeNow();
								int ptc = (int) (resp.getStatusWord() & 0xF);
								if (ptc == 0xF)
									ViewUtils.errorDialog(Messages.get("CONNECT_UNDEF_ERROR"));
								else if (ptc == 0xE)
									ViewUtils.errorDialog(Messages.get("CONNECT_OLD_KEY_ERROR"));
								else
									ViewUtils.errorDialog(Messages.get("CONNECT_ERROR") + ptc);
								appli.disconnect();
								appli = null;
							}
						}
					}
				} catch (ConnectionException e) {
					d.closeNow();
					appli = null;
					ViewUtils.errorDialog(e.getMessage());
				}
				
				if (appli != null) {
					Prefs.put(Prefs.KEY_READER, ((CardTerminal) values[0]).toString());
					
					new Thread((Runnable) () -> {
						double delta = 1d / appli.getGroups().size();
						for (Group group : appli.getGroups()) {
							appli.getEntries(group);
							GlobalView.getGroups().getChildren().add(new TreeItem<String>(group.name));
							d.addProgress(delta);
						}
						
						//Avoid concurrent exception: use a dedicated for loop
						EntryReader.setAllowReaderCreation(true);
						for (Group group : appli.getGroups()) {
							for (Entry e : appli.getEntries(group, true))
								EntryReader.readEntry(e);
						}
						EntryReader.setAllowReaderCreation(false);
						d.closeDialog();
						ConnectionTimer.start();
					}).start();
				}
			}
		}
		else {
			//Disconnect and clear view
			appli.disconnect();
			appli = null;
			GlobalView.getGroups().getChildren().clear();
			selectGroup(null);
		}
		
		//Update view
		ViewUtils.cardConnected.set(appli != null);
		connection.setSelected(appli != null);
		if (appli != null) {
			connection.setGraphic(new ImageView(Images.DISCONNECT));
			connection.setTooltip(new Tooltip(Controls.DISCONNECT));
			connectionMenu.setGraphic(new ImageView(Images.DISCONNECT));
			connectionMenu.setText(Controls.DISCONNECT);
			
		}
		else {
			ConnectionTimer.stop();
			connection.setGraphic(new ImageView(Images.CONNECT));
			connection.setTooltip(new Tooltip(CONNECT));
			connectionMenu.setGraphic(new ImageView(Images.CONNECT));
			connectionMenu.setText(CONNECT);
		}
	});
	
	public static final String NEW_GROUP = Messages.get("NEW_GROUP");
	public static final Action ACTION_NEW_GROUP = new Action(NEW_GROUP, false, null, params -> {
		ConnectionTimer.restart();
		final String name = GlobalView.groupDialog(false);
		if (name != null) {
			EntryReader.suspendReader();
			short sw = appli.createGroup(name, true).getStatusWord();
			EntryReader.restartReader();
			if (sw == SmartSafeAppli.SW_NO_ERROR) {
				GlobalView.getGroups().getChildren().add(new TreeItem<String>(name));
			}
			else if (sw == SmartSafeAppli.SW_FILE_FULL) {
				ViewUtils.errorDialog(Messages.get("GROUP_ERROR_1"));
			}
			else {
				ViewUtils.errorDialog(Messages.get("GROUP_ERROR_2") + new StringHex(sw).toString());
			}
		}
	});
	
	public static final String NEW_ENTRY = Messages.get("NEW_ENTRY");
	public static final Action ACTION_NEW_ENTRY = new Action(NEW_ENTRY, false, null, params -> {
		ConnectionTimer.restart();
		String[] data = GlobalView.entryDialog(null);
		if (data != null) {
			EntryReader.suspendReader();
			appli.selectGroup(getCurrentSelectedGroupForUse());
			Entry entry = new Entry(appli.getSelectedGroup(), data[0], data[1]);
			short sw = appli.addEntry(Entry.NB_PROPERTIES, entry, true).getStatusWord();
			if (sw == SmartSafeAppli.SW_FILE_FULL) {
				ViewUtils.errorDialog(Messages.get("ENTRY_ERROR_1"));
				return;
			}
			else if (sw != SmartSafeAppli.SW_NO_ERROR) {
				ViewUtils.errorDialog(Messages.get("ENTRY_ERROR_2") + new StringHex(sw).toString());
				return;
			}
			GlobalView.getTableEntries().getItems().add(entry);
			if (data[Entry.INDEX_PASSWORD + 2].length() != 0) {
				appli.setData(Entry.INDEX_PASSWORD, data[Entry.INDEX_PASSWORD + 2], true);
				appli.setData(Entry.INDEX_lAST_UPDATE, data[Entry.INDEX_lAST_UPDATE + 2], true);
				entry.maskPassword();
			}
			if (data[Entry.INDEX_EXP_DATE + 2] != null)
				appli.setData(Entry.INDEX_EXP_DATE, data[Entry.INDEX_EXP_DATE + 2], true);
			appli.setData(Entry.INDEX_URL, data[Entry.INDEX_URL + 2], true);
			appli.setData(Entry.INDEX_NOTES, data[Entry.INDEX_NOTES + 2], true);
			entry.validate();
			EntryReader.restartReader();
		}
	});
	
	public static final String DELETE = Messages.get("DELETE");
	public static final Action ACTION_DELETE = new Action(DELETE, false, null, params -> {
		ConnectionTimer.restart();
		Group group = getCurrentSelectedGroupForUse();
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		Object response = GlobalView.deleteDialog(group, e);
		if (response != null) {
			EntryReader.suspendReader();
			if (response instanceof Group) {
				GlobalView.getGroups().getChildren().remove(GlobalView.getGroupsView().getSelectionModel().getSelectedItem());
				GlobalView.getGroupsView().getSelectionModel().clearSelection();
				for (Entry entry : appli.getEntries(group, true))
					EntryReader.removeEntry(entry);
				appli.deleteGroup(group);
			}
			else if (response instanceof Entry) {
				GlobalView.getTableEntries().getItems().remove(e);
				EntryReader.removeEntry(e);
				appli.selectGroup(getCurrentSelectedGroupForUse());
				appli.deleteEntry(e);
			}
			EntryReader.restartReader();
		}
	});
	
	public static final String CHANGE_PIN = Messages.get("CHANGE_PIN");
	public static final Action ACTION_CHANGE_PIN = new Action(CHANGE_PIN, false, null, params -> {
		ConnectionTimer.restart();
		String[] data = GlobalView.changePINDialog();
		if (data != null) {
			EntryReader.suspendReader();
			if (appli.changePin(data[0], data[1], false) == null) {
				ViewUtils.errorDialog(Messages.get("CHANGE_PIN_APP_ERROR_1"));
			}
			EntryReader.restartReader();
		}
	});
	
	public static final String BACKUP = Messages.get("BACKUP");
	public static final Action ACTION_BACKUP = new Action(BACKUP, false, null, params -> {
		ConnectionTimer.restart();
		String[] data = GlobalView.backupAndRestoreDialog();
		if (data == null)
			return;
		if (data[2].equals(Messages.get("BACKUP_BACKUP"))) {
			BackupManager.backup(appli, data[0], data[1]);
		}
		else if (data[2].equals(Messages.get("BACKUP_RESTORE"))) {
			BackupManager.restore(appli, data[0], data[1]);
		}
	});
	
	public static final String EXIT = Messages.get("EXIT");
	public static final Action ACTION_EXIT = new Action(EXIT, false, null, params -> {
		ConnectionTimer.stop();
		if (appli != null)
			appli.disconnect();
		System.exit(0);
	});
	
	public static final String RENAME_GROUP = Messages.get("RENAME_GROUP");
	public static final Action ACTION_RENAME_GROUP = new Action(RENAME_GROUP, false, null, params -> {
		ConnectionTimer.restart();
		final String name = GlobalView.groupDialog(true);
		if (name != null) {
			EntryReader.suspendReader();
			appli.selectGroup(getCurrentSelectedGroupForUse());
			short sw = appli.renameGroup(name).getStatusWord();
			EntryReader.restartReader();
			if (sw == SmartSafeAppli.SW_NO_ERROR) {
				GlobalView.getGroupsView().getSelectionModel().getSelectedItem().setValue(name);
			}
			else {
				ViewUtils.errorDialog(Messages.get("GROUP_ERROR_3") + new StringHex(sw).toString());
			}
		}
	});
	
	public static final String EDIT = Messages.get("EDIT");
	public static final Action ACTION_EDIT = new Action(EDIT, false, null, params -> {
		ConnectionTimer.restart();
		Entry e = getCurrentSelectedEntryForUse();
		EntryReader.suspendReader();
		appli.selectGroup(e.group);
		appli.selectEntry(e);
		appli.getData(Entry.INDEX_PASSWORD);
		final String oldPass = e.getPassword().get();
		String[] data = GlobalView.entryDialog(e);
		e.maskPassword();
		if (data != null) {
			String fullId = data[0] + Entry.SEPARATOR + data[1];
			if (!fullId.equals(e.getFullIdentifier())) {
				e.setIdentifier(data[0]);
				e.setUserName(data[1]);
				appli.renameEntry();
			}
			if (!data[Entry.INDEX_PASSWORD + 2].equals(oldPass)) {
				appli.setData(Entry.INDEX_PASSWORD, data[Entry.INDEX_PASSWORD + 2], true);
				appli.setData(Entry.INDEX_lAST_UPDATE, data[Entry.INDEX_lAST_UPDATE + 2], true);
			}
			e.maskPassword();
			if (data[Entry.INDEX_EXP_DATE + 2] != null)
				appli.setData(Entry.INDEX_EXP_DATE, data[Entry.INDEX_EXP_DATE + 2], true);
			appli.setData(Entry.INDEX_URL, data[Entry.INDEX_URL + 2], true);
			appli.setData(Entry.INDEX_NOTES, data[Entry.INDEX_NOTES + 2], true);
		}
		EntryReader.restartReader();
		GlobalView.getTableEntries().getSelectionModel().select(null);
		GlobalView.getTableEntries().getSelectionModel().select(e);
	});
	
	public static final String GOTO = Messages.get("GOTO");
	public static final Action ACTION_GOTO = new Action(GOTO, false, null, params -> {
		ConnectionTimer.restart();
		Entry e = getCurrentSelectedEntryForUse();
		if (e.getUrl().get() != null) {
			try {
				java.awt.Desktop.getDesktop().browse(new URI(e.getUrl().get()));
			}
			catch (IOException e1) {}
			catch (URISyntaxException e1) {}
		}
	});
	
	public static final String COPY_USER = Messages.get("COPY_USER");
	public static final Action ACTION_COPY_USER = new Action(COPY_USER, false, null, params -> {
		ConnectionTimer.restart();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Entry e = getCurrentSelectedEntryForUse();
		clipboard.setContents(new StringSelection(e.getUserName().get()), null);
	});
	
	public static final String COPY_PASS = Messages.get("COPY_PASS");
	public static final Action ACTION_COPY_PASS = new Action(COPY_PASS, false, null, params -> {
		ConnectionTimer.restart();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Entry e = getCurrentSelectedEntryForUse();
		EntryReader.suspendReader();
		appli.selectGroup(e.group);
		appli.selectEntry(e);
		appli.getData(Entry.INDEX_PASSWORD);
		EntryReader.restartReader();
		clipboard.setContents(new StringSelection(e.getPassword().get()), null);
		e.maskPassword();
	});
	
	public static final String SHOW_PASS = Messages.get("SHOW_PASS");
	public static final Action ACTION_SHOW_PASS = new Action(SHOW_PASS, false, null, params -> {
		ConnectionTimer.restart();
		Entry e = getCurrentSelectedEntryForUse();
		EntryReader.suspendReader();
		appli.selectGroup(e.group);
		appli.selectEntry(e);
		appli.getData(Entry.INDEX_PASSWORD);
		EntryReader.restartReader();
	});
	
	public static final String GROUP_UP = Messages.get("GROUP_UP");
	public static final Action ACTION_GROUP_UP = new Action(GROUP_UP, false, null, params -> {
		ConnectionTimer.restart();
		EntryReader.suspendReader();
		appli.selectGroup(getCurrentSelectedGroupForUse());
		appli.moveGroup(true);
		int index = GlobalView.getGroupsView().getSelectionModel().getSelectedIndex();
		TreeItem<String> item = GlobalView.getGroups().getChildren().remove(index);
		GlobalView.getGroups().getChildren().add(index - 1, item);
		GlobalView.getGroupsView().getSelectionModel().select(item);
		EntryReader.restartReader();
	});
	public static final String GROUP_DOWN = Messages.get("GROUP_DOWN");
	public static final Action ACTION_GROUP_DOWN = new Action(GROUP_DOWN, false, null, params -> {
		ConnectionTimer.restart();
		EntryReader.suspendReader();
		appli.selectGroup(getCurrentSelectedGroupForUse());
		appli.moveGroup(false);
		int index = GlobalView.getGroupsView().getSelectionModel().getSelectedIndex();
		TreeItem<String> item = GlobalView.getGroups().getChildren().remove(index);
		GlobalView.getGroups().getChildren().add(index + 1, item);
		GlobalView.getGroupsView().getSelectionModel().select(item);
		EntryReader.restartReader();
	});
	
	public static final String ENTRY_UP = Messages.get("ENTRY_UP");
	public static final Action ACTION_ENTRY_UP = new Action(ENTRY_UP, false, null, params -> {
		ConnectionTimer.restart();
		EntryReader.suspendReader();
		int index = GlobalView.getTableEntries().getSelectionModel().getSelectedIndex();
		appli.selectGroup(getCurrentSelectedGroupForUse());
		appli.selectEntry(getCurrentSelectedEntryForUse());
		appli.moveEntry(index, true);
		Entry item = GlobalView.getTableEntries().getItems().remove(index);
		GlobalView.getTableEntries().getItems().add(index - 1, item);
		GlobalView.getTableEntries().getSelectionModel().select(item);
		EntryReader.restartReader();
	});
	public static final String ENTRY_DOWN = Messages.get("ENTRY_DOWN");
	public static final Action ACTION_ENTRY_DOWN = new Action(ENTRY_DOWN, false, null, params -> {
		ConnectionTimer.restart();
		EntryReader.suspendReader();
		int index = GlobalView.getTableEntries().getSelectionModel().getSelectedIndex();
		appli.selectGroup(getCurrentSelectedGroupForUse());
		appli.selectEntry(getCurrentSelectedEntryForUse());
		appli.moveEntry(index, false);
		Entry item = GlobalView.getTableEntries().getItems().remove(index);
		GlobalView.getTableEntries().getItems().add(index + 1, item);
		GlobalView.getTableEntries().getSelectionModel().select(item);
		EntryReader.restartReader();
	});
	public static final String ENTRY_MOVE_TO = Messages.get("ENTRY_MOVE_TO");
	public static final Action ACTION_ENTRY_MOVE_TO = new Action(ENTRY_MOVE_TO, false, null, params -> {
		ConnectionTimer.restart();
		EntryReader.suspendReader();
		Entry e = getCurrentSelectedEntryForUse();
		appli.selectGroup(getCurrentSelectedGroupForUse());
		appli.selectEntry(e);
		appli.moveEntryToGroup((String) params);
		EntryReader.restartReader();
		
		GlobalView.getTableEntries().getItems().remove(e);
		EntryReader.removeEntry(e);		
	});
	
	public static final String HELP = Messages.get("HELP");
	public static final Action ACTION_HELP = new Action(HELP, false, null, params -> {
		ConnectionTimer.restart();
		Help.helpDialog();
	});
	
	public static final String PROPERTIES = Messages.get("PROPERTIES");
	public static final Action ACTION_PROPERTIES = new Action(PROPERTIES, false, null, params -> {
		ConnectionTimer.restart();
		GlobalView.propertiesDialog(appli);
	});
	
	public static final String PREFERENCES = Messages.get("PREFERENCES");
	public static final Action ACTION_PREFERENCES = new Action(PREFERENCES, false, null, params -> {
		ConnectionTimer.restart();
		GlobalView.preferencesDialog();
	});
	
	public static final String ABOUT = Messages.get("ABOUT");
	public static final Action ACTION_ABOUT = new Action(ABOUT, false, null, params -> {
		ConnectionTimer.restart();
		GlobalView.aboutDialog(appli == null ? null : appli.getVersion());
	});
	//================== END ACTIONS ==================\\
	
	static {
		ButtonBase b;
		MenuItem mi;
		
		GlobalView.BUTTONS.add(b = connection = new ToggleButton("", new ImageView(Images.CONNECT)));
		b.setTooltip(new Tooltip(CONNECT));
		b.setOnAction(event-> ACTION_CONNECT.run());
		
		GlobalView.ITEMS.add(mi = connectionMenu = new MenuItem(CONNECT, new ImageView(Images.CONNECT)));
		mi.setOnAction(event -> ACTION_CONNECT.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+Q"));
		
		GlobalView.BUTTONS.add(b = new Button("", new ImageView(Images.NEW_GROUP)));
		ViewUtils.addDisableListener(b, ViewUtils.cardConnected);
		b.setTooltip(new Tooltip(NEW_GROUP));
		b.setOnAction(event -> ACTION_NEW_GROUP.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(NEW_GROUP, new ImageView(Images.NEW_GROUP)));
		ViewUtils.addDisableListener(mi, ViewUtils.cardConnected);
		mi.setOnAction(event -> ACTION_NEW_GROUP.run());
		
		GlobalView.GROUP_ITEMS.add(mi = new MenuItem(NEW_GROUP, new ImageView(Images.NEW_GROUP)));
		ViewUtils.addDisableListener(mi, ViewUtils.cardConnected);
		mi.setOnAction(event -> ACTION_NEW_GROUP.run());
		
		GlobalView.BUTTONS.add(b = new Button("", new ImageView(Images.NEW_ENTRY)));
		ViewUtils.addDisableListener(b, ViewUtils.groupSelected);
		b.setTooltip(new Tooltip(NEW_ENTRY));
		b.setOnAction(event -> ACTION_NEW_ENTRY.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(NEW_ENTRY, new ImageView(Images.NEW_ENTRY)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupSelected);
		mi.setOnAction(event -> ACTION_NEW_ENTRY.run());
		
		GlobalView.GROUP_ITEMS.add(mi = new MenuItem(NEW_ENTRY, new ImageView(Images.NEW_ENTRY)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupSelected);
		mi.setOnAction(event -> ACTION_NEW_ENTRY.run());
		
		GlobalView.BUTTONS.add(b = new Button("", new ImageView(Images.DELETE)));
		ViewUtils.addDisableListener(b, ViewUtils.groupSelected);
		b.setTooltip(new Tooltip(DELETE));
		b.setOnAction(event -> ACTION_DELETE.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(DELETE, new ImageView(Images.DELETE)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupSelected);
		mi.setOnAction(event -> ACTION_DELETE.run());
		mi.setAccelerator(KeyCombination.valueOf("Delete"));
		
		GlobalView.GROUP_ITEMS.add(mi = new MenuItem(DELETE, new ImageView(Images.DELETE)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupSelected);
		mi.setOnAction(event -> ACTION_DELETE.run());
		mi.setAccelerator(KeyCombination.valueOf("Delete"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(CHANGE_PIN, new ImageView(Images.PIN)));
		ViewUtils.addDisableListener(mi, ViewUtils.cardConnected);
		mi.setOnAction(event -> ACTION_CHANGE_PIN.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(BACKUP, new ImageView(Images.BACKUP)));
		ViewUtils.addDisableListener(mi, ViewUtils.cardConnected);
		mi.setOnAction(event -> ACTION_BACKUP.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(EXIT, new ImageView(Images.EXIT)));
		mi.setOnAction(event -> ACTION_EXIT.run());
		mi.setAccelerator(KeyCombination.valueOf("Alt+F4"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(RENAME_GROUP, new ImageView(Images.RENAME)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupSelected);
		mi.setOnAction(event -> ACTION_RENAME_GROUP.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+R"));
		
		GlobalView.GROUP_ITEMS.add(mi = new MenuItem(RENAME_GROUP, new ImageView(Images.RENAME)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupSelected);
		mi.setOnAction(event -> ACTION_RENAME_GROUP.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+R"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(EDIT, new ImageView(Images.EDIT)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_EDIT.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+E"));
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(EDIT, new ImageView(Images.EDIT)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_EDIT.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+E"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(GOTO, new ImageView(Images.GOTO)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_GOTO.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+G"));
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(GOTO, new ImageView(Images.GOTO)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_GOTO.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+G"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(COPY_USER, new ImageView(Images.COPY)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_COPY_USER.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+X"));
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(COPY_USER, new ImageView(Images.COPY)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_COPY_USER.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+X"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(COPY_PASS, new ImageView(Images.COPY_PASS)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_COPY_PASS.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+C"));
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(COPY_PASS, new ImageView(Images.COPY_PASS)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_COPY_PASS.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+C"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(SHOW_PASS, new ImageView(Images.SHOW_PASS)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_SHOW_PASS.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+S"));
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(SHOW_PASS, new ImageView(Images.SHOW_PASS)));
		ViewUtils.addDisableListener(mi, ViewUtils.entrySelected);
		mi.setOnAction(event -> ACTION_SHOW_PASS.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+S"));
		
		GlobalView.GROUP_ITEMS.add(mi = new MenuItem(GROUP_UP, new ImageView(Images.MOVE_UP)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupCanUp);
		mi.setOnAction(event -> ACTION_GROUP_UP.run());
		
		GlobalView.GROUP_ITEMS.add(mi = new MenuItem(GROUP_DOWN, new ImageView(Images.MOVE_DOWN)));
		ViewUtils.addDisableListener(mi, ViewUtils.groupCanDown);
		mi.setOnAction(event -> ACTION_GROUP_DOWN.run());
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(ENTRY_UP, new ImageView(Images.MOVE_UP)));
		ViewUtils.addDisableListener(mi, ViewUtils.entryCanUp);
		mi.setOnAction(event -> ACTION_ENTRY_UP.run());
		
		GlobalView.ENTRY_ITEMS.add(mi = new MenuItem(ENTRY_DOWN, new ImageView(Images.MOVE_DOWN)));
		ViewUtils.addDisableListener(mi, ViewUtils.entryCanDown);
		mi.setOnAction(event -> ACTION_ENTRY_DOWN.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(HELP, new ImageView(Images.HELP)));
		mi.setOnAction(event -> ACTION_HELP.run());
		mi.setAccelerator(KeyCombination.valueOf("F1"));
		
		GlobalView.ITEMS.add(mi = new MenuItem(PROPERTIES, new ImageView(Images.PROPERTIES)));
		ViewUtils.addDisableListener(mi, ViewUtils.cardConnected);
		mi.setOnAction(event -> ACTION_PROPERTIES.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(PREFERENCES, new ImageView(Images.PREFERENCES)));
		mi.setOnAction(event -> ACTION_PREFERENCES.run());
		
		GlobalView.ITEMS.add(mi = new MenuItem(ABOUT, new ImageView(Images.ABOUT)));
		mi.setOnAction(event -> ACTION_ABOUT.run());
		
		ViewUtils.cardConnected.set(false);
		ViewUtils.groupSelected.set(false);
		ViewUtils.groupCanUp.set(false);
		ViewUtils.groupCanDown.set(false);
		ViewUtils.entrySelected.set(false);
		ViewUtils.entryCanUp.set(false);
		ViewUtils.entryCanDown.set(false);
	}
	
	public static void cleanOnExiting() {
		ConnectionTimer.stop();
		if (appli != null)
			appli.disconnect();
	}
	public static void selectGroup(String groupName) {
		ViewUtils.groupSelected.set(groupName != null);
		GlobalView.getTableEntries().getItems().clear();
		if (groupName == null) {
			ViewUtils.groupCanUp.set(false);
			ViewUtils.groupCanDown.set(false);
			GlobalView.getGroupsView().getSelectionModel().clearSelection();
			return;
		}
		
		int size = GlobalView.getGroups().getChildren().size();
		int index = GlobalView.getGroupsView().getSelectionModel().getSelectedIndex();
		ViewUtils.groupCanUp.set(index != 0);
		ViewUtils.groupCanDown.set(index != size - 1);
		
		List<Entry> entries = appli.getEntries(appli.getGroup(groupName), true);
		GlobalView.getTableEntries().getItems().addAll(entries);
		EntryReader.newQueue();
		for(Entry e : entries)
			EntryReader.readEntry(e);
	}
	
	public static Group getCurrentSelectedGroupForUse() {
		return appli.getGroup(GlobalView.getGroupsView().getSelectionModel().getSelectedItem().getValue());
	}
	public static Entry getCurrentSelectedEntryForUse() {
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		while (!e.isInCache()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {}
		}
		return e;
	}
}
