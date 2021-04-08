package fr.securingdata.smartsafe.view;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.Node;

public class ProgressDialog extends Dialog<Object> {
	private ProgressBarWithText pb;
	
	public ProgressDialog(String title, Image icon) {
		super();
		GlobalView.updateTheme(getDialogPane().getScene());
		initOwner(GlobalView.getScene().getWindow());
		((Stage) getDialogPane().getScene().getWindow()).getIcons().add(0, icon);
		setTitle(title);
		setHeaderText(null);
		pb = new ProgressBarWithText();
		pb.setMinWidth(300);
		getDialogPane().setContent(pb);
		
		//Remove ButtonBar display
		for (Node n : getDialogPane().getChildren()) {
			if (n instanceof ButtonBar) {
				getDialogPane().getChildren().remove(n);
				return;
			}
		}
	}
	
	public void showDialog() {
		Platform.runLater(() -> show());
	}
	public void closeNow() {
		allowClose();
		close();
	}
	public void closeDialog() {
		Platform.runLater(() -> {
			allowClose();
			close();
		});
	}
	public void allowClose() {
		getDialogPane().getButtonTypes().add(ButtonType.CANCEL);//Hack to allow closing
	}
	
	public void setProgress(double value) {
		Platform.runLater(() -> pb.setProgress(value));
	}
	public void setText(String text) {
		Platform.runLater(() -> pb.setText(text));
	}
	public void addProgress(double value) {
		Platform.runLater(() -> pb.addProgress(value));
	}
	public void setProgress(double value, String text) {
		Platform.runLater(() -> pb.setProgress(value, text));
	}
	public void setTextStyle(boolean error) {
		Platform.runLater(() -> pb.setTextStyle(error));
		allowClose();
	}
}
