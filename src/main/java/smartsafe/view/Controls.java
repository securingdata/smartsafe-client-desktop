package smartsafe.view;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import javax.smartcardio.CardTerminal;

import connection.APDUResponse;
import connection.loader.GPException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import smartsafe.Messages;
import smartsafe.comm.SmartSafeAppli;
import smartsafe.model.Entry;

public class Controls {
	private static final BooleanProperty cardConnected = new SimpleBooleanProperty();
	private static final BooleanProperty groupSelected = new SimpleBooleanProperty();
	private static final BooleanProperty entrySelected = new SimpleBooleanProperty();
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
			GlobalView.connectDialog();
		}
		else {
			appli.disconnect();
			appli = null;
			
			connection.setGraphic(new ImageView(Images.CONNECT));
			connection.setTooltip(new Tooltip(CONNECT));
			connectionMenu.setGraphic(new ImageView(Images.CONNECT));
			connectionMenu.setText(CONNECT);
		}
		cardConnected.set(appli != null);
		connection.setSelected(appli != null);
		
		//Clearing
		GlobalView.getGroups().getChildren().clear();
		
		//Updating only if the application still exists
		if (appli == null) {
			selectGroup(null);
			return;
		}
		connection.setGraphic(new ImageView(Images.DISCONNECT));
		connection.setTooltip(new Tooltip(Controls.DISCONNECT));
		connectionMenu.setGraphic(new ImageView(Images.DISCONNECT));
		connectionMenu.setText(Controls.DISCONNECT);
		for (String group : appli.getGroups())
			GlobalView.getGroups().getChildren().add(new TreeItem<String>(group));
		appli.getAivailableMemory();
	});
	
	public static final String NEW_GROUP = Messages.get("NEW_GROUP");
	public static final Action ACTION_NEW_GROUP = new Action(NEW_GROUP, false, null, params -> {
		GlobalView.newGroupDialog();
	});
	
	public static final String NEW_ENTRY = Messages.get("NEW_ENTRY");
	public static final Action ACTION_NEW_ENTRY = new Action(NEW_ENTRY, false, null, params -> {
		GlobalView.entryDialog(null);
	});
	
	public static final String DELETE = Messages.get("DELETE");
	public static final Action ACTION_DELETE = new Action(DELETE, false, null, params -> {
		TreeItem<String> group = GlobalView.getGroupsView().getSelectionModel().getSelectedItem();
		if (group != null) {
			Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
			GlobalView.deleteDialog(group, e);
		}
		
	});
	
	public static final String CHANGE_PIN = Messages.get("CHANGE_PIN");
	public static final Action ACTION_CHANGE_PIN = new Action(CHANGE_PIN, false, null, params -> {
		GlobalView.changePINDialog();
	});
	
	public static final String BACKUP = Messages.get("BACKUP");
	public static final Action ACTION_BACKUP = new Action(BACKUP, false, null, params -> {
		GlobalView.backupDialog();
	});
	
	public static final String INIT = Messages.get("INIT");
	public static final Action ACTION_INIT = new Action(INIT, false, null, params -> {
		
	});
	
	public static final String UPDATE = Messages.get("UPDATE");
	public static final Action ACTION_UPDATE = new Action(UPDATE, false, null, params -> {
		GlobalView.manageServerDialog();
	});
	
	public static final String EXIT = Messages.get("EXIT");
	public static final Action ACTION_EXIT = new Action(EXIT, false, null, params -> {
		if (appli != null)
			appli.disconnect();
		System.exit(0);
	});
	
	public static final String EDIT = Messages.get("EDIT");
	public static final Action ACTION_EDIT = new Action(EDIT, false, null, params -> {
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		appli.selectEntry(e);
		appli.getData(Entry.INDEX_PASSWORD);
		GlobalView.entryDialog(e);
		GlobalView.getTableEntries().getSelectionModel().select(null);
		GlobalView.getTableEntries().getSelectionModel().select(e);
	});
	
	public static final String GOTO = Messages.get("GOTO");
	public static final Action ACTION_GOTO = new Action(GOTO, false, null, params -> {
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		try {
			java.awt.Desktop.getDesktop().browse(new URI(e.getUrl().get()));
		}
		catch (IOException e1) {}
		catch (URISyntaxException e1) {}
	});
	
	public static final String COPY_USER = Messages.get("COPY_USER");
	public static final Action ACTION_COPY_USER = new Action(COPY_USER, false, null, params -> {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		clipboard.setContents(new StringSelection(e.getUserName().get()), null);
	});
	
	public static final String COPY_PASS = Messages.get("COPY_PASS");
	public static final Action ACTION_COPY_PASS = new Action(COPY_PASS, false, null, params -> {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		appli.selectEntry(e);
		appli.getData(Entry.INDEX_PASSWORD);
		clipboard.setContents(new StringSelection(e.getPassword().get()), null);
		e.maskPassword();
	});
	
	public static final String SHOW_PASS = Messages.get("SHOW_PASS");
	public static final Action ACTION_SHOW_PASS = new Action(SHOW_PASS, false, null, params -> {
		Entry e = GlobalView.getTableEntries().getSelectionModel().getSelectedItem();
		appli.selectEntry(e);
		appli.getData(Entry.INDEX_PASSWORD);
	});
	
	public static final String HELP = Messages.get("HELP");
	public static final Action ACTION_HELP = new Action(HELP, false, null, params -> {
		//TODO
	});
	
	public static final String PROPERTIES = Messages.get("PROPERTIES");
	public static final Action ACTION_PROPERTIES = new Action(PROPERTIES, false, null, params -> {
		//TODO
	});
	
	public static final String PREFERENCES = Messages.get("PREFERENCES");
	public static final Action ACTION_PREFERENCES = new Action(PREFERENCES, false, null, params -> {
		//TODO
	});
	
	public static final String ABOUT = Messages.get("ABOUT");
	public static final Action ACTION_ABOUT = new Action(ABOUT, false, null, params -> {
		GlobalView.aboutDialog();
	});
	//================== END ACTIONS ==================\\
	
	
	private static final List<ButtonBase> BUTTONS = new LinkedList<>();
	private static final List<MenuItem> ITEMS = new LinkedList<>();
	
	static {
		ButtonBase b;
		MenuItem mi;
		
		BUTTONS.add(b = connection = new ToggleButton("", new ImageView(Images.CONNECT)));
		b.setTooltip(new Tooltip(CONNECT));
		b.setOnAction(event-> ACTION_CONNECT.run());
		
		ITEMS.add(mi = connectionMenu = new MenuItem(CONNECT, new ImageView(Images.CONNECT)));
		mi.setOnAction(event -> ACTION_CONNECT.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+Q"));
		
		BUTTONS.add(b = new Button("", new ImageView(Images.NEW_GROUP)));
		addDisableListener(b, cardConnected);
		b.setTooltip(new Tooltip(NEW_GROUP));
		b.setOnAction(event -> ACTION_NEW_GROUP.run());
		
		ITEMS.add(mi = new MenuItem(NEW_GROUP, new ImageView(Images.NEW_GROUP)));
		addDisableListener(mi, cardConnected);
		mi.setOnAction(event -> ACTION_NEW_GROUP.run());
		
		BUTTONS.add(b = new Button("", new ImageView(Images.NEW_ENTRY)));
		addDisableListener(b, groupSelected);
		b.setTooltip(new Tooltip(NEW_ENTRY));
		b.setOnAction(event -> ACTION_NEW_ENTRY.run());
		
		ITEMS.add(mi = new MenuItem(NEW_ENTRY, new ImageView(Images.NEW_ENTRY)));
		addDisableListener(mi, groupSelected);
		mi.setOnAction(event -> ACTION_NEW_ENTRY.run());
		
		BUTTONS.add(b = new Button("", new ImageView(Images.DELETE)));
		addDisableListener(b, groupSelected);
		b.setTooltip(new Tooltip(DELETE));
		b.setOnAction(event -> ACTION_DELETE.run());
		
		ITEMS.add(mi = new MenuItem(DELETE, new ImageView(Images.DELETE)));
		addDisableListener(mi, groupSelected);
		mi.setOnAction(event -> ACTION_DELETE.run());
		mi.setAccelerator(KeyCombination.valueOf("Delete"));
		
		ITEMS.add(mi = new MenuItem(CHANGE_PIN, new ImageView(Images.PIN)));
		addDisableListener(mi, cardConnected);
		mi.setOnAction(event -> ACTION_CHANGE_PIN.run());
		
		ITEMS.add(mi = new MenuItem(BACKUP, new ImageView(Images.BACKUP)));
		addDisableListener(mi, cardConnected);
		mi.setOnAction(event -> ACTION_BACKUP.run());
		
		ITEMS.add(mi = new MenuItem(INIT, new ImageView(Images.INIT)));
		mi.setOnAction(event -> ACTION_INIT.run());
		
		ITEMS.add(mi = new MenuItem(UPDATE, new ImageView(Images.UPDATE)));
		mi.setOnAction(event -> ACTION_UPDATE.run());
		
		ITEMS.add(mi = new MenuItem(EXIT));
		mi.setOnAction(event -> ACTION_EXIT.run());
		mi.setAccelerator(KeyCombination.valueOf("Alt+F4"));
		
		ITEMS.add(mi = new MenuItem(EDIT, new ImageView(Images.EDIT)));
		addDisableListener(mi, entrySelected);
		mi.setOnAction(event -> ACTION_EDIT.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+E"));
		
		ITEMS.add(mi = new MenuItem(GOTO, new ImageView(Images.GOTO)));
		addDisableListener(mi, entrySelected);
		mi.setOnAction(event -> ACTION_GOTO.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+G"));
		
		ITEMS.add(mi = new MenuItem(COPY_USER, new ImageView(Images.COPY)));
		addDisableListener(mi, entrySelected);
		mi.setOnAction(event -> ACTION_COPY_USER.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+X"));
		
		ITEMS.add(mi = new MenuItem(COPY_PASS, new ImageView(Images.COPY_PASS)));
		addDisableListener(mi, entrySelected);
		mi.setOnAction(event -> ACTION_COPY_PASS.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+C"));
		
		ITEMS.add(mi = new MenuItem(SHOW_PASS, new ImageView(Images.SHOW_PASS)));
		addDisableListener(mi, entrySelected);
		mi.setOnAction(event -> ACTION_SHOW_PASS.run());
		mi.setAccelerator(KeyCombination.valueOf("Ctrl+S"));
		
		ITEMS.add(mi = new MenuItem(HELP, new ImageView(Images.HELP)));
		mi.setOnAction(event -> ACTION_HELP.run());
		mi.setAccelerator(KeyCombination.valueOf("F1"));
		
		ITEMS.add(mi = new MenuItem(PROPERTIES, new ImageView(Images.PROPERTIES)));
		addDisableListener(mi, cardConnected);
		mi.setOnAction(event -> ACTION_PROPERTIES.run());
		
		ITEMS.add(mi = new MenuItem(PREFERENCES, new ImageView(Images.PREFERENCES)));
		mi.setOnAction(event -> ACTION_PREFERENCES.run());
		
		ITEMS.add(mi = new MenuItem(ABOUT, new ImageView(Images.ABOUT)));
		mi.setOnAction(event -> ACTION_ABOUT.run());
		
		cardConnected.set(false);
		groupSelected.set(false);
		entrySelected.set(false);
	}
	
	public static ButtonBase getButton(String name) {
		for (ButtonBase b : BUTTONS)
			if (b.getTooltip().getText().equals(name))
				return b;
		return null;
	}
	public static MenuItem getMenuItem(String name) {
		for (MenuItem mi : ITEMS)
			if (mi.getText().equals(name))
				return mi;
		return null;
	}
	
	public static void handle(Action action) {
		action.run();
	}
	public static BooleanProperty getEntrySelectedProperty() {
		return entrySelected;
	}
	public static void createAppli(CardTerminal reader, String password) {
		appli = new SmartSafeAppli(reader);
		try {
			appli.coldReset();
			APDUResponse resp = appli.select();
			if (resp.getStatusWord() != (short) SmartSafeAppli.SW_NO_ERROR) {
				GlobalView.errorDialog(Messages.get("CONNECT_NO_APP"));
				appli = null;
				return;
			}
			resp = appli.authenticate(password);
			if (resp.getStatusWord() == (short) SmartSafeAppli.SW_NO_ERROR)
				return;
			else
				GlobalView.errorDialog(Messages.get("CONNECT_ERROR") + (int) (resp.getStatusWord() & 0xF));
			appli.disconnect();
		} catch (GPException e) {}
		appli = null;
	}
	public static void selectGroup(String groupName) {
		groupSelected.set(groupName != null);
		GlobalView.getTableEntries().getItems().clear();
		if (groupName == null) {
			return;
		}
		GlobalView.getTableEntries().getItems().addAll(appli.getEntries(groupName, true));
	}
	
	private static void addDisableListener(ButtonBase b, BooleanProperty prop) {
		b.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> b.setDisable(!newV.booleanValue()));
	}
	private static void addDisableListener(MenuItem mi, BooleanProperty prop) {
		mi.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> mi.setDisable(!newV.booleanValue()));
	}
}
