package fr.securingdata.smartsafe.view;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import javax.smartcardio.CardTerminal;

import fr.securingdata.smartsafe.Messages;
import fr.securingdata.smartsafe.Prefs;
import fr.securingdata.smartsafe.Version;
import fr.securingdata.smartsafe.comm.SmartSafeAppli;
import fr.securingdata.smartsafe.controller.Controls;
import fr.securingdata.smartsafe.controller.EntryReader;
import fr.securingdata.smartsafe.model.Entry;
import fr.securingdata.smartsafe.model.Group;
import fr.securingdata.smartsafe.util.ResourcesManager;
import fr.securingdata.smartsafe.view.AdvancedTextField.State;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class GlobalView {
	public static final List<ButtonBase> BUTTONS = new LinkedList<>();
	public static final List<MenuItem> ITEMS = new LinkedList<>();
	public static final List<MenuItem> GROUP_ITEMS = new LinkedList<>();
	public static final List<MenuItem> ENTRY_ITEMS = new LinkedList<>();
	
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
	private static MenuItem getMenuItem(String name, List<MenuItem> from) {
		for (MenuItem mi : from)
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
		
		ContextMenu groupsContextMenu = new ContextMenu();
		groupsContextMenu.getItems().add(getMenuItem(Controls.NEW_GROUP, GROUP_ITEMS));
		groupsContextMenu.getItems().add(getMenuItem(Controls.NEW_ENTRY, GROUP_ITEMS));
		groupsContextMenu.getItems().add(getMenuItem(Controls.DELETE, GROUP_ITEMS));
		groupsContextMenu.getItems().add(getMenuItem(Controls.RENAME_GROUP, GROUP_ITEMS));
		groupsContextMenu.getItems().add(new SeparatorMenuItem());
		groupsContextMenu.getItems().add(getMenuItem(Controls.GROUP_UP, GROUP_ITEMS));
		groupsContextMenu.getItems().add(getMenuItem(Controls.GROUP_DOWN, GROUP_ITEMS));
		groupsView.setOnMousePressed(event -> {
			groupsContextMenu.hide();
		});
		groupsView.setOnContextMenuRequested(event -> {
			groupsContextMenu.show(groupsView, event.getScreenX(), event.getScreenY());
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
		m.getItems().add(getMenuItem(Controls.CONNECT, ITEMS));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.NEW_GROUP, ITEMS));
		m.getItems().add(getMenuItem(Controls.NEW_ENTRY, ITEMS));
		m.getItems().add(getMenuItem(Controls.DELETE, ITEMS));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.CHANGE_PIN, ITEMS));
		m.getItems().add(getMenuItem(Controls.BACKUP, ITEMS));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.EXIT, ITEMS));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_EDIT")));
		m.getItems().add(getMenuItem(Controls.RENAME_GROUP, ITEMS));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.EDIT, ITEMS));
		m.getItems().add(getMenuItem(Controls.GOTO, ITEMS));
		m.getItems().add(getMenuItem(Controls.COPY_USER, ITEMS));
		m.getItems().add(getMenuItem(Controls.COPY_PASS, ITEMS));
		m.getItems().add(getMenuItem(Controls.SHOW_PASS, ITEMS));
		mb.getMenus().add(m = new Menu(Messages.get("MENU_HELP")));
		m.getItems().add(getMenuItem(Controls.HELP, ITEMS));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.PROPERTIES, ITEMS));
		m.getItems().add(getMenuItem(Controls.PREFERENCES, ITEMS));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(getMenuItem(Controls.ABOUT, ITEMS));
		
		
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
			for (Group group : Controls.getAppli().getGroups()) {
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
			
			Menu menu;
			ContextMenu entriesContextMenu = new ContextMenu();
			entriesContextMenu.getItems().add(getMenuItem(Controls.EDIT, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(getMenuItem(Controls.GOTO, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(getMenuItem(Controls.COPY_USER, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(getMenuItem(Controls.COPY_PASS, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(getMenuItem(Controls.SHOW_PASS, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(new SeparatorMenuItem());
			entriesContextMenu.getItems().add(getMenuItem(Controls.ENTRY_UP, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(getMenuItem(Controls.ENTRY_DOWN, ENTRY_ITEMS));
			entriesContextMenu.getItems().add(menu = new Menu(Messages.get("ENTRY_MOVE_TO"), new ImageView(Images.MOVE_TO)));
			ViewUtils.addDisableListener(menu, ViewUtils.entrySelected);
			table.setOnMousePressed(event -> {
				entriesContextMenu.hide();
			});
			table.setOnContextMenuRequested(event -> {
				MenuItem mi;
				menu.getItems().clear();
				for (Group g : Controls.getAppli().getGroups()) {
					if (g != Controls.getCurrentSelectedGroupForUse()) {
						menu.getItems().add(mi = new MenuItem(g.name));
						mi.setOnAction(actionEvent -> {
							MenuItem source = (MenuItem) actionEvent.getSource();
							Controls.ACTION_ENTRY_MOVE_TO.setParams(source.getText());
							Controls.ACTION_ENTRY_MOVE_TO.run();
						});
					}
				}
				
				entriesContextMenu.show(table, event.getScreenX(), event.getScreenY());
			});
			
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
				ViewUtils.entryCanUp.set(false);
				ViewUtils.entryCanDown.set(false);
				if (oldValue != null)
					oldValue.maskPassword();
				lastUpdate.setText("-");
				expiresOn.setText("-");
				notes.setText("");
				if (newValue != null) {
					int size = table.getItems().size();
					int index = table.getSelectionModel().getSelectedIndex();
					ViewUtils.entryCanUp.set(index != 0);
					ViewUtils.entryCanDown.set(index != size - 1);
					
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
	
	private static ButtonType initDialog(Dialog<?> dialog, Image image, String title) {
		dialog.initOwner(scene.getWindow());
		updateTheme(dialog.getDialogPane().getScene());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(0, image);
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
		
		return ok;
	}
	
	public static Object[] connectDialog() {
		Dialog<Object[]> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.CONNECT, Messages.get("CONNECT_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		ComboBox<CardTerminal> readerList = ViewUtils.getTerminals();
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
	
	public static String groupDialog(boolean rename) {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok;
		if (rename)
			ok = initDialog(dialog, Images.RENAME, Messages.get("GROUP_DIALOG_RENAME"));
		else
			ok = initDialog(dialog, Images.NEW_GROUP, Messages.get("GROUP_DIALOG_NEW"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		AdvancedTextField groupName = new AdvancedTextField();
		groupName.setState(State.ERROR, Messages.get("GROUP_TFI_ERROR_1"));
		
		gp.add(new Label(Messages.get("GROUP_NAME")), 0, 0);
		gp.add(groupName, 1, 0);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		groupName.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			okButton.setDisable(true);
			if (newValue.trim().isEmpty()) {
				groupName.setState(State.ERROR, Messages.get("GROUP_TFI_ERROR_1"));
				return;
			}
			else if (Controls.getAppli().getGroup(newValue) != null) {
				groupName.setState(State.ERROR, Messages.get("GROUP_TFI_ERROR_2"));
				return;
			}
			else if (newValue.length() > 30) {
				groupName.setState(State.ERROR, Messages.get("GROUP_TFI_ERROR_3"));
				return;
			}
			groupName.setState(State.ACCEPT);
			okButton.setDisable(false);
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> groupName.getTextField().requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok)
				return groupName.getTextField().getText();
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
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(selectedEntry == null);
		
		HBox exp = new HBox(4);
		DatePicker expires = new DatePicker();
		expires.setEditable(false);
		exp.setDisable(true);
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
		AdvancedTextField password = new AdvancedTextField(346, 0, true, true, false);
		password.setState(State.ACCEPT);
		
		pass.getChildren().add(password);
		pass.getChildren().add(b = new Button(Messages.get("ENTRY_RANDOM")));
		b.setOnAction(event -> randomDialog(password.getTextField()));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		AdvancedTextField identifier = new AdvancedTextField(460, true, false);
		identifier.setState(State.ERROR, Messages.get("ENTRY_IDENTIFIER_ERROR_1"));
		AdvancedTextField userName = new AdvancedTextField(460, true, false);
		userName.setState(State.ERROR, Messages.get("ENTRY_USER_NAME_ERROR_1"));
		TextField url = new TextField();
		TextArea notes = new TextArea();
		notes.setPrefHeight(100);
		
		password.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			exp.setDisable(newValue.isEmpty());
			if (newValue.length() > 200)
				password.setState(State.ERROR, Messages.get("ENTRY_PASSWORD_ERROR_1"));
			else
				password.setState(State.ACCEPT, null);
			
			okButton.setDisable(identifier.error() || userName.error() || password.error());
		});
		
		ChangeListener<? super String> listener = (observable, oldValue, newValue) -> {
			identifier.setState(State.ACCEPT, null);
			userName.setState(State.ACCEPT, null);
			
			if (identifier.getTextField().getText().trim().isEmpty())
				identifier.setState(State.ERROR, Messages.get("ENTRY_IDENTIFIER_ERROR_1"));
			else if (identifier.getTextField().getText().length() > 64)
				identifier.setState(State.ERROR, Messages.get("ENTRY_IDENTIFIER_ERROR_2"));
			if (userName.getTextField().getText().trim().isEmpty())
				userName.setState(State.ERROR, Messages.get("ENTRY_USER_NAME_ERROR_1"));
			else if (userName.getTextField().getText().length() > 64)
				userName.setState(State.ERROR, Messages.get("ENTRY_USER_NAME_ERROR_2"));
			
			String fullId = identifier.getTextField().getText() + Entry.SEPARATOR + userName.getTextField().getText();
			for (Group g : Controls.getAppli().getGroups()) {
				for (Entry e : g.entries) {
					if (e != selectedEntry && e.getFullIdentifier().equals(fullId)) {
						identifier.setState(State.ERROR, Messages.get("ENTRY_FULL_ID_ERROR_1"));
						userName.setState(State.ERROR, Messages.get("ENTRY_FULL_ID_ERROR_1"));
					}
				}
			}
			
			okButton.setDisable(identifier.error() || userName.error() || password.error());
		};
		identifier.getTextField().textProperty().addListener(listener);
		userName.getTextField().textProperty().addListener(listener);
		url.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > 200)
				url.setText(newValue.substring(0, 200));
		});
		notes.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > 200)
				notes.setText(newValue.substring(0, 200));
		});
		
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
			identifier.getTextField().setText(selectedEntry.getIdentifier().get());
			userName.getTextField().setText(selectedEntry.getUserName().get());
			password.getTextField().setText(selectedEntry.getPassword().get());
			expires.setValue(selectedEntry.getExpiresDate().get());
			url.setText(selectedEntry.getUrl().get());
			notes.setText(selectedEntry.getNotes().get());
		}

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> identifier.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				return new String[] {identifier.getTextField().getText(), 
									 userName.getTextField().getText(), 
									 password.getTextField().getText(), 
									 LocalDate.now().toString(),
									 expires.getValue() != null ? expires.getValue().toString() : null, 
									 url.getText(), 
									 notes.getText()};
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	public static void randomDialog(TextField pf) {
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
						if (special.isSelected() && !spe.isEmpty()) {
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
	public static Object deleteDialog(Group group, Entry entry) {
		Dialog<Object> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.DELETE, Messages.get("DELETE_DIALOG"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		ToggleGroup tGroup = new ToggleGroup();
		RadioButton rGroup = new RadioButton(Messages.get("DELETE_GROUP") + group.name);
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
					return group;
				}
				else {
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

		AdvancedTextField password = new AdvancedTextField(true, false, false);
		AdvancedTextField password2 = new AdvancedTextField(true, false, false);
		password.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_1"));
		password2.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_1"));
		
		gp.add(new Label(Messages.get("CHANGE_PIN_NEW")), 0, 0);
		gp.add(password, 1, 0);
		gp.add(new Label(Messages.get("CHANGE_PIN_CONFIRM")), 0, 1);
		gp.add(password2, 1, 1);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

		password.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.trim().isEmpty())
				password.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_1"));
			else if (newValue.length() < 8)
				password.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_3"));
			else if (newValue.length() < 16)
				password.setState(State.WARNING, Messages.get("CHANGE_PIN_WARNING_1"));
			else
				password.setState(State.ACCEPT, null);
			
			if (password2.getTextField().getText().trim().isEmpty())
				password2.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_1"));
			else if (!newValue.equals(password2.getTextField().getText()))
				password2.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_2"));
			else
				password2.setState(State.ACCEPT, null);
			
			okButton.setDisable(password.error() || password2.error());
		});
		password2.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.trim().isEmpty())
				password2.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_1"));
			else if (!newValue.equals(password.getTextField().getText()))
				password2.setState(State.ERROR, Messages.get("CHANGE_PIN_ERROR_2"));
			else
				password2.setState(State.ACCEPT, null);
			
			okButton.setDisable(password.error() || password2.error());
		});

		dialog.getDialogPane().setContent(gp);
		Platform.runLater(() -> password.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ok) {
				return password.getTextField().getText();
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
		double btnSize = Prefs.get(Prefs.KEY_LANGUAGE).equals(Prefs.DEFAULT_LANGUAGE) ? 60 : 70;
		Dialog<String[]> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(0, Images.BACKUP);
		dialog.setTitle(Messages.get("BACKUP_RESTORE_DIALOG"));
		dialog.setHeaderText(null);
		
		ButtonType action;
		if (root.getChildren().isEmpty())
			action = new ButtonType(Messages.get("BACKUP_RESTORE"), ButtonData.OK_DONE);
		else
			action = new ButtonType(Messages.get("BACKUP_BACKUP"), ButtonData.OK_DONE);
		ButtonType close = new ButtonType(Messages.get("BACKUP_CANCEL"), ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().addAll(action, close);
		
		AdvancedTextField file = new AdvancedTextField(250, true, false);
		file.setState(State.ERROR, Messages.get("BACKUP_FILE_ERROR_1"));
		Button browse = new Button(Messages.get("BACKUP_BROWSE"));
		browse.setPrefWidth(btnSize);
		browse.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(Messages.get("BACKUP_CHOOSE"));
			File tmp;
			if (root.getChildren().isEmpty())
				tmp = fileChooser.showOpenDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			else
				tmp = fileChooser.showSaveDialog((Stage) dialog.getDialogPane().getScene().getWindow());
			if (tmp != null) {
				file.getTextField().setText(tmp.getAbsolutePath());
			}
		});
		
		AdvancedTextField password = new AdvancedTextField(250, btnSize, true, true, false);
		password.setState(State.ERROR, Messages.get("BACKUP_PASSWORD_ERROR_1"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);
		gp.add(new Label(Messages.get("BACKUP_CHOOSER")), 0, 0);
		gp.add(new HBox(2, file, browse), 1, 0);
		//gp.add(browse, 2, 0);
		gp.add(new Label(Messages.get("BACKUP_PASSWORD")), 0, 1);
		gp.add(password, 1, 1);
		
		
		Node actionButton = dialog.getDialogPane().lookupButton(action);
		actionButton.setDisable(true);

		file.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.trim().isEmpty())
				file.setState(State.ERROR, Messages.get("BACKUP_FILE_ERROR_1"));
			else if (root.getChildren().isEmpty() && !Paths.get(newValue).toFile().exists())
				file.setState(State.ERROR, Messages.get("BACKUP_FILE_ERROR_2"));
			else
				file.setState(State.ACCEPT, null);
			
			actionButton.setDisable(!password.accept() || !file.accept());
		});
		password.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.trim().isEmpty())
				password.setState(State.ERROR, Messages.get("BACKUP_PASSWORD_ERROR_1"));
			else
				password.setState(State.ACCEPT, null);
			actionButton.setDisable(!password.accept() || !file.accept());
		});

		dialog.getDialogPane().setContent(new VBox(4, gp));
		Platform.runLater(() -> file.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == action) {
				return new String[] {file.getTextField().getText(), password.getTextField().getText(), dialogButton.getText()};
			}
			return null;
		});
		
		try {
			return dialog.showAndWait().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	public static void preferencesDialog() {
		Dialog<String> dialog = new Dialog<>();
		updateTheme(dialog.getDialogPane().getScene());
		dialog.initOwner(scene.getWindow());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(0, Images.PREFERENCES);
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
		        	try {
		        		Integer value = converter.fromString(text);
		        		valueFactory.setValue(value);
		        	}
		        	catch (NumberFormatException nfe) {
		        		timer.getEditor().setText("" + valueFactory.getValue());
		        	}
		            
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
		stage.getIcons().add(0, Images.PROPERTIES);
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
		
		for (Group group : appli.getGroups()) {
			short groupSize = 0;
			for (Entry e : appli.getEntries(group, true)) {
				groupSize += e.getIdentifier().getValue().length();
				groupSize += e.getUserName().getValue().length();
				groupSize += e.getPassword().getValue().length();
				groupSize += e.getUrl().getValue().length();
				groupSize += e.getNotes().getValue().length();
				groupSize += 20;//~Dates
			}
			footprints.put(group.name, Short.valueOf(groupSize));
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
		stage.getIcons().add(0, Images.ABOUT);
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
		
		gp.add(new Label(Messages.get("ABOUT_LICENSE")), 0, 2);
		gp.add(l = new Label("GPL-3.0"), 1, 2);
		
		String bugLink = "https://github.com/securingdata";
		gp.add(new Label(Messages.get("ABOUT_BUG")), 0, 3);
		gp.add(l = new Label(bugLink), 1, 3);
		l.setTextFill(Color.BLUE);
		l.setOnMousePressed(event -> {
			try {
				java.awt.Desktop.getDesktop().browse(URI.create(bugLink));
			}
			catch (IOException e1) {}
		});
		
		String creditsLink = "www.famfamfam.com/lab/icons/silk/";
		gp.add(new Label(Messages.get("ABOUT_CREDITS")), 0, 4);
		gp.add(l = new Label(creditsLink), 1, 4);
		l.setTextFill(Color.BLUE);
		l.setOnMousePressed(event -> {
			try {
				java.awt.Desktop.getDesktop().browse(URI.create(creditsLink));
			}
			catch (IOException e1) {}
		});
		
		ImageView iv = new ImageView(Images.SMARTSAFE);
		iv.setPreserveRatio(true);
		iv.setFitWidth(260);
		
		BorderPane bp = new BorderPane(gp);
		bp.setTop(iv);
		
		dialog.getDialogPane().setContent(bp);
		dialog.showAndWait();
	}
}
