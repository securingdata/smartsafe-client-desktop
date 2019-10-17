package project;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JCCodeParser {
	private static final String[] KEYWORDS = new String[] {
            "abstract", "boolean", "break", "byte", "case", 
            "catch", "char", "class", "continue", "default", 
            "do", "else", "extends", "final", "finally",
            "for", "if", "implements", "import", "true",
            "instanceof", "int", "interface", "new",
            "package", "private", "protected", "public",
            "return", "short", "static", "super", "switch", 
            "this", "throw", "throws", "try", "void", "while"
    };

    private static final String KEYWORD_PATTERN   = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String STRING_PATTERN    = "\"([^\n\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN   = "//[^\n]*" + "|" + "/\\*.*?(\\*/|\\z)";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    , Pattern.DOTALL);
    
    public static void computeHighlight(String text, List<StyledText> styledText) {
    	Matcher matcher = PATTERN.matcher(text);
		int last = 0;
		styledText.clear();
		while(matcher.find()) {
			String styleClass;
			styleClass =
					matcher.group("KEYWORD")   != null ? "keyword" :
                    matcher.group("STRING")    != null ? "string" :
                    matcher.group("COMMENT")   != null ? "comment" :
					null; /* never happens */ assert styleClass != null;

			addStyledText(styledText, text.substring(last, matcher.start()), "");
			addStyledText(styledText, text.substring(matcher.start(), matcher.end()), styleClass);
			last = matcher.end();
		}
		addStyledText(styledText, text.substring(last), "");
	}
	private static void addStyledText(List<StyledText> styledText, String string, String style) {
		if (string.isEmpty())
			return;
		styledText.add(new StyledText(string, style));
	}
}
