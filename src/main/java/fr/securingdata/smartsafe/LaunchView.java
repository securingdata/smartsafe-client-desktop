package fr.securingdata.smartsafe;

import java.io.PrintWriter;
import java.io.StringWriter;

import fr.securingdata.smartsafe.controller.Controls;
import fr.securingdata.smartsafe.view.GlobalView;
import fr.securingdata.smartsafe.view.Images;
import javafx.application.Application;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LaunchView extends Application {
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			primaryStage.setTitle("SmartSafe");
			primaryStage.getIcons().add(Images.FAVICON);
			primaryStage.setScene(GlobalView.createView());
			primaryStage.show();
			primaryStage.setOnCloseRequest(event -> Controls.cleanOnExiting());
		} catch (Throwable th) {
			th.printStackTrace();
			createExceptionDialog(th);
		}
	}
	protected Dialog<ButtonType> createExceptionDialog(Throwable th) {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle("Program exception");
		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.setContentText("Details of the problem:");
		dialogPane.getButtonTypes().addAll(ButtonType.OK);
		dialogPane.setContentText(th.getMessage());
		dialog.initModality(Modality.APPLICATION_MODAL);
		Label label = new Label("Exception stacktrace:");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		th.printStackTrace(pw);
		pw.close();
		TextArea textArea = new TextArea(sw.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		GridPane root = new GridPane();
		root.setVisible(false);
		root.setMaxWidth(Double.MAX_VALUE);
		root.add(label, 0, 0);
		root.add(textArea, 0, 1);
		dialogPane.setExpandableContent(root);
		dialog.showAndWait()
		.filter(response -> response == ButtonType.OK)
		.ifPresent(response -> System.exit(-1));
		return dialog;
	}
}
