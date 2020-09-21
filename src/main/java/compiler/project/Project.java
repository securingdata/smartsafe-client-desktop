package compiler.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import compiler.Compiler;
import compiler.CompilerException;
import util.StringHex;

public class Project {
	
	private String name;
	private URI path;
	
	private transient List<Package> packages;
	
	public Project(String name, URI path) {
		this.name = name;
		this.path = path;
	}
	public Project(String name, String path) {
		this.name = name;
		this.path = new File(path).toURI();
	}
	
	public String getName() {
		return name;
	}
	public URI getPath() {
		return path;
	}
	
	public List<Package> getPackages() {
		return packages;
	}
	public void parsePckgs() {
		packages = new LinkedList<>();
		
		Path project = Paths.get(Paths.get(path).toString() + "/src/");
		try (Stream<Path> paths = Files.walk(project)) {
			paths.forEach(filePath -> {
				if (filePath.toString().endsWith(".java")) {
					try (BufferedReader br = Files.newBufferedReader(filePath)) {
						String line, txt = "";
						while ((line = br.readLine()) != null)
							txt += line;
						List<StyledText> parsedText = new ArrayList<>();
						JCCodeParser.computeHighlight(txt, parsedText);
						Package p = null;
						for (int i = 0; i < parsedText.size(); i++) {
							StyledText st = parsedText.get(i);
							if (st.text.equals("package") && st.style.equals("keyword")) {
								String s = parsedText.get(++i).text.replaceAll("\\s*", "");
								s = s.substring(s.lastIndexOf(' ') + 1, s.indexOf(';'));
								p = addPckg(s, filePath.getParent());
							}
							if (st.text.equals("import") && st.style.equals("keyword")) {
								String s = parsedText.get(++i).text.replaceAll("\\s*", "");
								s = s.substring(s.lastIndexOf(' ') + 1, s.lastIndexOf("."));
								if (!p.getImports().contains(s))
									p.getImports().add(s);
							}
							if (st.text.equals("extends") && st.style.equals("keyword")) {
								String s = parsedText.get(++i).text;
								if (s.contains("javacard.framework.Applet") || (s.matches(".*\\bApplet\\b.*") && p.getImports().contains("javacard.framework"))) {
									p.getApplets().add(parsedText.get(i - 2).text.replaceAll("\\s*", ""));
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<Package> oldList = packages;
		packages = new LinkedList<>();
		for (Package p : oldList)
			addOrderedPckg(p, oldList);
	}
	public void build() throws CompilerException {
		Path target = Paths.get(Paths.get(path).toString(), "/build/");
		if (!Files.exists(target))
			try {
				Files.createDirectory(target);
			} catch (IOException e) {}
		String[] extraCP = new String[packages.size()];
		for (int i = 0; i < extraCP.length; i++)
			extraCP[i] = packages.get(i).getPath().toString();
		
		List<String> exportPaths = new ArrayList<>();
		exportPaths.add(target.toString() + "\\");
		for (Package p : packages) {
			if (!Compiler.compile(target.toString(), Paths.get(path).toString() + "/src/", p.getName().replace('.', '/'), false, extraCP))
				throw new CompilerException("Error in compilation of " + p.getName());
			if (!Compiler.convert(p.getName(), p.getFormattedAid(), "1.0", target.toString() + "\\", false, exportPaths, p.getApplets(), p.getAppletsAID()))
				throw new CompilerException("Error in conversion of " + p.getName());
			
		}
	}
	
	private Package addPckg(String name, Path path) {
		for (Package p : packages)
			if (p.getName().equals(name))
				return p;
		Package p = new Package(name, path);
		packages.add(p);
		return p;
	}
	private void addOrderedPckg(Package newP, List<Package> oldList) {
		for (Package p : packages)
			if (p == newP)
				return;
		for (String packageName : newP.getImports()) {
			for (Package dependency : oldList) {
				if (packageName.equals(dependency.getName())) {
					addOrderedPckg(dependency, oldList);
				}
			}
		}
		newP.setAid(StringHex.concatenate(new StringHex("A0 00 00 00 00 20"), new StringHex(StringHex.byteToHex((byte) packages.size()))));
		packages.add(newP);
	}
}
