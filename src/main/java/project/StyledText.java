package project;

public class StyledText {
	public final String text;
	public final String style;
	public final int nbLines;
	
	public StyledText(String text, String style) {
		this.text = text;
		this.style = style;
		this.nbLines = (int) text.chars().filter(ch -> ch == '\n').count();
	}
	
	public int gotoLine(int lineNumber) {
		int index = 0;
		do {
			if (lineNumber == 0)
				return index;
			lineNumber--;
			index = text.indexOf('\n', index) + 1;
		} while (index != 0);
		return text.length();
	}
}
