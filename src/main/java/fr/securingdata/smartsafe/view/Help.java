package fr.securingdata.smartsafe.view;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import fr.securingdata.smartsafe.Messages;
import fr.securingdata.smartsafe.Prefs;
import fr.securingdata.smartsafe.util.ResourcesManager;
import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Help {
	private static Path htmlDir;
	
	public static void clearCache() {
		htmlDir = null;
	}
	
	public static void helpDialog() {
		if (htmlDir == null)
			htmlDir = ResourcesManager.initHtmlDirectory(!Prefs.get(Prefs.KEY_THEME).equals(Prefs.DEFAULT_THEME));
		
		Dialog<String> dialog = new Dialog<>();
		dialog.initOwner(GlobalView.getScene().getWindow());
		GlobalView.updateTheme(dialog.getDialogPane().getScene());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(0, Images.HELP);
		
		dialog.setTitle(Messages.get("HELP_DIALOG"));
		dialog.setHeaderText(null);
		dialog.initModality(Modality.NONE);
		dialog.setResizable(true);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok);
		
		TreeItem<String> root, tmp;
		TreeView<String> rootView = new TreeView<>(root = new TreeItem<>(Messages.get("HELP_ITEM_HOME")));
		root.setExpanded(true);
		
		root.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_FIRST_INIT")));
		root.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_CONNECTION")));
		root.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_BACKUP")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_GROUPS_ENTRIES")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_CREATING_GROUP")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_CREATING_ENTRY")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_ACTION_ENTRY")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_SEARCH")));
		tmp.setExpanded(true);
		root.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_PREFERENCES")));
		
		WebView browser = new WebView();
		browser.setPrefSize(700, 400);
		WebEngine webEngine = browser.getEngine();
		
		webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == State.SUCCEEDED) {
				EventListener listener = new EventListener() {
	                public void handleEvent(Event ev) {
	                	String link = ((Element)ev.getTarget()).getAttribute("href");
	                	TreeItem<String> ti = findItem(root, link);
	                	if (ti != null)
	                		Platform.runLater(() -> rootView.getSelectionModel().select(ti));
	                }
	            };
				
				Document doc = browser.getEngine().getDocument();
                NodeList nodeList = doc.getElementsByTagName("a");
                for (int i = 0; i < nodeList.getLength(); i++) {
                	((EventTarget) nodeList.item(i)).addEventListener("click", listener, false);
                }
			}
		});
		
		rootView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null)
				webEngine.loadContent("");
			else
				webEngine.load(ResourcesManager.loadHtmlPage(htmlDir, newValue.getValue().toLowerCase().replace(' ', '_')));
		});
		rootView.getSelectionModel().select(root);
		
		
		HBox hb = new HBox(rootView, browser);
		hb.setSpacing(5);
		dialog.getDialogPane().setContent(hb);
		dialog.showAndWait();
	}
	
	private static final TreeItem<String> findItem(TreeItem<String> root, String value) {
		for (TreeItem<String> ti : root.getChildren()) {
    		if (ti.getValue().equals(value))
    			return ti;
    		if (!ti.isLeaf() && (ti = findItem(ti, value)) != null)
				return ti;
		}
		return null;
	}
}
