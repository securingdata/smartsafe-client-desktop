package smartsafe.view;


import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.smartcardio.CardTerminal;

import compiler.Compiler;
import compiler.CompilerException;
import compiler.project.Project;
import connection.Connection;
import connection.loader.GPCommands;
import connection.loader.GPException;
import connection.loader.SCP;
import connection.loader.SCP03;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
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
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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
		m.getItems().add(Controls.getMenuItem(Controls.UPDATE));
		m.getItems().add(Controls.getMenuItem(Controls.INIT));
		m.getItems().add(Controls.getMenuItem(Controls.CHANGE_PIN));
		m.getItems().add(Controls.getMenuItem(Controls.BACKUP));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(Controls.getMenuItem(Controls.EXIT));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_EDIT")));
		m.getItems().add(Controls.getMenuItem(Controls.EDIT));
		m.getItems().add(Controls.getMenuItem(Controls.GOTO));
		m.getItems().add(Controls.getMenuItem(Controls.COPY_USER));
		m.getItems().add(Controls.getMenuItem(Controls.COPY_PASS));
		m.getItems().add(Controls.getMenuItem(Controls.SHOW_PASS));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_HELP")));
		m.getItems().add(Controls.getMenuItem(Controls.HELP));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(Controls.getMenuItem(Controls.PROPERTIES));
		m.getItems().add(Controls.getMenuItem(Controls.PREFERENCES));
		m.getItems().add(new SeparatorMenuItem());
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
			column.prefWidthProperty().bind(table.widthProperty().divide(5));
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_USER")));
			column.setCellValueFactory(cellData -> cellData.getValue().getUserName());
			column.prefWidthProperty().bind(table.widthProperty().divide(5));
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_PASSWORD")));
			column.setCellValueFactory(cellData -> cellData.getValue().getPassword());
			column.prefWidthProperty().bind(table.widthProperty().divide(5));
			
			table.getColumns().add(column = new TableColumn<>(Messages.get("TABLE_URL")));
			column.setCellValueFactory(cellData -> cellData.getValue().getUrl());
			column.prefWidthProperty().bind(table.widthProperty().multiply(2).divide(5));
			
			table.setPrefHeight(Double.MAX_VALUE);
			table.setPlaceholder(new Label(""));
			
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
					else {
						expiresOn.setText(formatDate(newValue.getExpiresDate().get()));//TODO color
						if (LocalDate.now().isAfter(newValue.getExpiresDate().get())) {
							expiresOn.setTextFill(Color.RED);
						}
						else {
							expiresOn.setTextFill(Color.BLACK);
						}
					}
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
		return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));//TODO prefs
	}
	private static void addDisableListener(Node n, BooleanProperty prop) {
		n.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> n.setDisable(!newV.booleanValue()));
	}
	private static void bindTextAndPassField(TextField tf, PasswordField pf) {
		tf.textProperty().addListener((observable, oldValue, newValue) -> pf.setText(newValue));
		pf.textProperty().addListener((observable, oldValue, newValue) -> tf.setText(newValue));
	}
	private static ImageView createWarning() {
		ImageView iv = new ImageView(Images.WARNING);
		StackPane.setAlignment(iv, Pos.CENTER_RIGHT);
		return iv;
	}
	public static Dialog<ButtonType> errorDialog(String error) {
		return dialog(Messages.get("ERROR_DIALOG"), error);
	}
	private static Dialog<ButtonType> dialog(String title, String content) {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle(title);
		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.getButtonTypes().addAll(ButtonType.OK);
		dialogPane.setContentText(content);
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
			okButton.setDisable(newValue.isEmpty());
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
		pass.getChildren().add(b = new Button(Messages.get("ENTRY_RANDOM")));
		b.setOnAction(event -> randomDialog(password));
		
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
	public static void randomDialog(PasswordField pf) {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.EDIT, Messages.get("RANDOM_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);
		
		Button generate = new Button(Messages.get("RANDOM_GENERATE"));
		Spinner<Integer> passwordSize = new Spinner<>(1, 128, 16);
		TextField specialValues = new TextField("#$%?!/*=");
		final CheckBox num, alpha, upper, special;
		
		gp.add(new Label(Messages.get("RANDOM_SIZE")), 0, 0);
		gp.add(passwordSize, 1, 0);
		gp.add(num = new CheckBox(Messages.get("RANDOM_NUMERIC")), 0, 1);
		gp.add(alpha = new CheckBox(Messages.get("RANDOM_ALPHABETIC")), 0, 2);
		gp.add(upper = new CheckBox(Messages.get("RANDOM_UPPER")), 1, 2);
		gp.add(special = new CheckBox(Messages.get("RANDOM_SPECIAL")), 0, 3);
		gp.add(specialValues, 1, 3);
		
		num.setSelected(true);
		alpha.setSelected(true);
		upper.setSelected(true);
		
		BorderPane generationPane = new BorderPane();
		Label labelPass = new Label("");
		labelPass.setTextFill(Color.GRAY);
		BorderPane labelPane = new BorderPane();
		labelPane.setCenter(labelPass);
		labelPane.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(3))));
		
		generationPane.setCenter(labelPane);
		generationPane.setRight(generate);
		generationPane.setTop(new Label());
		
		BorderPane bp = new BorderPane();
		bp.setCenter(gp);
		bp.setBottom(generationPane);
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);
		generate.setOnAction(event -> {
			if (!num.isSelected() && !alpha.isSelected() && !upper.isSelected() && (!special.isSelected() || (specialValues.getText().isEmpty())))
				return;
			
			String newPass = "";
			String spe = specialValues.getText();
			for (int i = 0; i < passwordSize.getValue().intValue(); /*no ++*/) {
				switch(ThreadLocalRandom.current().nextInt(0, 4)) {
					case 0:
						if (num.isSelected()) {
							newPass += "" + ThreadLocalRandom.current().nextInt(0, 10);
							i++;
						}
						break;
					case 1:
						if (alpha.isSelected()) {
							newPass += (char) ('a' + ThreadLocalRandom.current().nextInt(0, 26));
							i++;
						}
						break;
					case 2:
						if (upper.isSelected()) {
							newPass += (char) ('A' + ThreadLocalRandom.current().nextInt(0, 26));
							i++;
						}
						break;
					case 3:
						if (special.isSelected()) {
							newPass += spe.charAt(ThreadLocalRandom.current().nextInt(0, spe.length()));
						}
						break;
				}
			}
			labelPass.setText(newPass);
			okButton.setDisable(false);
		});
		
		dialog.getDialogPane().setContent(bp);
		Platform.runLater(() -> generate.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				pf.setText(labelPass.getText());
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
					root.getChildren().remove(group);
					groupsView.getSelectionModel().clearSelection();
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
	
	public static void changePINDialog() {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.PIN, Messages.get("CHANGE_PIN_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		PasswordField password = new PasswordField();
		PasswordField password2 = new PasswordField();
		
		gp.add(new Label(Messages.get("CHANGE_PIN_NEW")), 0, 0);
		gp.add(password, 1, 0);
		gp.add(new Label(Messages.get("CHANGE_PIN_CONFIRM")), 0, 1);
		gp.add(password2, 1, 1);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		password.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.isEmpty() || !password.getText().equals(password2.getText()));
		});
		password2.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.isEmpty() || !password.getText().equals(password2.getText()));
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> password.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				Controls.getAppli().changePin(password.getText());
			}
			return null;
		});
		
		dialog.showAndWait();
	}
	
	public static void backupDialog() {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.BACKUP, Messages.get("BACKUP_DIALOG"));
		
		
		TextField file = new TextField();
		file.setPrefWidth(250);
		Button browse = new Button(Messages.get("BACKUP_BROWSE"));
		browse.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(Messages.get("BACKUP_CHOOSE"));
			File tmp = fileChooser.showSaveDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			if (tmp != null) {
				file.setText(tmp.getAbsolutePath());
			}
		});
		
		PasswordField password = new PasswordField();
		TextField password2 = new TextField();
		bindTextAndPassField(password2, password);
		ToggleButton show = new ToggleButton(Messages.get("MANAGE_SHOW"));
		show.setMaxWidth(Double.MAX_VALUE);
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);
		gp.add(new Label(Messages.get("BACKUP_CHOOSER")), 0, 0);
		gp.add(file, 1, 0);
		gp.add(browse, 2, 0);
		gp.add(new Label(Messages.get("BACKUP_PASSWORD")), 0, 1);
		gp.add(password, 1, 1);
		gp.add(show, 2, 1);
		gp.add(new Label(Messages.get("BACKUP_INFO")), 1, 2);
		
		show.setOnAction(event -> {
			gp.getChildren().remove(show.isSelected() ? password : password2);
			gp.add(show.isSelected() ? password2 : password, 1, 1);
		});
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		file.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.isEmpty() || password.getText().isEmpty());
		});
		password.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.isEmpty() || file.getText().isEmpty());
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> file.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				if (Controls.getAppli().backupData(file.getText(), password.getText()))
					dialog("Information", Messages.get("BACKUP_INFO2") + new File(file.getText()).getAbsolutePath());
				else
					errorDialog(Messages.get("BACKUP_ERROR"));
			}
			return null;
		});
		
		dialog.showAndWait();
	}
	private static void keyValidator(IntegerProperty loadValidation, StackPane keySp, ComboBox<String> keyComboBox, String newValue, int keyPosition) {
		ImageView iv = (ImageView) keySp.getChildren().get(1);
		boolean tmp;
		if (keyComboBox.getSelectionModel().getSelectedIndex() == 0) {
			int len = newValue.replaceAll(" ", "").length();
			tmp = (len != 16*2 && len != 16*2*3) || !newValue.matches("[0-9a-fA-F ]+");
		}
		else
			tmp = newValue.isEmpty();
		iv.setVisible(tmp);
		if (tmp)
			loadValidation.set(loadValidation.get() | keyPosition);
		else
			loadValidation.set(loadValidation.get() & ~keyPosition);
	}
	private static void bckpFileValidator(IntegerProperty loadValidation, StackPane bckpSp, String newValue, boolean active) {
		ImageView iv = (ImageView) bckpSp.getChildren().get(1);
		boolean tmp = active && (newValue.isEmpty() || !new File(newValue).exists());
		iv.setVisible(tmp);
		if (tmp)
			loadValidation.set(loadValidation.get() | 0x8);
		else
			loadValidation.set(loadValidation.get() & ~0x8);
	}
	public static void firstInitDialog() {
		
	}
	public static void manageServerDialog() {
		Dialog<String> dialog = new Dialog<>();
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.UPDATE);
		dialog.setTitle(Messages.get("MANAGE_DIALOG"));
		dialog.setHeaderText(null);
		
		IntegerProperty loadValidation = new SimpleIntegerProperty(0x1 | 0x2);
		
		ButtonType close = new ButtonType(Messages.get("MANAGE_CLOSE"), ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(close);
		
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
		
		gp.add(new Label(Messages.get("CONNECT_SELECT_READER")), 0, 0);
		gp.add(readerList, 1, 0);
		
		TextField dir = new TextField();
		StackPane spDir = new StackPane(dir, createWarning());
		dir.setPrefWidth(250);
		Button browse = new Button(Messages.get("MANAGE_BROWSE"));
		browse.setOnAction(event -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle(Messages.get("MANAGE_CHOOSE"));
			File tmp = directoryChooser.showDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			if (tmp != null) {
				dir.setText(tmp.getAbsolutePath());
			}
		});
		Button compile = new Button(Messages.get("MANAGE_COMPILE"));
		compile.setDisable(true);
		dir.textProperty().addListener((observable, oldValue, newValue) -> {
			ImageView iv = (ImageView) spDir.getChildren().get(1);
			boolean tmp = newValue.isEmpty() || !new File(dir.getText()).isDirectory();
			iv.setVisible(tmp);
			compile.setDisable(tmp);
			if (tmp)
				loadValidation.set(loadValidation.get() | 0x1);
			else
				loadValidation.set(loadValidation.get() & ~0x1);
		});
		gp.add(new Label(Messages.get("MANAGE_PROJECT_PATH")), 0, 1);
		gp.add(spDir, 1, 1);
		gp.add(browse, 2, 1);
		gp.add(compile, 3, 1);
		
		ComboBox<String> scpVersion = new ComboBox<>();
		scpVersion.getItems().add("SCP03");
		scpVersion.getSelectionModel().select(0);
		ComboBox<String> keyDerivation = new ComboBox<>();
		keyDerivation.getItems().addAll("No derivation", "EMVCPS v1.1", "VISA", "VISA2");
		keyDerivation.getSelectionModel().select(0);
		ComboBox<String> authMode = new ComboBox<>();
		authMode.getItems().addAll("Auth", "C-MAC", "C-ENC");
		authMode.getSelectionModel().select(1);
		HBox options = new HBox(2);
		options.getChildren().addAll(scpVersion, keyDerivation, authMode);
		gp.add(new Label(Messages.get("MANAGE_OPTIONS")), 0, 2);
		gp.add(options, 1, 2);
		
		PasswordField key1Pass = new PasswordField();
		TextField key1Text = new TextField();
		StackPane key1Sp = new StackPane(key1Pass, createWarning());
		bindTextAndPassField(key1Text, key1Pass);
		ToggleButton key1Show = new ToggleButton(Messages.get("MANAGE_SHOW"));
		key1Show.setMaxWidth(Double.MAX_VALUE);
		key1Show.setOnAction(event -> key1Sp.getChildren().set(0, key1Show.isSelected() ? key1Text : key1Pass));
		ComboBox<String> key1ComboBox = new ComboBox<>();
		key1ComboBox.setMaxWidth(Double.MAX_VALUE);
		key1ComboBox.getItems().addAll(Messages.get("MANAGE_PLAIN"), Messages.get("MANAGE_DERIVE"));
		key1ComboBox.getSelectionModel().select(0);
		
		key1ComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			keyValidator(loadValidation, key1Sp, key1ComboBox, key1Pass.getText(), 0x2);
		});
		key1Pass.textProperty().addListener((observable, oldValue, newValue) -> {
			keyValidator(loadValidation, key1Sp, key1ComboBox, newValue, 0x2);
		});
		gp.add(new Label(Messages.get("MANAGE_KEY_1")), 0, 3);
		gp.add(key1Sp, 1, 3);
		gp.add(key1Show, 2, 3);
		gp.add(key1ComboBox, 3, 3);
		
		PasswordField key2Pass = new PasswordField();
		TextField key2Text = new TextField();
		StackPane key2Sp = new StackPane(key2Pass, createWarning());
		key2Sp.getChildren().get(1).setVisible(false);
		bindTextAndPassField(key2Text, key2Pass);
		ToggleButton key2Show = new ToggleButton(Messages.get("MANAGE_SHOW"));
		key2Show.setOnAction(event -> key2Sp.getChildren().set(0, key2Show.isSelected() ? key2Text : key2Pass));
		key2Show.setMaxWidth(Double.MAX_VALUE);
		ComboBox<String> key2ComboBox = new ComboBox<>();
		key2ComboBox.setMaxWidth(Double.MAX_VALUE);
		key2ComboBox.getItems().addAll(Messages.get("MANAGE_PLAIN"), Messages.get("MANAGE_DERIVE"));
		key2ComboBox.getSelectionModel().select(0);
		key2ComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			keyValidator(loadValidation, key2Sp, key2ComboBox, key2Pass.getText(), 0x4);
		});
		key2Pass.textProperty().addListener((observable, oldValue, newValue) -> {
			keyValidator(loadValidation, key2Sp, key2ComboBox, newValue, 0x4);
		});
		CheckBox key2Check = new CheckBox(Messages.get("MANAGE_KEY_2"));
		key2Check.selectedProperty().addListener((observable, oldValue, newValue) -> {
			key2Sp.getChildren().get(1).setVisible(newValue.booleanValue());
			if (newValue)
				keyValidator(loadValidation, key2Sp, key2ComboBox, key2Pass.getText(), 0x4);
			else
				loadValidation.set(loadValidation.get() & ~0x4);
		});
		addDisableListener(key2Sp, key2Check.selectedProperty());
		addDisableListener(key2Show, key2Check.selectedProperty());
		addDisableListener(key2ComboBox, key2Check.selectedProperty());
		gp.add(key2Check, 0, 4);
		gp.add(key2Sp, 1, 4);
		gp.add(key2Show, 2, 4);
		gp.add(key2ComboBox, 3, 4);
		
		TextField bckpFile = new TextField();
		StackPane bckpSp = new StackPane(bckpFile, createWarning());
		bckpSp.getChildren().get(1).setVisible(false);
		bckpFile.textProperty().addListener((observable, oldValue, newValue) -> {
			bckpFileValidator(loadValidation, bckpSp, newValue, true);
		});
		Button browseBckp = new Button(Messages.get("MANAGE_BROWSE"));
		browseBckp.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(Messages.get("MANAGE_CHOOSE_BCKP"));
			File tmp = fileChooser.showOpenDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			if (tmp != null) {
				bckpFile.setText(tmp.getAbsolutePath());
			}
		});
		Label bckpLabel = new Label(Messages.get("MANAGE_BCKP_LABEL"));
		PasswordField bckpPass = new PasswordField();
		TextField bckpText = new TextField();
		bindTextAndPassField(bckpText, bckpPass);
		ToggleButton bckpShow = new ToggleButton(Messages.get("MANAGE_SHOW"));
		bckpShow.setOnAction(event -> {
			gp.getChildren().remove(bckpShow.isSelected() ? bckpPass : bckpText);
			gp.add(bckpShow.isSelected() ? bckpText : bckpPass, 1, 6);
		});
		bckpShow.setMaxWidth(Double.MAX_VALUE);
		CheckBox bckpCheck = new CheckBox(Messages.get("MANAGE_BCKP"));
		bckpCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
			bckpFileValidator(loadValidation, bckpSp, bckpFile.getText(), newValue.booleanValue());
		});
		addDisableListener(bckpSp, bckpCheck.selectedProperty());
		addDisableListener(browseBckp, bckpCheck.selectedProperty());
		addDisableListener(bckpLabel, bckpCheck.selectedProperty());
		addDisableListener(bckpPass, bckpCheck.selectedProperty());
		addDisableListener(bckpShow, bckpCheck.selectedProperty());
		gp.add(bckpCheck, 0, 5);
		gp.add(bckpSp, 1, 5);
		gp.add(browseBckp, 2, 5);
		gp.add(bckpLabel, 0, 6);
		gp.add(bckpPass, 1, 6);
		gp.add(bckpShow, 2, 6);
		
		PasswordField key3Pass = new PasswordField();
		TextField key3Text = new TextField();
		bindTextAndPassField(key3Text, key3Pass);
		ToggleButton key3Show = new ToggleButton(Messages.get("MANAGE_SHOW"));
		key3Show.setMaxWidth(Double.MAX_VALUE);
		key3Show.setOnAction(event -> {
			gp.getChildren().remove(key3Show.isSelected() ? key3Pass : key3Text);
			gp.add(key1Show.isSelected() ? key3Text : key3Pass, 1, 7);
		});
		Button load = new Button(Messages.get("MANAGE_LOAD"));
		load.setMaxWidth(Double.MAX_VALUE);
		load.setDisable(true);
		loadValidation.addListener((observable, oldValue, newValue) -> load.setDisable(newValue.intValue() != 0));
		gp.add(new Label(Messages.get("MANAGE_KEY_3")), 0, 7);
		gp.add(key3Pass, 1, 7);
		gp.add(key3Show, 2, 7);
		gp.add(load, 3, 7);
		
		CheckBox deleteOld = new CheckBox(Messages.get("MANAGE_DELETE"));
		gp.add(deleteOld, 0, 8);
		
		ProgressBar pb = new ProgressBar(0);
		pb.setMaxWidth(Double.MAX_VALUE);
		
		StringProperty sp = new SimpleStringProperty("");
		Compiler.setLogListener(sp);
		Connection.setLogListener(sp);
		SCP.setLogListener(sp);
		/*TextArea consoleContent = new TextArea();
		consoleContent.setEditable(false);
		consoleContent.setFont(Font.font("Courier New", 13));
		TitledPane console = new TitledPane(Messages.get("MANAGE_CONSOLE"), consoleContent);
		console.setGraphic(new ImageView(Images.DETAILS));
		console.setCollapsible(false);
		
		Button clear = new Button("Clear console");
		clear.setOnAction(event -> sp.set(""));
		BorderPane clearPane = new BorderPane();
		clearPane.setRight(clear);*/
		
		compile.setOnAction(event -> {
			new Thread((Runnable) () -> {
				pb.setProgress(0);
				Project p = new Project("SmartSafe", dir.getText());
				p.parsePckgs();
				pb.setProgress(0.3);
				p.getPackages().get(0).setAid(SmartSafeAppli.PACK_AID);
				p.getPackages().get(0).setAppletsAID(Collections.singletonList(SmartSafeAppli.APP_AID));
				pb.setProgress(0.5);
				try {
					p.build();
				} catch (CompilerException e) {
					errorDialog(e.getMessage());
				}
				pb.setProgress(1);
			}).start();
		});
		
		load.setOnAction(event -> {
			new Thread((Runnable) () -> {
				/*SmartSafeAppli ssa = new SmartSafeAppli(null);
		    	ssa.restoreData(bckpFile.getText(), bckpPass.getText());*/
		    	pb.setProgress(0);
		    	SCP scp = new SCP03();
				GPCommands gpc = new GPCommands(scp);
				scp.setStaticDerivation(SCP.StaticDerivation.EMVCPS1_1);
				//StringHex kmac, kenc, kdek;
				if (key1ComboBox.getSelectionModel().getSelectedIndex() == 0) {
					//kmac = new StringHex(key1Pass.getText());
					
				}
				else {
					
				}
				scp.addKey((short) 0, scp.instanciateKey(new StringHex("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f").toBytes()));
				scp.addKey((short) 1, scp.instanciateKey(new StringHex("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f").toBytes()));
				scp.addKey((short) 2, scp.instanciateKey(new StringHex("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f").toBytes()));
				
				try {
					scp.coldReset();
					scp.select("");
					scp.initUpdate((byte) 0, (byte) 0);
					scp.externalAuth(SCP.SEC_LEVEL_C_MAC);
					
					String packAid = SmartSafeAppli.PACK_AID.toString();
					String appAid = SmartSafeAppli.APP_AID.toString();
					if (deleteOld.isSelected()) {
						gpc.delete(appAid, true);
						gpc.delete(packAid, true);
					}
					gpc.installForLoad(packAid, "");
					gpc.loadCAP(GPCommands.getRawCap(dir.getText() + "/build/smartsafe/server/javacard/server.cap").toBytes());
					gpc.installForInstallAndMakeSelectable(packAid, appAid, appAid, "", "");
				} catch (GPException e) {
					errorDialog(e.getMessage());
				}
		    	pb.setProgress(1);
			}).start();
		});
		
		VBox main = new VBox(4, gp, pb/*, console, clearPane*/);
		
		dialog.getDialogPane().setContent(main);
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
