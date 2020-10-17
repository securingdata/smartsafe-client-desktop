package smartsafe.view;

import javafx.scene.control.ProgressBar;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ProgressBarWithText extends StackPane {
	private Text text;
	private ProgressBar bar;
	
	public ProgressBarWithText() {
		text = new Text();
		text.setFont(Font.font("System Regular", FontWeight.BOLD, 10));
		
		bar = new ProgressBar(0);
		bar.setMaxWidth(Double.MAX_VALUE);
		bar.setMinHeight(text.getBoundsInLocal().getHeight() + 8);

		getChildren().setAll(bar, text);
		reset();
	}
	
	public void setText(String text) {
		this.text.setText(text);
	}
	public void setProgress(double value) {
		bar.setProgress(value);
	}
	public void setProgress(double value, String text) {
		setText(text);
		setProgress(value);
	}
	public void reset() {
		setProgress(0, "");
		setTextStyle(false);
	}
	public void setTextStyle(boolean error) {
		if (error)
			text.setFill(Color.DARKRED);
		else
			text.setFill(Color.DARKBLUE);
	}
}
