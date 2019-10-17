package smartsafe.view;

import java.util.List;

public class Action {
	public interface Executable {
		void run(List<Object> params);
	}
	
	public String name;
	public final boolean undoable;
	private List<Object> params;
	private Executable exec;
	
	public Action(String name, boolean undoable, List<Object> params, Executable exec) {
		this.name = name;
		this.undoable = undoable;
		this.params = params;
		this.exec = exec;
	}
	
	public void run() {
		exec.run(params);
	}
	
	
}
