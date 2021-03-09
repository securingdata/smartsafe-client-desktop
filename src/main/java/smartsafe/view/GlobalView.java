package smartsafe.view;


import java.io.File;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import javax.smartcardio.CardTerminal;

import compiler.Compiler;
import compiler.CompilerException;
import compiler.project.Project;
import connection.APDUResponse;
import connection.Connection;
import connection.loader.GPCommands;
import connection.loader.GPException;
import connection.loader.SCP;
import connection.loader.SCP02;
import connection.loader.SCP03;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.chart.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import smartsafe.Messages;
import smartsafe.Prefs;
import smartsafe.Version;
import smartsafe.comm.SmartSafeAppli;
import smartsafe.controller.Controls;
import smartsafe.controller.EntryReader;
import smartsafe.model.Entry;
import util.Crypto;
import util.ResourcesManager;
import util.StringHex;

public class GlobalView {
	public static final List<ButtonBase> BUTTONS = new LinkedList<>();
	public static final List<MenuItem> ITEMS = new LinkedList<>();
	
	private static Scene scene;
	private static TreeView<String> groupsView;
	private static TreeItem<String> root;
	private static TableView<Entry> table;
	private static TextField searchField;
	private static TitledPane details;
	private static Label lastUpdate, expiresOn;
	private static TextArea notes;
	
	private static ButtonBase getButton(String name) {
		for (ButtonBase b : BUTTONS)
			if (b.getTooltip().getText().equals(name))
				return b;
		return null;
	}
	private static MenuItem getMenuItem(String name) {
		for (MenuItem mi : ITEMS)
			if (mi.getText().equals(name))
				return mi;
		return null;
	}
	
	public static void updateTheme() {
		updateTheme(scene);
	}
	public static void updateTheme(Scene scene) {
		String css = ResourcesManager.getURLFile("dark_theme.css").toString();
		scene.getStylesheets().remove(css);
		if (!Prefs.get(Prefs.KEY_THEME).equals(Prefs.DEFAULT_THEME)) {
	        scene.getStylesheets().add(css);
	    }
	}
	
	public static Scene getScene() {
		return scene;
	}
	public static Scene createView() {
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
		tb.getItems().add(getButton(Controls.CONNECT));
		tb.getItems().add(getButton(Controls.NEW_GROUP));
		tb.getItems().add(getButton(Controls.NEW_ENTRY));
		tb.getItems().add(getButton(Controls.DELETE));
		HBox hb = new HBox();
		HBox.setHgrow(hb, Priority.ALWAYS);
		tb.getItems().add(hb);
		tb.getItems().add(searchField = new TextField());
		ViewUtils.addDisableListener(searchField, ViewUtils.cardConnected);
		
		BorderPane rootPane = new BorderPane(mainPane);
		rootPane.setTop(tb);
		
		MenuBar mb = new MenuBar();
		Menu m;
		mb.getMenus().add(m = new Menu(Messages.get("MENU_FILE")));
		m.getItems().add(getMenuItem(Controls.CONNECT));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.NEW_GROUP));
		m.getItems().add(getMenuItem(Controls.NEW_ENTRY));
		m.getItems().add(getMenuItem(Controls.DELETE));
		m.getItems().add(new SeparatorMenuItem());
		if (Prefs.get(Prefs.KEY_ADM).equals(Prefs.ADM_LIST[1]))
			m.getItems().add(getMenuItem(Controls.UPDATE));
		m.getItems().add(getMenuItem(Controls.CHANGE_PIN));
		m.getItems().add(getMenuItem(Controls.BACKUP));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.EXIT));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_EDIT")));
		m.getItems().add(getMenuItem(Controls.EDIT));
		m.getItems().add(getMenuItem(Controls.GOTO));
		m.getItems().add(getMenuItem(Controls.COPY_USER));
		m.getItems().add(getMenuItem(Controls.COPY_PASS));
		m.getItems().add(getMenuItem(Controls.SHOW_PASS));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_HELP")));
		m.getItems().add(getMenuItem(Controls.HELP));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.PROPERTIES));
		m.getItems().add(getMenuItem(Controls.PREFERENCES));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.ABOUT));
		
		
		BorderPane superRoot = new BorderPane(rootPane);
		superRoot.setTop(mb);
		superRoot.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                event.consume();
                searchField.requestFocus();
                
                //In order to force refresh of selected entries
                String tmp = searchField.getText();
                searchField.setText("");
                searchField.setText(tmp);
                searchField.selectAll();
            }
        });
		searchField.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                if (searchField.getText().length() != 0) {
                	searchField.setText("");
                }
                else {
                	groupsView.requestFocus();
                }
            }
            else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.DOWN) {
            	table.requestFocus();
            	table.getSelectionModel().select(0);
            }
        });
		searchField.textProperty().addListener((observable, oldValue, newValue) -> {
			Controls.selectGroup(null);
			if (newValue.isEmpty())
				return;
			EntryReader.newQueue();
			newValue = normalise(newValue);
			Map<String, Entry> map = new HashMap<>();
			for (String group : Controls.getAppli().getGroups()) {
				for (Entry e : Controls.getAppli().getEntries(group, true)) {
					String tmp = normalise(e.getIdentifier().get());
					if (tmp.contains(newValue)) {
						GlobalView.getTableEntries().getItems().add(e);
						EntryReader.readEntry(e);
					} else
						map.put(tmp, e);//for search with typo
				}
			}
			
			if (newValue.length() < 3)//Search with typo active when 3 or more chars in the search bar 
				return;
			
			//Search with typo
			for (int i = 0; i < newValue.length(); i++) {
				Map<String, Entry> newMap = new HashMap<>();
				for (Map.Entry<String, Entry> mapEntry : map.entrySet()) {
					String regex = ".*" + newValue.substring(0, i) + "." + newValue.substring(i + 1) + ".*";
					if (mapEntry.getKey().matches(regex)) {
						GlobalView.getTableEntries().getItems().add(mapEntry.getValue());
						EntryReader.readEntry(mapEntry.getValue());
					} else
						newMap.put(mapEntry.getKey(), mapEntry.getValue());
				}
				map = newMap;
			}
		});
		
		scene = new Scene(superRoot, 700, 400);
		updateTheme();
		return scene;
	}
	private static String normalise(String s) {
		s = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
	    return s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	}
	public static TreeView<String> getGroupsView() {
		return groupsView;
	}
	public static TreeItem<String> getGroups() {
		return root;
	}
	public static TextField getSearchField() {
		return searchField;
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
				ViewUtils.entrySelected.set(newValue != null);
				if (oldValue != null)
					oldValue.maskPassword();
				lastUpdate.setText("-");
				expiresOn.setText("-");
				notes.setText("");
				if (newValue != null) {
					EntryReader.newQueue();
					EntryReader.readEntry(newValue);
					if (newValue.getLastUpdate().get() != null)
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
					if(newValue.getNotes().get() != null)
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
		dialog.initOwner(scene.getWindow());
		updateTheme(dialog.getDialogPane().getScene());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(image);
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
		
		return ok;
	}
	
	private static ComboBox<CardTerminal> getTerminals() {
		List<CardTerminal> terminals = Connection.getTerminals();
		if (terminals == null || terminals.isEmpty()) {
			errorDialog(Messages.get("CONNECT_NO_READER"));
			return null;
		}
		ComboBox<CardTerminal> readerList = new ComboBox<>();
		readerList.getItems().addAll(terminals);
		readerList.getSelectionModel().select(0);
		for (int i = 0; i < terminals.size(); i++) {
			if (terminals.get(i).getName() == Prefs.get(Prefs.KEY_READER))
				readerList.getSelectionModel().select(i);
		}
		return readerList;
	}
	
	public static Object[] connectDialog() {
		Dialog<Object[]> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.CONNECT, Messages.get("CONNECT_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		ComboBox<CardTerminal> readerList = getTerminals();
		if (readerList == null)
			return null;
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
				return new Object[] {readerList.getValue(), password.getText()};
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	public static String newGroupDialog() {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.NEW_GROUP, Messages.get("GROUP_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		TextField groupName = new TextField();
		
		gp.add(new Label(Messages.get("GROUP_NAME")), 0, 0);
		gp.add(groupName, 1, 0);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		groupName.textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(newValue.trim().isEmpty());
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> groupName.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok)
				return groupName.getText();
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	public static String[] entryDialog(final Entry selectedEntry) {
		Dialog<String[]> dialog = new Dialog<>();
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
				return new String[] {identifier.getText(), userName.getText(), password.getText(), LocalDate.now().toString(),
						expires.getValue() != null ? expires.getValue().toString() : null, url.getText(), notes.getText()};
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	public static void randomDialog(PasswordField pf) {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.EDIT, Messages.get("RANDOM_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);
		
		Button generate = new Button(Messages.get("RANDOM_GENERATE"));
		Spinner<Integer> passwordSize = new Spinner<>(1, 128, 16);
		TextField specialValues = new TextField(Prefs.get(Prefs.KEY_CHARS));
		Color color = Prefs.get(Prefs.KEY_THEME).equals(Prefs.DEFAULT_THEME) ? Color.GRAY : Color.WHITESMOKE;
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
		labelPass.setTextFill(color);
		BorderPane labelPane = new BorderPane();
		labelPane.setCenter(labelPass);
		labelPane.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(3))));
		
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
							i++;
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
	public static Object deleteDialog(TreeItem<String> group, Entry entry) {
		Dialog<Object> dialog = new Dialog<>();
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
					root.getChildren().remove(group);
					groupsView.getSelectionModel().clearSelection();
					return group.getValue();
				}
				else {
					table.getItems().remove(entry);
					return entry;
				}
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	public static String changePINDialog() {
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
				return password.getText();
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	public static String[] backupAndRestoreDialog() {
		Dialog<String[]> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.BACKUP);
		dialog.setTitle(Messages.get("BACKUP_RESTORE_DIALOG"));
		dialog.setHeaderText(null);
		
		ButtonType action;
		if (root.getChildren().isEmpty())
			action = new ButtonType(Messages.get("BACKUP_RESTORE"), ButtonData.OK_DONE);
		else
			action = new ButtonType(Messages.get("BACKUP_BACKUP"), ButtonData.OK_DONE);
		ButtonType close = new ButtonType(Messages.get("BACKUP_CANCEL"), ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().addAll(action, close);
		
		TextField file = new TextField();
		file.setPrefWidth(250);
		Button browse = new Button(Messages.get("BACKUP_BROWSE"));
		browse.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(Messages.get("BACKUP_CHOOSE"));
			File tmp;
			if (root.getChildren().isEmpty())
				tmp = fileChooser.showOpenDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			else
				tmp = fileChooser.showSaveDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			if (tmp != null) {
				file.setText(tmp.getAbsolutePath());
			}
		});
		
		PasswordField password = new PasswordField();
		TextField password2 = new TextField();
		ViewUtils.bindTextAndPassField(password2, password);
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
		
		show.setOnAction(event -> {
			gp.getChildren().remove(show.isSelected() ? password : password2);
			gp.add(show.isSelected() ? password2 : password, 1, 1);
		});
		
		Node actionButton = dialog.getDialogPane().lookupButton(action);
		actionButton.setDisable(true);

		file.textProperty().addListener((observable, oldValue, newValue) -> {
			actionButton.setDisable(newValue.isEmpty() || password.getText().isEmpty());
		});
		password.textProperty().addListener((observable, oldValue, newValue) -> {
			actionButton.setDisable(newValue.isEmpty() || file.getText().isEmpty());
		});

		dialog.getDialogPane().setContent(new VBox(4, gp));
		Platform.runLater(() -> file.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == action) {
				return new String[] {file.getText(), password.getText(), dialogButton.getText()};
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	public static void manageServerDialog() {
		Dialog<String> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.UPDATE);
		dialog.setTitle(Messages.get("MANAGE_DIALOG"));
		dialog.setHeaderText(null);
		
		IntegerProperty validator = new SimpleIntegerProperty(0x1);
		
		ButtonType close = new ButtonType(Messages.get("MANAGE_CLOSE"), ButtonData.CANCEL_CLOSE);
		ButtonType manage = new ButtonType(Messages.get("MANAGE_LOAD"), ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(close, manage);
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);
		
		ComboBox<CardTerminal> readerList = getTerminals();
		if (readerList == null)
			return;
		
		gp.add(new Label(Messages.get("CONNECT_SELECT_READER")), 0, 0);
		gp.add(readerList, 1, 0);
		
		TextField dir = new TextField();
		StackPane spDir = new StackPane(dir, ViewUtils.createWarning());
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
				validator.set(validator.get() | 0x1);
			else
				validator.set(validator.get() & ~0x1);
		});
		gp.add(new Label(Messages.get("MANAGE_PROJECT_PATH")), 0, 1);
		gp.add(spDir, 1, 1);
		gp.add(browse, 2, 1);
		gp.add(compile, 3, 1);
		
		ComboBox<String> scpVersion = new ComboBox<>();
		scpVersion.getItems().addAll("SCP02", "SCP03");
		scpVersion.getSelectionModel().select(1);
		TextField implem = new TextField("15");
		implem.setMaxWidth(30);
		ComboBox<String> keyDerivation = new ComboBox<>();
		keyDerivation.getItems().addAll("No derivation", "EMVCPS v1.1", "VISA", "VISA2");
		keyDerivation.getSelectionModel().select(0);
		ComboBox<String> authMode = new ComboBox<>();
		authMode.getItems().addAll("Auth", "C-MAC", "C-ENC");//, "C-ENC"
		authMode.getSelectionModel().select(1);
		ComboBox<String> jcVersion = new ComboBox<>();
		jcVersion.getItems().addAll("JC 2.2.1", "JC 2.2.2", "JC 3.0.2", "JC 3.0.4", "JC 3.0.5");
		jcVersion.getSelectionModel().select(0);
		HBox options = new HBox(2);
		options.getChildren().addAll(scpVersion, implem, keyDerivation, authMode, jcVersion);
		gp.add(new Label(Messages.get("MANAGE_OPTIONS")), 0, 2);
		gp.add(options, 1, 2);
		
		PasswordField key1Pass = new PasswordField();
		TextField key1Text = new TextField();
		StackPane key1Sp = new StackPane(key1Pass, ViewUtils.createWarning());
		key1Sp.getChildren().get(1).setVisible(false);
		ViewUtils.bindTextAndPassField(key1Text, key1Pass);
		key1Text.setText("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f");
		ToggleButton key1Show = new ToggleButton(Messages.get("MANAGE_SHOW"));
		key1Show.setMaxWidth(Double.MAX_VALUE);
		key1Show.setOnAction(event -> key1Sp.getChildren().set(0, key1Show.isSelected() ? key1Text : key1Pass));
		ComboBox<String> key1ComboBox = new ComboBox<>();
		key1ComboBox.setMaxWidth(Double.MAX_VALUE);
		key1ComboBox.getItems().addAll(Messages.get("MANAGE_PLAIN"), Messages.get("MANAGE_DERIVE"));
		key1ComboBox.getSelectionModel().select(0);
		
		key1ComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			ViewUtils.keyValidator(validator, key1Sp, key1ComboBox, key1Pass.getText(), 0x2);
		});
		key1Pass.textProperty().addListener((observable, oldValue, newValue) -> {
			ViewUtils.keyValidator(validator, key1Sp, key1ComboBox, newValue, 0x2);
		});
		gp.add(new Label(Messages.get("MANAGE_KEY_1")), 0, 3);
		gp.add(key1Sp, 1, 3);
		gp.add(key1Show, 2, 3);
		gp.add(key1ComboBox, 3, 3);
		
		PasswordField key2Pass = new PasswordField();
		TextField key2Text = new TextField();
		StackPane key2Sp = new StackPane(key2Pass, ViewUtils.createWarning());
		key2Sp.getChildren().get(1).setVisible(false);
		ViewUtils.bindTextAndPassField(key2Text, key2Pass);
		ToggleButton key2Show = new ToggleButton(Messages.get("MANAGE_SHOW"));
		key2Show.setOnAction(event -> key2Sp.getChildren().set(0, key2Show.isSelected() ? key2Text : key2Pass));
		key2Show.setMaxWidth(Double.MAX_VALUE);
		ComboBox<String> key2ComboBox = new ComboBox<>();
		key2ComboBox.setMaxWidth(Double.MAX_VALUE);
		key2ComboBox.getItems().addAll(Messages.get("MANAGE_PLAIN"), Messages.get("MANAGE_DERIVE"));
		key2ComboBox.getSelectionModel().select(0);
		key2ComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			ViewUtils.keyValidator(validator, key2Sp, key2ComboBox, key2Pass.getText(), 0x4);
		});
		key2Pass.textProperty().addListener((observable, oldValue, newValue) -> {
			ViewUtils.keyValidator(validator, key2Sp, key2ComboBox, newValue, 0x4);
		});
		CheckBox key2Check = new CheckBox(Messages.get("MANAGE_KEY_2"));
		key2Check.selectedProperty().addListener((observable, oldValue, newValue) -> {
			key2Sp.getChildren().get(1).setVisible(newValue.booleanValue());
			if (newValue)
				ViewUtils.keyValidator(validator, key2Sp, key2ComboBox, key2Pass.getText(), 0x4);
			else
				validator.set(validator.get() & ~0x4);
		});
		ViewUtils.addDisableListener(key2Sp, key2Check.selectedProperty());
		ViewUtils.addDisableListener(key2Show, key2Check.selectedProperty());
		ViewUtils.addDisableListener(key2ComboBox, key2Check.selectedProperty());
		gp.add(key2Check, 0, 4);
		gp.add(key2Sp, 1, 4);
		gp.add(key2Show, 2, 4);
		gp.add(key2ComboBox, 3, 4);
		
		Button manageB = (Button) dialog.getDialogPane().lookupButton(manage);
		manageB.setDisable(true);
		validator.addListener((observable, oldValue, newValue) -> manageB.setDisable(newValue.intValue() != 0));
		
		Label label = new Label();
		readerList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			SmartSafeAppli appli = new SmartSafeAppli(newValue);
			try {
				appli.coldReset();
				APDUResponse resp = appli.select();
				if (resp.getStatusWord() != (short) SmartSafeAppli.SW_NO_ERROR) {
					label.setText(Messages.get("MANAGE_ABS"));
					label.setTextFill(Color.DARKGREEN);
				}
				else {
					String tmp = Messages.get("MANAGE_PRES") + " " +appli.getVersion() + ") ";
					if (new StringHex(resp.getData()).toString().equals("DE CA")) {
						label.setText(tmp + Messages.get("MANAGE_NOT_PERSO"));
						label.setTextFill(Color.DARKORANGE);
					}
					else {
						label.setText(tmp + Messages.get("MANAGE_PERSO"));
						label.setTextFill(Color.DARKRED);
					}
				}
				appli.disconnect();
			} catch (GPException e) {}
		});
		readerList.getSelectionModel().clearSelection();
		readerList.getSelectionModel().select(0);
		
		ProgressBarWithText pb = new ProgressBarWithText();
		
		StringProperty sp = new SimpleStringProperty("");
		Compiler.setLogListener(sp);
		Connection.setLogListener(sp);
		TextArea consoleContent = new TextArea("");
		consoleContent.setEditable(false);
		consoleContent.setFont(Font.font("Courier New", 13));
		sp.addListener((observable, oldValue, newValue) -> consoleContent.appendText(newValue));
		
		VBox main = new VBox(4, gp, label, pb);
		
		Button showConsole = new Button(Messages.get("MANAGE_SHOW_CONSOLE"));
		showConsole.setOnAction(event -> {
			main.getChildren().set(3, consoleContent);
			dialog.getDialogPane().getScene().getWindow().sizeToScene();
		});
		BorderPane showConsolePane = new BorderPane();
		showConsolePane.setRight(showConsole);
		main.getChildren().add(showConsolePane);
		
		compile.setOnAction(event -> {
			new Thread((Runnable) () -> {
				pb.reset();
				pb.setText(Messages.get("MANAGE_COMPILE_1"));
				switch (jcVersion.getSelectionModel().getSelectedItem()) {
					case "JC 2.2.1":
						Compiler.changeJCVersion(Compiler.JC_221);
						break;
					case "JC 2.2.2":
						Compiler.changeJCVersion(Compiler.JC_222);
						break;
					case "JC 3.0.2":
						Compiler.changeJCVersion(Compiler.JC_302);
						break;
					case "JC 3.0.4":
						Compiler.changeJCVersion(Compiler.JC_304);
						break;
					case "JC 3.0.5":
						Compiler.changeJCVersion(Compiler.JC_305);
						break;
				}
				Project p = new Project("SmartSafe", dir.getText());
				p.parsePckgs();
				pb.setProgress(0.3);
				p.getPackages().get(0).setAid(Prefs.getPckgAID());
				p.getPackages().get(0).setAppletsAID(Collections.singletonList(Prefs.getAppAID()));
				pb.setProgress(0.5, Messages.get("MANAGE_COMPILE_2"));
				try {
					p.build();
					pb.setProgress(1, Messages.get("MANAGE_COMPILE_3"));
				} catch (CompilerException e) {
					pb.setProgress(1, Messages.get("MANAGE_COMPILE_4"));
					pb.setTextStyle(true);
				}
			}).start();
		});
		
		manageB.addEventFilter(ActionEvent.ACTION, event -> {
			event.consume();
			new Thread((Runnable) () -> {
				pb.reset();
		    	SCP scp;
				switch (scpVersion.getSelectionModel().getSelectedItem()) {
					case "SCP02":
						scp = new SCP02(readerList.getSelectionModel().getSelectedItem());
						break;
					case "SCP03":
						scp = new SCP03(readerList.getSelectionModel().getSelectedItem());
						break;
					default:
						//Should never happen
						return;
				}
				scp.setImplementationOption(Byte.parseByte(implem.getText(), 16));
				byte secLevel = SCP.SEC_LEVEL_NO;
				switch (authMode.getSelectionModel().getSelectedItem()) {
					case "C-ENC":
						secLevel |= SCP.SEC_LEVEL_C_DEC;
						//no break
					case "C-MAC":
						secLevel |= SCP.SEC_LEVEL_C_MAC;
						//no break
					case "Auth":
					default:
				}
				switch (keyDerivation.getSelectionModel().getSelectedItem()) {
					case "EMVCPS v1.1":
						scp.setStaticDerivation(SCP.StaticDerivation.EMVCPS1_1);
						break;
					case "VISA":
						scp.setStaticDerivation(SCP.StaticDerivation.VISA);
						break;
					case "VISA2":
						scp.setStaticDerivation(SCP.StaticDerivation.VISA2);
						break;
					case "No derivation":
					default:
						scp.setStaticDerivation(SCP.StaticDerivation.NO_DERIVATION);
				}
				GPCommands gpc = new GPCommands(scp);
				StringHex keys;
				if (key1ComboBox.getSelectionModel().getSelectedIndex() == 0) {
					keys = new StringHex(key1Pass.getText());
					if (keys.size() == 16)
						keys = new StringHex(keys.toString() + keys.toString() + keys.toString());
				}
				else {
					keys = Crypto.keyFromPassword(key1Pass.getText());
				}
				scp.addKey((short) 0, scp.instanciateKey(keys.get(0, 16).toBytes()));
				scp.addKey((short) 1, scp.instanciateKey(keys.get(16, 16).toBytes()));
				scp.addKey((short) 2, scp.instanciateKey(keys.get(32, 16).toBytes()));
				pb.setProgress(0.1);
				try {
					scp.coldReset();
					scp.select("");
					pb.setProgress(0.2, Messages.get("MANAGE_LOAD_1"));
					scp.initUpdate((byte) 0, (byte) 0);
					scp.externalAuth(secLevel);
					pb.setProgress(0.3, Messages.get("MANAGE_LOAD_2"));
					String packAid = Prefs.getPckgAID().toString();
					String appAid = Prefs.getAppAID().toString();
					gpc.delete(appAid, true);
					gpc.delete(packAid, true);
					pb.setProgress(0.4, Messages.get("MANAGE_LOAD_3"));
					gpc.installForLoad(packAid, "");
					pb.setProgress(0.9, 3000);
					gpc.loadCAP(GPCommands.getRawCap(dir.getText() + "/build/smartsafe/server/javacard/server.cap").toBytes());
					pb.setProgress(0.9, Messages.get("MANAGE_LOAD_4"));
					gpc.installForInstallAndMakeSelectable(packAid, appAid, appAid, "", "");
					pb.setProgress(1, Messages.get("MANAGE_LOAD_5"));
					
					SmartSafeAppli appli = new SmartSafeAppli(readerList.getSelectionModel().getSelectedItem());
					appli.coldReset();
					appli.select();
					appli.changePin("password");
					appli.disconnect();
				} catch (GPException e) {
					sp.set(e.getMessage());
					pb.setProgress(1, Messages.get("MANAGE_LOAD_6"));
					pb.setTextStyle(true);
					e.printStackTrace();
				}
			}).start();
		});
		
		dialog.getDialogPane().setContent(main);
		dialog.showAndWait();
	}
	
	public static void preferencesDialog() {
		Dialog<String> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.PREFERENCES);
		dialog.setTitle(Messages.get("PREFS_DIALOG"));
		dialog.setHeaderText(null);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		ButtonType reset = new ButtonType("Reset to default", ButtonData.APPLY);
		dialog.getDialogPane().getButtonTypes().addAll(reset, ok, ButtonType.CANCEL);
		
		
		ComboBox<String> language = new ComboBox<>();
		language.setMaxWidth(Double.MAX_VALUE);
		language.getItems().addAll(Prefs.LANGUAGES_LIST);
		language.getSelectionModel().select(Prefs.get(Prefs.KEY_LANGUAGE));
		
		ComboBox<String> theme = new ComboBox<>();
		theme.setMaxWidth(Double.MAX_VALUE);
		theme.getItems().addAll(Prefs.THEMES_LIST);
		theme.getSelectionModel().select(Prefs.get(Prefs.KEY_THEME));
		
		TextField chars = new TextField(Prefs.get(Prefs.KEY_CHARS));
		chars.setPrefWidth(250);
		
		TextField pckgAid = new TextField(Prefs.get(Prefs.KEY_PCKG_AID));
		pckgAid.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > 10)
				pckgAid.setText(newValue.substring(0, 10));
		});
		
		TextField appAidSuffix = new TextField(Prefs.get(Prefs.KEY_APP_AID_SUFFIX));
		appAidSuffix.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > 6)
				appAidSuffix.setText(newValue.substring(0, 10));
		});
		
		Spinner<Integer> timer = new Spinner<>(0, Integer.MAX_VALUE, Integer.valueOf(Prefs.get(Prefs.KEY_TIMER)));
		timer.setEditable(true);
		timer.setMaxWidth(Double.MAX_VALUE);
		timer.focusedProperty().addListener((s, ov, nv) -> {
		    if (nv) return;
		    String text = timer.getEditor().getText();
		    if (text == null || text.length() == 0)
		    	return;
		    SpinnerValueFactory<Integer> valueFactory = timer.getValueFactory();
		    if (valueFactory != null) {
		        StringConverter<Integer> converter = valueFactory.getConverter();
		        if (converter != null) {
		        	Integer value = converter.fromString(text);
		            valueFactory.setValue(value);
		        }
		    }
		});
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);
		gp.add(new Label(Messages.get("PREFS_LANGUAGE")), 0, 0);
		gp.add(language, 1, 0);
		gp.add(new Label(Messages.get("PREFS_THEME")), 0, 1);
		gp.add(theme, 1, 1);
		gp.add(new Label(Messages.get("PREFS_CHARS")), 0, 2);
		gp.add(chars, 1, 2);
		if (Prefs.get(Prefs.KEY_ADM).equals(Prefs.ADM_LIST[1])) {
			gp.add(new Label(Messages.get("PREFS_PCKG_AID")), 0, 3);
			gp.add(pckgAid, 1, 3);
			gp.add(new Label(Messages.get("PREFS_APP_AID")), 0, 4);
			gp.add(appAidSuffix, 1, 4);
		}
		gp.add(new Label(Messages.get("PREFS_TIMER")), 0, 5);
		gp.add(timer, 1, 5);

		dialog.getDialogPane().setContent(gp);
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				Prefs.put(Prefs.KEY_LANGUAGE, language.getSelectionModel().getSelectedItem());
				Prefs.put(Prefs.KEY_THEME, theme.getSelectionModel().getSelectedItem());
				Prefs.put(Prefs.KEY_CHARS, chars.getText());
				if (Prefs.get(Prefs.KEY_ADM).equals(Prefs.ADM_LIST[1])) {
					Prefs.put(Prefs.KEY_PCKG_AID, pckgAid.getText());
					Prefs.put(Prefs.KEY_APP_AID_SUFFIX, appAidSuffix.getText());
				}
				Prefs.put(Prefs.KEY_TIMER, timer.getValue().toString());
			}
			else if (dialogButton == reset) {
				Prefs.put(Prefs.KEY_LANGUAGE, Prefs.DEFAULT_LANGUAGE);
				Prefs.put(Prefs.KEY_THEME, Prefs.DEFAULT_THEME);
				Prefs.put(Prefs.KEY_CHARS, Prefs.DEFAULT_CHARS);
				if (Prefs.get(Prefs.KEY_ADM).equals(Prefs.ADM_LIST[1])) {
					Prefs.put(Prefs.KEY_PCKG_AID, Prefs.DEFAULT_PCKG_AID);
					Prefs.put(Prefs.KEY_APP_AID_SUFFIX, Prefs.DEFAULT_APP_AID_SUFFIX);
				}
				Prefs.put(Prefs.KEY_TIMER, Prefs.DEFAULT_TIMER);
			}
			Help.clearCache();
			Messages.reloadMessages();
			updateTheme();
			return null;
		});
		
		dialog.showAndWait();
	}

	public static void propertiesDialog(SmartSafeAppli appli) {
		Dialog<String> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.PROPERTIES);
		dialog.setTitle(Messages.get("PROP_DIALOG"));
		dialog.setHeaderText(null);
		
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok);
		
		ProgressDialog d = new ProgressDialog(Messages.get("PROP_LOADING"), Images.PROPERTIES);
		Map<String, Short> footprints = new HashMap<>();
		
		if (EntryReader.getRemaining() != 0) {
			new Thread((Runnable) () -> {
				int remaining = EntryReader.getRemaining();
				double total = remaining;
				do {
					d.setProgress(1.0 * ((total - remaining) / total));
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				} while ((remaining = EntryReader.getRemaining()) != 0);
				d.closeDialog();
			}).start();
			d.showAndWait();
		}
		
		for (String group : appli.getGroups()) {
			short groupSize = 0;
			for (Entry e : appli.getEntries(group, true)) {
				groupSize += e.getIdentifier().getValue().length();
				groupSize += e.getUserName().getValue().length();
				groupSize += e.getPassword().getValue().length();
				groupSize += e.getUrl().getValue().length();
				groupSize += e.getNotes().getValue().length();
				groupSize += 20;//~Dates
			}
			footprints.put(group, Short.valueOf(groupSize));
		}
		
		int freeSpace = appli.getAivailableMemory();
		int usedSpace = 1;
		for (Map.Entry<String, Short> e : footprints.entrySet()) {
			usedSpace += e.getValue();
		}
		
		double totalSpace = freeSpace + usedSpace;
		ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(
                new PieChart.Data(Messages.get("PROP_FREE"), freeSpace * 100 / totalSpace),
                new PieChart.Data(Messages.get("PROP_USED"), usedSpace * 100 / totalSpace));
        PieChart chart = new PieChart(pieChartData);
        chart.setTitle(Messages.get("PROP_TOKEN"));
        chart.setLegendVisible(false);
        
        ObservableList<PieChart.Data> pieChartData2 = FXCollections.observableArrayList();
        for (Map.Entry<String, Short> e : footprints.entrySet()) {
        	pieChartData2.add(new PieChart.Data(e.getKey(), e.getValue() * 100 / usedSpace));
        }
        PieChart chart2 = new PieChart(pieChartData2);
        chart2.setTitle(Messages.get("PROP_GROUP"));
        chart2.setLegendVisible(false);
		
		
        GridPane gp = new GridPane();
        gp.add(chart, 0, 0);
        gp.add(chart2, 1, 0);
		
		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> pieChartData.get(0).getNode().setStyle("-fx-pie-color: #6495ED;"));
		dialog.showAndWait();
	}
	
	public static void aboutDialog(String version) {
		Dialog<String> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
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
		gp.add(new Label(version == null ? Messages.get("ABOUT_NO_CARD") : version), 1, 0);

		gp.add(new Label(Messages.get("ABOUT_CLIENT")), 0, 1);
		gp.add(new Label(Version.version), 1, 1);
		gp.add(new Label(Messages.get("ABOUT_BUG")), 0, 2);
		gp.add(l = new Label("contact.securingdata@gmail.com"), 1, 2);
		l.setTextFill(Color.BLUE);
		
		ImageView iv = new ImageView(Images.SMARTSAFE);
		iv.setPreserveRatio(true);
		iv.setFitWidth(260);
		
		BorderPane bp = new BorderPane(gp);
		bp.setTop(iv);
		
		dialog.getDialogPane().setContent(bp);
		dialog.showAndWait();
	}
}
