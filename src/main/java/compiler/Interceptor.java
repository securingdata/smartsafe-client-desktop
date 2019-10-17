package compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class Interceptor extends OutputStream {
	private PrintStream backup;
	private String record;
	
	public Interceptor() {
		super();
	}
	
	public void attach() {
		backup = System.err;
		record = "";
		System.setErr(new PrintStream(this));
	}
	public void detach() {
		System.setErr(backup);
	}
	
	public String getRecord() {
		return record;
	}

	@Override
	public void write(int b) throws IOException {
		record += (char) b;
	}
}
