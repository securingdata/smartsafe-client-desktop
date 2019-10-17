package smartsafe.view;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
		/*mainPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
			reference.setPrefWidth(new Double(110));
		});*/
		
		ToolBar tb = new ToolBar();
		tb.getItems().add(Controls.getButton(Controls.CONNECT));
		tb.getItems().add(Controls.getButton(Controls.NEW_GROUP));
		tb.getItems().add(Controls.getButton(Controls.NEW_ENTRY));
		tb.getItems().add(Controls.getButton(Controls.DELETE));
		
		BorderPane rootPane = new BorderPane(mainPane);
		rootPane.setTop(tb);
		
		MenuBar mb = new MenuBar();
		Menu m;
		mb.getMenus().add(m = new Menu("File"));
		m.getItems().add(Controls.getMenuItem(Controls.CONNECT));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(Controls.getMenuItem(Controls.NEW_GROUP));
		m.getItems().add(Controls.getMenuItem(Controls.NEW_ENTRY));
		m.getItems().add(Controls.getMenuItem(Controls.DELETE));
		m.getItems().add(new SeparatorMenuItem());
		m.getItems().add(Controls.getMenuItem(Controls.EXIT));
		mb.getMenus().add(m = new Menu("Edit"));
		m.getItems().add(Controls.getMenuItem(Controls.EDIT));
		m.getItems().add(Controls.getMenuItem(Controls.GOTO));
		m.getItems().add(Controls.getMenuItem(Controls.COPY_USER));
		m.getItems().add(Controls.getMenuItem(Controls.COPY_PASS));
		m.getItems().add(Controls.getMenuItem(Controls.SHOW_PASS));
		mb.getMenus().add(m = new Menu("Help"));
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
			
			table.getColumns().add(column = new TableColumn<>("Identifier"));
			column.setCellValueFactory(cellData -> cellData.getValue().getIdentifier());
			
			table.getColumns().add(column = new TableColumn<>("User name"));
			column.setCellValueFactory(cellData -> cellData.getValue().getUserName());
			
			table.getColumns().add(column = new TableColumn<>("Password"));
			column.setCellValueFactory(cellData -> cellData.getValue().getPassword());
			
			table.getColumns().add(column = new TableColumn<>("URL"));
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
						expiresOn.setText("Never");
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
			gp.add(l = new Label("Last update:"), 0, 0);
			gp.add(lastUpdate = new Label("-"), 1, 0);
			gp.add(new Label("Expires on:"), 0, 1);
			gp.add(expiresOn = new Label("-"), 1, 1);
			gp.add(new Label("Notes:"), 0, 2);
			gp.add(notes = new TextArea(), 1, 2);
			l.setMinWidth(80);
			notes.setEditable(false);
			
			details = new TitledPane("Details", gp);
			details.setGraphic(new ImageView(Images.DETAILS));
			details.setCollapsible(true);
		}
		return details;
	}
	private static String formatDate(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	}
	private static Dialog<ButtonType> errorDialog(String error) {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle("OOps!! An error has occured!");
		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.setContentText("Details of the problem:");
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
		ButtonType ok = initDialog(dialog, Images.CONNECT, "Connect to card");
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		ComboBox<CardTerminal> readerList = new ComboBox<>();
		readerList.getItems().addAll(Connection.getTerminals());
		readerList.getSelectionModel().select(0);
		PasswordField password = new PasswordField();
		
		gp.add(new Label("Select reader:"), 0, 0);
		gp.add(readerList, 1, 0);
		gp.add(new Label("Password:"), 0, 1);
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
		ButtonType ok = initDialog(dialog, Images.NEW_GROUP, "New group");
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		TextField groupName = new TextField();
		Spinner<Integer> groupSize = new Spinner<>(8, 255, 32);
		
		gp.add(new Label("Group name:"), 0, 0);
		gp.add(groupName, 1, 0);
		gp.add(new Label("Group size:"), 0, 1);
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
					errorDialog("No more group can be created (smart card is full) !");
				}
				else {
					errorDialog("The group has not been created with an undefined error: " + new StringHex(sw).toString());
				}
		    }
		    return null;
		});
		
		dialog.showAndWait();
	}
	public static void newEntryDialog() {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.NEW_ENTRY, "New entry");
		
		
		HBox exp = new HBox();
		DatePicker expires = new DatePicker();
		expires.setMaxWidth(160);
		exp.getChildren().add(expires);
		Button b;
		exp.getChildren().add(new Label(" "));
		exp.getChildren().add(b = new Button("T+1 month"));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(1)));
		exp.getChildren().add(new Label(" "));
		exp.getChildren().add(b = new Button("T+3 months"));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(3)));
		exp.getChildren().add(new Label(" "));
		exp.getChildren().add(b = new Button("T+6 months"));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(6)));
		exp.getChildren().add(new Label(" "));
		exp.getChildren().add(b = new Button("T+1 year"));
		b.setOnAction(event -> expires.setValue(LocalDate.now().plusMonths(12)));
		exp.setDisable(true);
		
		HBox pass = new HBox();
		ToggleButton tb;
		PasswordField password = new PasswordField();
		TextField password2 = new TextField();
		password.setMinWidth(363);
		password2.setMinWidth(363);
		pass.getChildren().add(password);
		pass.getChildren().add(new Label(" "));
		pass.getChildren().add(tb = new ToggleButton("Show"));
		tb.setOnAction(event -> {
			pass.getChildren().set(0, tb.isSelected() ? password2 : password);
		});
		pass.getChildren().add(new Label(" "));
		pass.getChildren().add(new Button("Random"));
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		TextField identifier = new TextField();
		TextField userName = new TextField();
		TextField url = new TextField();
		TextArea notes = new TextArea();
		
		gp.add(new Label("* Identifier:"), 0, 0);
		gp.add(identifier, 1, 0);
		gp.add(new Label("* User name:"), 0, 1);
		gp.add(userName, 1, 1);
		gp.add(new Label("Password:"), 0, 2);
		gp.add(pass, 1, 2);
		gp.add(new Label("Expires:"), 0, 3);
		gp.add(exp, 1, 3);
		gp.add(new Label("URL:"), 0, 4);
		gp.add(url, 1, 4);
		gp.add(new Label("Notes:"), 0, 5);
		gp.add(notes, 1, 5);
		
		
		Node okButton = dialog.getDialogPane().lookupButton(ok);
		okButton.setDisable(true);

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
		    	Entry entry = new Entry(identifier.getText(), userName.getText());
		    	short sw = Controls.getAppli().addEntry(Entry.NB_PROPERTIES, entry).getStatusWord();
		    	if (sw == SmartSafeAppli.SW_NO_ERROR) {
		    		if (!password.getText().isEmpty()) {
			    		Controls.getAppli().setData(Entry.INDEX_PASSWORD, password.getText());
			    		Controls.getAppli().setData(Entry.INDEX_lAST_UPDATE, LocalDate.now().toString());
			    		if (expires.getValue() != null) {
				    		Controls.getAppli().setData(Entry.INDEX_EXP_DATE, expires.getValue().toString());
				    	}
			    	}
			    	if (!url.getText().isEmpty()) {
			    		Controls.getAppli().setData(Entry.INDEX_URL, url.getText());
			    	}
			    	if (!notes.getText().isEmpty()) {
			    		Controls.getAppli().setData(Entry.INDEX_NOTES, notes.getText());
			    	}
			    	entry.maskPassword();
			    	table.getItems().add(entry);
				}
				else if (sw == SmartSafeAppli.SW_FILE_FULL) {
					errorDialog("No more entry can be created (current group is full) !");
				}
				else {
					errorDialog("The entry has not been created with an undefined error: " + new StringHex(sw).toString());
				}
		    }
		    return null;
		});
		
		dialog.showAndWait();
	}
	public static void deleteDialog(TreeItem<String> group, Entry entry) {
		Dialog<String> dialog = new Dialog<>();
		ButtonType ok = initDialog(dialog, Images.DELETE, "Delete ?");
		
		GridPane gp = new GridPane();
		gp.setHgap(2);
		gp.setVgap(2);

		ToggleGroup tGroup = new ToggleGroup();
		RadioButton rGroup = new RadioButton("Delete group: " + group.getValue());
		RadioButton rEntry = new RadioButton(entry == null ? "No entry selected!" : "Delete entry: " + entry.getFullIdentifier().replace("\n", "//"));
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
		dialog.setTitle("About");
		dialog.setHeaderText(null);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok);
		
		GridPane gp = new GridPane();
		Label l;
		gp.setHgap(5);
		gp.setVgap(5);
		gp.add(new Label("Server version:"), 0, 0);
		if (Controls.getAppli() != null)
			gp.add(new Label(Controls.getAppli().getVersion()), 1, 0);
		else
			gp.add(new Label("Smart card not connected !"), 1, 0);

		gp.add(new Label("Client version:"), 0, 1);
		gp.add(new Label(Version.version), 1, 1);
		gp.add(new Label("Report a bug ?"), 0, 2);
		gp.add(l = new Label("contact-smartsafe@gmail.com"), 1, 2);
		l.setTextFill(Color.BLUE);
		dialog.getDialogPane().setContent(gp);
		
		dialog.showAndWait();
	}
}
