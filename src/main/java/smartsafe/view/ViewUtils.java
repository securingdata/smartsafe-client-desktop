package smartsafe.view;

import java.io.File;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ViewUtils {
	public static final BooleanProperty cardConnected = new SimpleBooleanProperty();
	public static final BooleanProperty groupSelected = new SimpleBooleanProperty();
	public static final BooleanProperty entrySelected = new SimpleBooleanProperty();
	
	public static void addDisableListener(Node n, BooleanProperty prop) {
		n.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> n.setDisable(!newV.booleanValue()));
	}
	public static void addDisableListener(MenuItem mi, BooleanProperty prop) {
		mi.setDisable(!prop.get());
		prop.addListener((ov, oldV, newV) -> mi.setDisable(!newV.booleanValue()));
	}
	public static void addEnableListener(MenuItem mi, BooleanProperty prop) {
		mi.setDisable(prop.get());
		prop.addListener((ov, oldV, newV) -> mi.setDisable(newV.booleanValue()));
	}
	
	public static void keyValidator(IntegerProperty validator, StackPane keySp, ComboBox<String> keyComboBox, String newValue, int keyPosition) {
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
			validator.set(validator.get() | keyPosition);
		else
			validator.set(validator.get() & ~keyPosition);
	}
	public static void bckpFileValidator(IntegerProperty validator, StackPane bckpSp, String newValue, boolean active) {
		ImageView iv = (ImageView) bckpSp.getChildren().get(1);
		boolean tmp = active && (newValue.isEmpty() || !new File(newValue).exists());
		iv.setVisible(tmp);
		if (tmp)
			validator.set(validator.get() | 0x8);
		else
			validator.set(validator.get() & ~0x8);
	}
	
	
	public static void bindTextAndPassField(TextField tf, PasswordField pf) {
		tf.textProperty().addListener((observable, oldValue, newValue) -> pf.setText(newValue));
		pf.textProperty().addListener((observable, oldValue, newValue) -> tf.setText(newValue));
	}
	public static ImageView createWarning() {
		ImageView iv = new ImageView(Images.WARNING);
		StackPane.setAlignment(iv, Pos.CENTER_RIGHT);
		return iv;
	}
}
