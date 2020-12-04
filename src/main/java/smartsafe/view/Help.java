package smartsafe.view;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import smartsafe.Messages;

public class Help {
	public static void helpDialog() {
		Dialog<String> dialog = new Dialog<>();
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Images.HELP);
		
		//dialog.setTitle(Messages.get("ABOUT_DIALOG"));
		dialog.setHeaderText(null);
		dialog.initModality(Modality.NONE);
		
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(ok);
		
		TreeItem<String> root, tmp;
		TreeView<String> rootView = new TreeView<>(root = new TreeItem<>(Messages.get("HELP_ITEM_HOME")));
		root.setExpanded(true);
		
		root.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_FIRST_INIT")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_MANAGING_SERVER")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_COMPILING")));
		tmp.getChildren().add(new TreeItem<>(Messages.get("HELP_ITEM_LOADING")));
		
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_CONNECTION")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_BACKUP")));
		root.getChildren().add(tmp = new TreeItem<>(Messages.get("HELP_ITEM_GROUPS_ENTRIES")));
		tmp.getChildren().add(new TreeItem<>("Creating a Group"));
		tmp.getChildren().add(new TreeItem<>("Creating an Entry"));
		tmp.getChildren().add(new TreeItem<>("Editing an Entry"));
		tmp.getChildren().add(new TreeItem<>("Actions on Entries"));
		tmp.getChildren().add(new TreeItem<>("Search engine"));
		
		WebView browser = new WebView();
		browser.setPrefSize(350, 400);
		WebEngine webEngine = browser.getEngine();
		
		rootView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null)
				webEngine.loadContent("");
			else if (newValue.getValue().equals(Messages.get("HELP_ITEM_HOME")))
				webEngine.loadContent(getDefaultPage());
			else if (newValue.getValue().equals(Messages.get("HELP_ITEM_FIRST_INIT")))
				webEngine.loadContent(getFirstinitPage());
		});
		rootView.getSelectionModel().select(root);
		
		
		HBox hb = new HBox(rootView, browser);
		hb.setSpacing(5);
		dialog.getDialogPane().setContent(hb);
		dialog.showAndWait();
	}
	
	private static String getDefaultPage() {
		String page = "<html style=\"font-family: Arial;\">";
		page += "<h1 style=\"color: #6495ED;\">Welcome !</h1>";
		page += "<p>Navigate using the left part of the window.</p>";
		page += "</html>";
		return page;
	}
	private static String getFirstinitPage() {
		String page = "<html style=\\\"font-family: Arial;\\\">";
		page += "<h1 style=\"color: #6495ED;\">First initialization</h1>";
		page += "<p style=\"text-align: justify;\">When using the token for the first time or after a token update, a first personnalization must be done. This is achieved by selecting the entry 'File' &rarr; 'First initialization'.</p>";
		page += "</html>";
		return page;
	}
}
