package fr.securingdata.smartsafe.view;

import fr.securingdata.smartsafe.Messages;
import javafx.geometry.Pos;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class AdvancedTextField extends HBox {
	public enum State {
		ACCEPT, WARNING, ERROR;
	}
	
	private TextField textField, passField;
	private StackPane imagePane;
	private ImageView accept, warning, error;
	private State state;
	
	public AdvancedTextField() {
		this(true, false);
	}
	
	public AdvancedTextField(boolean hasState, boolean hasShowButton) {
		this(0, hasState, hasShowButton);
	}
	
	public AdvancedTextField(boolean hasState, boolean hasShowButton, boolean initialText) {
		this(0, 0, hasState, hasShowButton, initialText);
	}
	
	public AdvancedTextField(double fieldWith, boolean hasState, boolean hasShowButton) {
		this(fieldWith, 0, hasState, hasShowButton, true);
	}
	
	public AdvancedTextField(double fieldWith, double showBtnWidth, boolean hasState, boolean hasShowButton, boolean initialText) {
		super(2);
		
		textField = new TextField();
		passField = new PasswordField();
		textField.textProperty().addListener((obs, oldVal, newVal) -> {
			if (!passField.getText().equals(newVal))
				passField.setText(newVal);
		});
		passField.textProperty().addListener((obs, oldVal, newVal) -> {
			if (!textField.getText().equals(newVal))
				textField.setText(newVal);
		});
		if (fieldWith != 0) {
			textField.setPrefWidth(fieldWith);
			passField.setPrefWidth(fieldWith);
		}
		getChildren().add(initialText ? textField : passField);
		
		if (hasState) {
			accept = new ImageView(Images.ACCEPT);
			StackPane.setAlignment(accept, Pos.CENTER);
			warning = new ImageView(Images.WARNING);
			StackPane.setAlignment(warning, Pos.CENTER);
			error = new ImageView(Images.ERROR);
			StackPane.setAlignment(error, Pos.CENTER_RIGHT);
			this.state = State.ERROR;
			imagePane = new StackPane(error);
			getChildren().add(imagePane);
		}
		if (hasShowButton) {
			ToggleButton show;
			getChildren().add(show = new ToggleButton(Messages.get("ENTRY_SHOW")));
			show.setMaxWidth(Double.MAX_VALUE);
			if (showBtnWidth != 0)
				show.setPrefWidth(showBtnWidth);
			show.setOnAction(event -> {
				getChildren().set(0, show.isSelected() ? textField : passField);
			});
		}
	}
	
	public TextField getTextField() {
		return textField;
	}
	public void setTooltip(String tooltip) {
		if (tooltip != null && !tooltip.isEmpty()) {
			textField.setTooltip(new Tooltip(tooltip));
			if (passField != null)
				passField.setTooltip(new Tooltip(tooltip));
		}
		else {
			textField.setTooltip(null);
			if (passField != null)
				passField.setTooltip(null);
		}
	}
	
	public State getState() {
		return state;
	}
	public boolean accept() {
		return state == State.ACCEPT;
	}
	public boolean error() {
		return state == State.ERROR;
	}
	public void setState(State state) {
		setState(state, null);
	}
	public void setState(State state, String tooltip) {
		setTooltip(tooltip);
		
		//state is null when haState is false in constructor
		if (state == null)
			return;
		this.state = state;
		switch (state) {
			case ACCEPT:
				imagePane.getChildren().set(0, accept);
				return;
			case WARNING:
				imagePane.getChildren().set(0, warning);
				return;
			case ERROR:
				imagePane.getChildren().set(0, error);
				return;
			default:
				//Should not happen
				return;
		}
	}
}
