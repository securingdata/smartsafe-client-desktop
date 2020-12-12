package smartsafe.view;

import java.nio.file.Path;

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
import smartsafe.Messages;
import util.ResourcesManager;

public class Help {
	private static Path htmlDir;
	
	public static void helpDialog() {
		if (htmlDir == null)
			htmlDir = ResourcesManager.initHtmlDirectory();
		
		Dialog<String> dialog = new Dialog<>();
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.HELP);
		
		dialog.setTitle(Messages.get("HELP_DIALOG"));
		dialog.setHeaderText(null);
		dialog.initModality(Modality.NONE);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok);
		
		TreeItem<String> root, tmp;
		TreeView<String> rootView = new TreeView<>(root = new TreeItem<>(Messages.get("HELP_ITEM_HOME")));
		root.setExpanded(true);
		
		root.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_FIRST_INIT")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_CONNECTION")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_BACKUP")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_GROUPS_ENTRIES")));
		tmp.getChildren().add(new TreeItem<>("Creating a Group"));
		tmp.getChildren().add(new TreeItem<>("Creating an Entry"));
		tmp.getChildren().add(new TreeItem<>("Editing an Entry"));
		tmp.getChildren().add(new TreeItem<>("Actions on Entries"));
		tmp.getChildren().add(new TreeItem<>("Search engine"));
		
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_MANAGING_SERVER")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_COMPILING")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_LOADING")));
		
		WebView browser = new WebView();
		browser.setPrefSize(700, 400);
		WebEngine webEngine = browser.getEngine();
		
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
}
