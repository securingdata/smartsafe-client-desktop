package util;

public class ExitException extends SecurityException {

	private static final long serialVersionUID = 2249128764633980978L;
	
	public ExitException(String exitCode) {
		super("Exit code: " + exitCode);
	}
}
