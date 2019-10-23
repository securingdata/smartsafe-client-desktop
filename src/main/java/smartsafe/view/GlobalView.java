package smartsafe.view;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.smartcardio.CardTerminal;

import connection.Connection;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import smartsafe.Messages;
import smartsafe.Version;
import smartsafe.comm.SmartSafeAppli;
import smartsafe.model.Entry;
import util.StringHex;

public class GlobalView {
	private static TreeView<String> groupsView;
	private static TreeItem<String> root;
	private static TableView<Entry> table;
	private static TitledPane details;
	private static Label lastUpdate, expiresOn;
	private static TextArea notes;
	
	public static Parent createView() {
		SplitPane centerPane = new SplitPane();
		centerPane.setOrientation(Orientation.VERTICAL);
		centerPane.getItems().addAll(getTableEntries(), getDetails());
		centerPane.setDividerPosition(0, 0.9);
		
		groupsView = new TreeView<>(root = new TreeItem<>());
		root.setExpanded(true);
		groupsView.setShowRoot(false);
		groupsView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			Controls.selectGroup(newValue == null ? null : newValue.getValue());
		});
		SplitPane mainPane = new SplitPane();
		mainPane.setOrientation(Orientation.HORIZONTAL);
		mainPane.getItems().addAll(groupsView, centerPane);
		mainPane.setDividerPosition(0, 0.2);
		
		ToolBar tb = new ToolBar();
		tb.getItems().add(Controls.getButton(Controls.CONNECT));
		tb.getItems().add(Controls.getButton(Controls.NEW_GROUP));
		tb.getItems().add(Controls.getButton(Controls.NEW_ENTRY));
		tb.getItems().add(Controls.getButton(Controls.DELETE));
		
		BorderPane rootPane = new BorderPane(mainPane);
		rootPane.setTop(tb);
		
		MenuBar mb = new MenuBar();
		Menu m;
		mb.getMenus().add(m = new Menu(Messages.get("MENU_FILE")));
		m.getItems().add(Controls.getMenuItem(Controls.CONNECT));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(Controls.getMenuItem(Controls.NEW_GROUP));
		m.getItems().add(Controls.getMenuItem(Controls.NEW_ENTRY));
		m.getItems().add(Controls.getMenuItem(Controls.DELETE));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(Controls.getMenuItem(Controls.EXIT));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_EDIT")));
		m.getItems().add(Controls.getMenuItem(Controls.EDIT));
		m.getItems().add(Controls.getMenuItem(Controls.GOTO));
		m.getItems().add(Controls.getMenuItem(Controls.COPY_USER));
		m.getItems().add(Controls.getMenuItem(Controls.COPY_PASS));
		m.getItems().add(Controls.getMenuItem(Controls.SHOW_PASS));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_HELP")));
		//preferences
		//stats
		//updates
		m.getItems().add(Controls.getMenuItem(Controls.HELP));
		m.getItems().add(Controls.getMenuItem(Controls.ABOUT));
		
		
		BorderPane superRoot = new BorderPane(rootPane);
		superRoot.setTop(mb);
		
		return superRoot;
	}
	public static TreeView<String> getGroupsView() {
		return groupsView;
	}
	public static TreeItem<String> getGroups() {
		return root;
	}
	public static TableView<Entry> getTableEntries() {
		if (table == null) {
			table = new TableView<>();
			
			TableColumn<Entry, String> column;
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_IDENTIFIER")));
			column.setCellValueFactory(cellData -> cellData.getValue().getIdentifier());
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_USER")));
			column.setCellValueFactory(cellData -> cellData.getValue().getUserName());
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_PASSWORD")));
			column.setCellValueFactory(cellData -> cellData.getValue().getPassword());
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_URL")));
			column.setCellValueFactory(cellData -> cellData.getValue().getUrl());
			
			//table.getSelectionModel().setCellSelectionEnabled(true);
			table.setPrefHeight(Double.MAX_VALUE);
			
			table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
				Controls.getEntrySelectedProperty().set(newValue != null);
				if (oldValue != null)
					oldValue.maskPassword();
				if (newValue == null) {
					lastUpdate.setText("-");
					expiresOn.setText("-");
					notes.setText("");
				}
				else {
					lastUpdate.setText(formatDate(newValue.getLastUpdate().get()));
					if (newValue.getExpiresDate().get() == null)
						expiresOn.setText(Messages.get("DETAILS_NEVER"));
					else
						expiresOn.setText(formatDate(newValue.getExpiresDate().get()));//TODO
					notes.setText(newValue.getNotes().get());
				}
				
			});
		}
		return table;
	}
	public static TitledPane getDetails() {
		if (details == null) {
			GridPane gp = new GridPane();
			Label l;
			gp.add(l = new Label(Messages.get("DETAILS_LAST_UPDATE")), 0, 0);
			gp.add(lastUpdate = new Label("-"), 1, 0);
			gp.add(new Label(Messages.get("DETAILS_EXPIRES_ON")), 0, 1);
			gp.add(expiresOn = new Label("-"), 1, 1);
			gp.add(new Label(Messages.get("DETAILS_NOTES")), 0, 2);
			gp.add(notes = new TextArea(), 1, 2);
			l.setMinWidth(75);
			notes.setEditable(false);
			
			details = new TitledPane(Messages.get("DETAILS"), gp);
			details.setGraphic(new ImageView(Images.DETAILS));
			details.setCollapsible(true);
		}
		return details;
	}
	private static String formatDate(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));//TODO
	}
	private static Dialog<ButtonType> errorDialog(String error) {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle(Messages.get("ERROR_DIALOG"));
		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.getButtonTypes().addAll(ButtonType.OK);
		dialogPane.setContentText(error);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.showAndWait();
		return dialog;
	}
	private static ButtonType initDialog(Dialog<?> dialog, Image image, String title) {
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(image);
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
		
		return ok;
	}
	
	public static void connectDialog() {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.CONNECT, Messages.get("CONNECT_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		List<CardTerminal> terminals = Connection.getTerminals();
		if (terminals == null || terminals.isEmpty()) {
			errorDialog(Messages.get("CONNECT_NO_READER"));
			return;
		}
		ComboBox<CardTerminal> readerList = new ComboBox<>();
		readerList.getItems().addAll(terminals);
		readerList.getSelectionModel().select(0);
		PasswordField password = new PasswordField();
		
		gp.add(new Label(Messages.get("CONNECT_SELECT_READER")), 0, 0);
		gp.add(readerList, 1, 0);
		gp.add(new Label(Messages.get("CONNECT_PASSWORD")), 0, 1);
		gp.add(password, 1, 1);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		password.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.trim().isEmpty());
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> password.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				Controls.createAppli(readerList.getValue(), password.getText());
			}
			return null;
		});
		
		dialog.showAndWait();
	}
	
	public static void newGroupDialog() {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.NEW_GROUP, Messages.get("GROUP_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		TextField groupName = new TextField();
		Spinner<Integer> groupSize = new Spinner<>(8, 255, 32);
		
		gp.add(new Label(Messages.get("GROUP_NAME")), 0, 0);
		gp.add(groupName, 1, 0);
		gp.add(new Label(Messages.get("GROUP_SIZE")), 0, 1);
		gp.add(groupSize, 1, 1);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		groupName.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.trim().isEmpty());
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> groupName.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				short sw = Controls.getAppli().createGroup(groupSize.getValue().byteValue(), groupName.getText()).getStatusWord();
				if (sw == SmartSafeAppli.SW_NO_ERROR) {
					GlobalView.getGroups().getChildren().add(new TreeItem<String>(groupName.getText()));
				}
				else if (sw == SmartSafeAppli.SW_FILE_FULL) {
					errorDialog(Messages.get("GROUP_ERROR_1"));
				}
				else {
					errorDialog(Messages.get("GROUP_ERROR_2") + new StringHex(sw).toString());
				}
			}
			return null;
		});
		
		dialog.showAndWait();
	}
	public static void entryDialog(Entry selectedEntry) {
		final String oldPass;
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok;
		if (selectedEntry == null)
			ok = initDialog(dialog, Images.NEW_ENTRY, Messages.get("NEW_ENTRY_DIALOG"));
		else
			ok = initDialog(dialog, Images.EDIT, Messages.get("EDIT_ENTRY_DIALOG"));
		
		
		HBox exp = new HBox(4);
		DatePicker expires = new DatePicker();
		expires.setMaxWidth(160);
		exp.getChildren().add(expires);
		Button b;
		exp.getChildren().add(b = new Button(Messages.get("T_PLUS_1_MONTH")));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(1)));
		exp.getChildren().add(b = new Button(Messages.get("T_PLUS_3_MONTHS")));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(3)));
		exp.getChildren().add(b = new Button(Messages.get("T_PLUS_6_MONTHS")));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(6)));
		exp.getChildren().add(b = new Button(Messages.get("T_PLUS_1_YEAR")));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(12)));
		
		HBox pass = new HBox(4);
		ToggleButton tb;
		PasswordField password = new PasswordField();
		TextField password2 = new TextField();
		password.setMinWidth(363);
		password2.setMinWidth(363);
		pass.getChildren().add(password);
		pass.getChildren().add(tb = new ToggleButton(Messages.get("ENTRY_SHOW")));
		tb.setOnAction(event -> {
			pass.getChildren().set(0, tb.isSelected() ? password2 : password);
		});
		pass.getChildren().add(new Button(Messages.get("ENTRY_RANDOM")));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		TextField identifier = new TextField();
		TextField userName = new TextField();
		TextField url = new TextField();
		TextArea notes = new TextArea();
		
		gp.add(new Label(Messages.get("ENTRY_IDENTIFIER")), 0, 0);
		gp.add(identifier, 1, 0);
		gp.add(new Label(Messages.get("ENTRY_USER_NAME")), 0, 1);
		gp.add(userName, 1, 1);
		gp.add(new Label(Messages.get("ENTRY_PASSWORD")), 0, 2);
		gp.add(pass, 1, 2);
		gp.add(new Label(Messages.get("ENTRY_EXPIRES")), 0, 3);
		gp.add(exp, 1, 3);
		gp.add(new Label(Messages.get("ENTRY_URL")), 0, 4);
		gp.add(url, 1, 4);
		gp.add(new Label(Messages.get("ENTRY_NOTES")), 0, 5);
		gp.add(notes, 1, 5);
		
		if (selectedEntry != null) {
			oldPass = selectedEntry.getPassword().get();
			identifier.setDisable(true);
			userName.setDisable(true);
			identifier.setText(selectedEntry.getIdentifier().get());
			userName.setText(selectedEntry.getUserName().get());
			password.setText(selectedEntry.getPassword().get());
			password2.setText(selectedEntry.getPassword().get());
			expires.setValue(selectedEntry.getExpiresDate().get());
			url.setText(selectedEntry.getUrl().get());
			notes.setText(selectedEntry.getNotes().get());
		}
		else
			oldPass = null;
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(selectedEntry == null);
		exp.setDisable(password.getText().isEmpty());

		identifier.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.trim().isEmpty() || userName.getText().trim().isEmpty());
		});
		userName.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.trim().isEmpty() || identifier.getText().trim().isEmpty());
		});
		password.textProperty().addListener((observable, oldValue, newValue) -> {
			exp.setDisable(newValue.isEmpty());
			if (password2.getText() != newValue)
				password2.setText(newValue);
		});
		password2.textProperty().addListener((observable, oldValue, newValue) -> {
			if (password.getText() != newValue)
				password.setText(newValue);
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> identifier.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				Entry entry = selectedEntry;
				if (entry == null) {
					entry = new Entry(identifier.getText(), userName.getText());
					short sw = Controls.getAppli().addEntry(Entry.NB_PROPERTIES, entry).getStatusWord();
					if (sw == SmartSafeAppli.SW_FILE_FULL) {
						errorDialog(Messages.get("ENTRY_ERROR_1"));
						return null;
					}
					else if (sw != SmartSafeAppli.SW_NO_ERROR) {
						errorDialog(Messages.get("ENTRY_ERROR_2") + new StringHex(sw).toString());
						return null;
					}
					table.getItems().add(entry);
				}
				
				if ((selectedEntry == null && !password.getText().isEmpty()) ||
					(selectedEntry != null && !password.getText().equals(oldPass))) {
					Controls.getAppli().setData(Entry.INDEX_PASSWORD, password.getText());
					Controls.getAppli().setData(Entry.INDEX_lAST_UPDATE, LocalDate.now().toString());
				}
				if (expires.getValue() != null) {
					Controls.getAppli().setData(Entry.INDEX_EXP_DATE, expires.getValue().toString());
				}
				Controls.getAppli().setData(Entry.INDEX_URL, url.getText());
				Controls.getAppli().setData(Entry.INDEX_NOTES, notes.getText());
				entry.maskPassword();
			}
			return null;
		});
		
		dialog.showAndWait();
	}
	public static void deleteDialog(TreeItem<String> group, Entry entry) {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.DELETE, Messages.get("DELETE_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		ToggleGroup tGroup = new ToggleGroup();
		RadioButton rGroup = new RadioButton(Messages.get("DELETE_GROUP") + group.getValue());
		RadioButton rEntry = new RadioButton(entry == null ? 
												Messages.get("DELETE_NO_ENTRY") : 
												Messages.get("DELETE_ENTRY") + entry.getFullIdentifier().replace("\n", "//"));
		if (entry == null) {
			rEntry.setDisable(true);
			rGroup.setSelected(true);
		}
		else {
			rEntry.setSelected(true);
		}
			
		rGroup.setToggleGroup(tGroup);
		rEntry.setToggleGroup(tGroup);
		
		gp.add(rGroup, 0, 0);
		gp.add(rEntry, 0, 1);
		
		
		dialog.getDialogPane().lookupButton(ok);

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> gp.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				if (tGroup.getSelectedToggle() == rGroup) {
					Controls.getAppli().deleteGroup(group.getValue());
					GlobalView.getGroups().getChildren().remove(group);
				}
				else {
					Controls.getAppli().deleteEntry(entry);
					table.getItems().remove(entry);
				}
			}
			return null;
		});
		
		dialog.showAndWait();
	}
	public static void aboutDialog() {
		Dialog<String> dialog = new Dialog<>();
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.ABOUT);
		dialog.setTitle(Messages.get("ABOUT_DIALOG"));
		dialog.setHeaderText(null);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok);
		
		GridPane gp = new GridPane();
		Label l;
		gp.setHgap(5);
		gp.setVgap(5);
		gp.add(new Label(Messages.get("ABOUT_SERVER")), 0, 0);
		if (Controls.getAppli() != null)
			gp.add(new Label(Controls.getAppli().getVersion()), 1, 0);
		else
			gp.add(new Label(Messages.get("ABOUT_NO_CARD")), 1, 0);

		gp.add(new Label(Messages.get("ABOUT_CLIENT")), 0, 1);
		gp.add(new Label(Version.version), 1, 1);
		gp.add(new Label(Messages.get("ABOUT_BUG")), 0, 2);
		gp.add(l = new Label("contact.smartthings@gmail.com"), 1, 2);
		l.setTextFill(Color.BLUE);
		dialog.getDialogPane().setContent(gp);
		
		dialog.showAndWait();
	}
}
