package compiler.project;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import util.StringHex;

public class Package {
	private StringHex aid;
	private String name;
	private Path path;
	private List<String> applets;
	private List<StringHex> appletsAID;
	private List<String> imports;
	
	Package(String name, Path path) {
		this.name = name;
		this.path = path;
		applets = new LinkedList<>();
		imports = new LinkedList<>();
	}
	
	public void setAid(StringHex aid) {
		this.aid = aid;
	}
	public StringHex getRawAid() {
		return aid;
	}
	public String getFormattedAid() {
		return "0x" + aid.toString().replaceAll(" ", ":0x");
	}
	
	public String getName() {
		return name;
	}
	public Path getPath() {
		return path;
	}
	public List<String> getApplets() {
		return applets;
	}
	public void setAppletsAID(List<StringHex> appletsAID) {
		this.appletsAID = appletsAID;
	}
	public List<StringHex> getAppletsAID() {
		return appletsAID;
	}
	public List<String> getImports() {
		return imports;
	}
}
