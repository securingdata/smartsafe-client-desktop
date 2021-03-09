package util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import smartsafe.Prefs;


public final class ResourcesManager {
	private static ClassLoader loader = Thread.currentThread().getContextClassLoader();
	private static Map<String, Path> customLoaders = new HashMap<>();
	private ResourcesManager() {}
	
	public static URLClassLoader loaderFromJar(String jar) {
		Path jarPath = customLoaders.get(jar);
		if (jarPath == null) {
			jarPath = getFile(jar);
			customLoaders.put(jar, jarPath);
		}
		try {
			return new URLClassLoader(new URL[]{jarPath.toUri().toURL()});
		} catch (MalformedURLException e) {
			return null;
		}
	}
	public static Class<?> loadClass(String jar, String className) {
		try {
			return loaderFromJar(jar).loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	public static URL getURLFile(String name) {
		return loader.getResource(name);
	}
	public static InputStream getResourceAsStream(String name) {
		return loader.getResourceAsStream(name);
	}
	public static Path getFileInDir(String name, String directory) {
		try {
			Path dir = Files.createTempDirectory("tmp");
			Path file = getFile(name);
			Path dest = Paths.get(dir.toFile().getAbsolutePath(), directory, name.substring(name.lastIndexOf('/') + 1));
			dest.getParent().toFile().mkdir();
			Files.copy(file, dest);
			recursiveDeleteOnExit(dir);
			return dir;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static void recursiveDeleteOnExit(Path p) {
		try {
			Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
				@Override
			    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					file.toFile().deleteOnExit();
					return FileVisitResult.CONTINUE;
				}
				@Override
			    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					dir.toFile().deleteOnExit();
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {}
	}
	public static Path getFile(String name) {
		URL url = getURLFile(name);
		if (url == null)
			return null;
		
		if (url.toExternalForm().startsWith("jar:")) {
			Path file;
			try {
				file = Files.createTempFile(name.substring(name.lastIndexOf('/') + 1), "");
			} catch (IOException e1) {
				return null;
			}
			file.toFile().deleteOnExit();
			
			copy(name, file);
			return file;
		}
		else {
			try {
				return Paths.get(url.toURI());
			} catch (URISyntaxException e) {
				return null;
			}
		}
	}
	public static Path initHtmlDirectory(boolean darkTheme) {
		URL url = getURLFile("html/");
		if (url == null)
			return null;
		
		//Creating root directory
		Path dir;
		try {
			dir = Files.createTempDirectory(null);
		} catch (IOException e) {
			return null;
		}
		recursiveDeleteOnExit(dir);
		
		//Loading global static
		Path staticDir;
		try {
			staticDir = Files.createDirectory(Paths.get(dir.toString(), "static"));
			copyStaticDir("html/static/", "", staticDir.toString());
			if (darkTheme) {
				staticDir.resolve("0.css").toFile().delete();//Remove classic css theme
				staticDir.resolve("1.css").toFile().renameTo(staticDir.resolve("0.css").toFile());//Help pages use 0.css for theme
			}
		} catch (IOException e) {
			return null;
		}
		
		//Creating directory of current locale
		try {
			String tmp = Prefs.get(Prefs.KEY_LANGUAGE).substring(0, 2).toLowerCase();
			Path locDir =  Files.createDirectory(Paths.get(dir.toString(), tmp));
			Files.createDirectory(Paths.get(locDir.toString(), "static"));
			return locDir;
		} catch (IOException e) {
			return null;
		}
	}
	public static String loadHtmlPage(Path htmlDir, String name) {
		try {
			String nameH = name + ".html";
			Path page = Paths.get(htmlDir.toString(), nameH);
			if (page.toFile().exists())
				return page.toUri().toURL().toExternalForm();
			
			String locDir = Prefs.get(Prefs.KEY_LANGUAGE).substring(0, 2).toLowerCase() + "/";
			copy("html/" + locDir + nameH, page);
			copyStaticDir("html/" + locDir + "static/", name, Paths.get(htmlDir.toString(), "static").toString());
			return page.toUri().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	private static void copyStaticDir(String src, String pre, String dst) {
		copyStaticDir(src, pre, dst, ".css");
		copyStaticDir(src, pre, dst, ".png");
	}
	private static void copyStaticDir(String src, String pre, String dst, String extension) {
		for (int i = 0; ;i++ ) {
			if (!copy(src + pre + i + extension, Paths.get(dst, pre + i + extension)))
				return;
		}
	}
	private static boolean copy(String src, Path dst) {
		if (getURLFile(src) == null)
			return false;
		try (InputStream is = loader.getResourceAsStream(src); OutputStream os = new FileOutputStream(dst.toFile())) {
			int read;
			byte[] bytes = new byte[1024];
			while ((read = is.read(bytes)) > 0) {
				os.write(bytes, 0, read);
			}
		} catch (IOException e) {
			return false;
		}
		dst.toFile().deleteOnExit();
		return true;
	}
}
