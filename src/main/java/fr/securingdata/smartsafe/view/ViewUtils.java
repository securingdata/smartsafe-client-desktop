package fr.securingdata.smartsafe.view;

import java.util.List;

import javax.smartcardio.CardTerminal;

import fr.securingdata.connection.Connection;
import fr.securingdata.smartsafe.Messages;
import fr.securingdata.smartsafe.Prefs;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;
import javafx.stage.Modality;

public class ViewUtils {
	public static final BooleanProperty cardConnected = new SimpleBooleanProperty();
	public static final BooleanProperty groupSelected = new SimpleBooleanProperty();
	public static final BooleanProperty groupCanUp    = new SimpleBooleanProperty();
	public static final BooleanProperty groupCanDown  = new SimpleBooleanProperty();
	public static final BooleanProperty entrySelected = new SimpleBooleanProperty();
	public static final BooleanProperty entryCanUp    = new SimpleBooleanProperty();
	public static final BooleanProperty entryCanDown  = new SimpleBooleanProperty();
	
	public static void addDisableListener(Node n, BooleanProperty prop) {
		n.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> n.setDisable(!newV.booleanValue()));
	}
	public static void addDisableListener(MenuItem mi, BooleanProperty prop) {
		mi.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> mi.setDisable(!newV.booleanValue()));
	}
	
	public static ComboBox<CardTerminal> getTerminals() {
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
}
