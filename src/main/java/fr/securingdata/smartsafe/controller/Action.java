package fr.securingdata.smartsafe.controller;

public class Action {
	public interface Executable {
		void run(Object params);
	}
	
	public String name;
	public final boolean undoable;
	private Object params;
	private Executable exec;
	
	public Action(String name, boolean undoable, Object params, Executable exec) {
		this.name = name;
		this.undoable = undoable;
		this.params = params;
		this.exec = exec;
	}
	
	public void setParams(Object params) {
		this.params = params;
	}
	
	public void run() {
		exec.run(params);
	}
	
	
}
