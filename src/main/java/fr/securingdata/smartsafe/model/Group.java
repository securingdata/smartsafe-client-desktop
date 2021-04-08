package fr.securingdata.smartsafe.model;

import java.util.LinkedList;
import java.util.List;

public class Group {
	public String name;
	public final List<Entry> entries;
	
	public Group(String name) {
		this.name = name;
		entries = new LinkedList<>();
	}
}
