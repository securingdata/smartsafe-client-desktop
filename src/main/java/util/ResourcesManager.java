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


public final class ResourcesManager {
	//private static URLClassLoader loader2 = (URLClassLoader)ResourcesManager.class.getClassLoader();
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
		//return loader.findResource(name);
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
			
			
			try (InputStream is = loader.getResourceAsStream(name); OutputStream os = new FileOutputStream(file.toFile())) {
				int read;
				byte[] bytes = new byte[1024];
				while ((read = is.read(bytes)) > 0) {
					os.write(bytes, 0, read);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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
}
