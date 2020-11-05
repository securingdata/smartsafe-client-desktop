package compiler;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import javafx.beans.property.StringProperty;
import util.CustomSecurityManager;
import util.ResourcesManager;
import util.StringHex;

public class Compiler {
	private static final Interceptor interceptor;
	
	private static StringProperty logListener;
	
	private static final String CONVERTER = "com.sun.javacard.converter.Main";

	private static final String JC_TOOLS   = "lib/tools.jar";
	private static final String API        = "lib/api.jar";
	private static final String EXP        = "api_export_files/";
	private static       String JC_HOME;
	private static       String GP_HOME;
	
	public static final int JC_302 = 302;
	public static final int JC_304 = 304;
	public static final int JC_305 = 305;
	
	public static final int GP_15 = 15;
	
	static {
		interceptor = new Interceptor();
		changeJCVersion(JC_302);
		changeGPVersion(GP_15);
		System.setSecurityManager(CustomSecurityManager.getSecurityManager());
	}
	
	private static Method getMain(String jarPath, String mainPath) {
		Class<?> javac = ResourcesManager.loadClass(jarPath, mainPath);
		Method m = null;
		try {
			m = javac.getDeclaredMethod("main", new String[0].getClass());
		} catch (NoSuchMethodException | SecurityException | NullPointerException e) {
			System.out.println("Compiler has not been correctly initialised!");
			e.printStackTrace();
		}
		return m;
	}
	private static void exec(Method m, String[] args) {
		CustomSecurityManager.getSecurityManager().setBlockExit(true);
		try {
			m.invoke(null, new Object[]{args});
		}
		catch(Throwable e) {}
		finally {
			CustomSecurityManager.getSecurityManager().setBlockExit(false);
		}
	}
	
	public static void setLogListener(StringProperty sp) {
		logListener = sp;
	}
	public static void changeJCVersion(int version) {
		JC_HOME = "bin/javacard/v_" + version + "/";
		System.setProperty("jc.home", ResourcesManager.getFileInDir(JC_HOME + API, "lib").toFile().getAbsolutePath());
	}
	public static void changeGPVersion(int version) {
		GP_HOME = "bin/gp/v_" + version + "/";
	}
	
	public static boolean compile(String outDir, String srcDir, String packagePath, boolean integer, String ... extraClasspath) {
		Path jcAPI = ResourcesManager.getFile(JC_HOME + API);
		Path gpAPI = ResourcesManager.getFile(GP_HOME + API);
		List<String> srcFiles = new LinkedList<>();
		List<String> options = new LinkedList<>();
		options.add("-source"); options.add("1.6");
		options.add("-target"); options.add("1.6");
		options.add("-d");  options.add(outDir);
		
		String classpath = jcAPI.toFile().getAbsolutePath() + ";" + gpAPI.toFile().getAbsolutePath() + ";" + srcDir;
		if (extraClasspath != null) {
			for (String cp : extraClasspath)
				classpath += ";" + cp;
		}
		options.add("-cp"); options.add(classpath);
		options.add("-bootclasspath"); options.add(ResourcesManager.getFile(JC_HOME + API).toFile().getAbsolutePath());
		
		for (File file : Paths.get(srcDir, packagePath).toFile().listFiles()) {
			if (file.getName().endsWith(".java") && ((!integer && !file.getName().endsWith("Int.java")) || integer))
				srcFiles.add(file.getAbsolutePath());
		}
		
		return javac(srcFiles, options);
	}
	public static boolean javac(List<String> srcFiles, List<String> options) {
		boolean result = false;
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
	    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
	    	Iterable<? extends JavaFileObject> fileUnits = fileManager.getJavaFileObjectsFromStrings(srcFiles);
	    	result = compiler.getTask(null, fileManager, diagnostics, options, null, fileUnits).call();
	    }
	    catch(Exception e) {}
	    
    	List<Diagnostic<? extends JavaFileObject>> list = diagnostics.getDiagnostics();
    	for (Diagnostic<? extends JavaFileObject> d : list) {
    		if (logListener != null)
    			logListener.set("" + d);
    	}
	    return result;
	}
	
	public static boolean convert(String packageName, String packageAID, String version, String classDir, boolean integer, List<String> exportPaths, List<String> applets, List<StringHex> appletsAID) {
		Path jcExp = ResourcesManager.getFile(JC_HOME + EXP);
		Path gpExp = ResourcesManager.getFile(GP_HOME + EXP);
		List<String> args = new LinkedList<>();
		args.add("-classdir"); args.add(classDir);
		args.add("-d");        args.add(classDir);
		for (int i = 0; i < applets.size(); i++) {
			args.add("-applet");
			if (appletsAID == null || appletsAID.size() != applets.size())
				args.add(packageAID + ":0x" + StringHex.byteToHex((byte) i)); 
			else
				args.add("0x" + appletsAID.get(i).toString().replaceAll(" ", ":0x"));
			args.add(applets.get(i));
		}
		
		if (integer)
			args.add("i");
		args.add("-out"); args.add("CAP"); args.add("EXP"); args.add("JCA");
		
		String export = jcExp.toFile().getAbsolutePath() + ";" + gpExp.toFile().getAbsolutePath();
		for (String ep : exportPaths)
			export += ";" + ep;
		args.add("-exportpath"); args.add(export);
			
		args.add(packageName); args.add(packageAID); args.add(version);
		
		return converter(args.toArray(new String[0]));
	}
	public static boolean converter(String ... args) {
		interceptor.attach();
		exec(getMain(JC_HOME + JC_TOOLS, CONVERTER), args);
		
		String errPrivate = interceptor.getRecord();
		interceptor.detach();
		
		int endOffset = errPrivate.indexOf("INFOS: conversion completed with ");
		if (endOffset == -1)
			endOffset = 0;
		else
			endOffset = errPrivate.indexOf("\n", endOffset);
		if (logListener != null)
			logListener.set(errPrivate.substring(0, endOffset));
		
		return errPrivate.indexOf("INFOS: conversion completed with 0 errors") != -1;
	}
}
