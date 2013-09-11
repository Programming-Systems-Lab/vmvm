package edu.columbia.cs.psl.vmvm.chroot;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;

import org.objectweb.asm.tree.MethodInsnNode;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.VMState;

public class ChrootUtils {
	
	private static HashMap<String, String> accessedPaths = new HashMap<>();
	private static final boolean CHROOT_ENABLED = true;
	private static final boolean DEBUG_ALL_ACCESS = false;
	private static final boolean DEBUG_WRITES =false;
	private static final boolean FORCE_CHROOT = false;
	
	public static HashSet<String> writtenPaths = new HashSet<>();
	static
	{
		reset(true);
	}
	public static void debugContextCL(ClassLoader cl)
	{
		System.err.println("Chrootdebugcl:" +cl);
		if(cl instanceof URLClassLoader)
		{
			System.err.println(Arrays.toString(((URLClassLoader)cl).getURLs()));
		}
		new Exception().printStackTrace();
	}
	static boolean isInAWrittenPath(String file)
	{
		if(file.equals("/"))
			return false;
		if(writtenPaths.contains(file))
			return true;
		else
		{
			return isInAWrittenPath(new File(file).getParent());
		}
	}
	static WeakHashMap<File, File> fileToFile = new WeakHashMap<>();
	public static String chrootCapture(String path, boolean isOutput)
	{
		if(isOutput || FORCE_CHROOT)
		{
			writtenPaths.add(new File(path).getAbsolutePath());
			path = ensureSafePath(path);
			if(DEBUG_WRITES)
				System.out.println("Redirecting write to " + path);
		}
		else
		{
			if(isInAWrittenPath(new File(path).getAbsolutePath()))
			{
				if(DEBUG_WRITES)
					System.out.println("Redirecting write to " + path);
				return ensureSafePath(path);
			}
		}
		return path;
	}
	public static URLConnection chrootCapture(URLConnection urlConnection, boolean isOutput)
	{
//		System.err.println("URL: "  + urlConnection.getURL());
//		new Exception().printStackTrace();
		if(urlConnection.getURL() != null && urlConnection.getURL().toString().startsWith("file://")){

		if(isOutput || FORCE_CHROOT)
		{
		
			writtenPaths.add(new File(urlConnection.getURL().getFile()).getAbsolutePath());
			URL url = ensureSafeURL(urlConnection.getURL());
			if(DEBUG_WRITES)
				System.out.println("Redirecting write to " + url);
			try {
				return url.openConnection();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			if(isInAWrittenPath(new File(urlConnection.getURL().getFile()).getAbsolutePath()))
			{
				URL url = ensureSafeURL(urlConnection.getURL());
				if(DEBUG_WRITES)
					System.out.println("Redirecting write to " + url);
				try {
					return url.openConnection();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		}
		return urlConnection;
	}
	
	public static URL chrootCapture(URL url, boolean isOutput)
	{
//		System.err.println("URL: "  + urlConnection.getURL());
//		new Exception().printStackTrace();
		if(url.toString().startsWith("file://")){

		if(isOutput || FORCE_CHROOT)
		{
		
			writtenPaths.add(new File(url.getFile()).getAbsolutePath());
			url = ensureSafeURL(url);
			if(DEBUG_WRITES)
				System.out.println("Redirecting write to " + url);
			return url;
		}
		else
		{
			if(isInAWrittenPath(new File(url.getFile()).getAbsolutePath()))
			{
				url = ensureSafeURL(url);
				if(DEBUG_WRITES)
					System.out.println("Redirecting write to " + url);
					return url;
			}
		}
		}
		return url;
	}
	public static File chrootCapture(File file, boolean isOutput)
	{
		if(isOutput || FORCE_CHROOT)
		{
			writtenPaths.add(file.getAbsolutePath());
			File _file = ensureSafeFile(file);
			fileToFile.put(file, _file);
			if(DEBUG_WRITES)
				System.out.println("Redirecting write to " + _file);
			return _file;
		}
		else
		{
			if(isInAWrittenPath(file.getAbsolutePath()))
			{
				File _file = ensureSafeFile(file);
				fileToFile.put(file, _file);
				if(DEBUG_WRITES)
					System.out.println("Redirecting write to " + _file);
				return _file;
			}
		}
		return file;
	}
	public static File chrootCaptureINV(File file, boolean isOutput)
	{
		if(fileToFile.containsKey(file))
			return fileToFile.get(file);
		return file;
	}
	public static Path chrootCapture(Path path, boolean isOutput)
	{
		if(isOutput || FORCE_CHROOT)
		{
			writtenPaths.add(path.toAbsolutePath().toString());
			Path ret = ensureSafeFolder(path);
			if(DEBUG_WRITES)
				System.out.println("Redirecting write to " + ret);
			return ret;
		}
		else if(isInAWrittenPath(path.toAbsolutePath().toString()))
			return ensureSafeFolder(path);
		return path;
	}
	
	
	public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
		return Files.write(path, bytes, options);
	}

	public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs, OpenOption... options) throws IOException {
		return Files.write(path, lines, cs, options);
	}
	public static void reset()
	{
		reset(false);
	}
	private static void reset(final boolean isInit) {
		Path chRootDir = FileSystems.getDefault().getPath(Constants.CHROOT_DIR);
		if (Files.exists(chRootDir) && !isInit) {
			try {
				final HashSet<Path> dirsToDelete = new HashSet<>();
				Files.walkFileTree(chRootDir, new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if(writtenPaths.contains(file) || isInit)
						{
							dirsToDelete.add(file.getParent());
							Files.delete(file);
						}
//						else
//							System.out.println("SKipping deleting " + file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						// try to delete the file anyway, even if its attributes
						// could not be read, since delete-only access is
						// theoretically possible
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (exc == null) {
							if(dirsToDelete.contains(dir) || isInit)
							{
								try{
								dirsToDelete.add(dir.getParent());
								Files.delete(dir);
								}
								catch(DirectoryNotEmptyException ex)
								{
									//XXX nop
								}
							}
							return FileVisitResult.CONTINUE;
						} else {
							// directory iteration failed; propagate exception
							throw exc;
						}
					}
				});
				accessedPaths.clear();
				writtenPaths.clear();
				try{
				Files.createDirectory(chRootDir);
				}
				catch(Exception ex)
				{
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if(!Files.exists(chRootDir))
		{
			try {
				Files.createDirectory(chRootDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		//Create a symlink to emulate cwd
//		Path cwd = new File(".").getAb.toPath();
//		try {
//			Files.createLink(chRootDir.resolve(cwd), chRootDir.resolve(cwd.toAbsolutePath()));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}


	private static synchronized void registerPathAccess(String path) {
		//		new Exception().printStackTrace();
		//					System.out.println("ChrootUtils accessing: " + path);
//System.out.println("Catalina.base: " + System.getProperty("catalina.base"));
//if(System.getProperty("catalina.base") == null)
//	new Exception().printStackTrace();
		if (!accessedPaths.containsKey(path)) {
//						System.out.println("ChrootUtils accessing: " + path);

			String chrootPath = path;
			if (!path.startsWith(Constants.CHROOT_DIR + "/")) {
				chrootPath = Constants.CHROOT_DIR + (path.startsWith("/") ? path : "/" + path);
			} else {
				path = chrootPath.substring(Constants.CHROOT_DIR.length() + 1);
			}
			accessedPaths.put(path, chrootPath);
			final Path existing = FileSystems.getDefault().getPath(path);
			final Path dest = FileSystems.getDefault().getPath(chrootPath);
			final Path chRootDirBase = FileSystems.getDefault().getPath(Constants.CHROOT_DIR);
			if (Files.exists(existing) && !Files.exists(dest)) {
				try {
					Files.createDirectories(dest.getParent());
					if(DEBUG_WRITES)
						System.out.println("Copying " + path + " to " + chrootPath);
					Files.walkFileTree(existing, new FileVisitor<Path>() {

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							Path targetPath = dest.resolve(existing.relativize(dir));
							if (!Files.exists(targetPath)) {
								Files.createDirectory(targetPath);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							try {
								Files.copy(file, dest.resolve(existing.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
							} catch (FileSystemException ex) {
								Files.createSymbolicLink(dest.resolve(existing.relativize(file)), file);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return null;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

					});
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (Files.isDirectory(dest) && Files.exists(existing) && Files.exists(dest)) {
				try {
					if(DEBUG_WRITES)
					System.out.println("Copying " + path + " to " + chrootPath);
					Files.walkFileTree(existing, new FileVisitor<Path>() {

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							Path targetPath = dest.resolve(existing.relativize(dir));
							if (!Files.exists(targetPath)) {
								Files.createDirectory(targetPath);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							try {
								Path targetPath = dest.resolve(existing.relativize(file));
								if(!Files.exists(targetPath))
									Files.copy(file, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
							} catch (FileSystemException ex) {
								Files.createSymbolicLink(dest.resolve(existing.relativize(file)), file);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return null;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

					});
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static String getCaptureMethodName(MethodInsnNode mi) {
		return "_" + mi.owner.replace("/", "_") + "_" + mi.name.replace("<", "_").replace(">", "_");
	}

	public static String getCaptureInitParentMethodName(MethodInsnNode mi) {
		return "_parent_" + mi.owner.replace("/", "_") + "_" + mi.name.replace("<", "_").replace(">", "_");
	}

	public static String ensureSafePath(String s, VMState state) {
		String path = new File(s).getAbsolutePath();
		registerPathAccess(path);
		if (!path.startsWith(Constants.CHROOT_DIR + "/")) {
			path = Constants.CHROOT_DIR + (s.startsWith("/") ? s : "/" + s);
		}
		//		System.out.println(path);
		return path;
	}

	public static String ensureSafePath(String s) {
		if(!CHROOT_ENABLED)
			return s;
		if(DEBUG_ALL_ACCESS)
			System.out.println("Accessing string-based file: " + s);
		if(s.contains("output/build/logs"))
			return s;
		String path = new File(s).getAbsolutePath();
		registerPathAccess(path);
		if (!path.startsWith(Constants.CHROOT_DIR + "/")) {
			path = Constants.CHROOT_DIR + (path.startsWith("/") ? path : "/" + path);
			//			System.out.println("I chrooted" + path);
		}
//		System.out.println("chrootutils returning " + path);
		return path;
	}

	public static Path ensureSafeFolder(Path f) {
		if(!CHROOT_ENABLED)
			return f;
		if(DEBUG_ALL_ACCESS)
			System.out.println("Accessing path: " + f);
		String path = f.toFile().getAbsolutePath();
		registerPathAccess(path);

		if (!path.startsWith(Constants.CHROOT_DIR + "/")) {
			path = Constants.CHROOT_DIR + path;
			//			System.out.println("Fudging temp folder" + path);
			Path r = FileSystems.getDefault().getPath(path);
			try {
				r = Files.createDirectories(r);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return r;
		}
		return f;
	}

	public static File ensureSafeFile(File f) {
		if(!CHROOT_ENABLED)
			return f;
		if(DEBUG_ALL_ACCESS)
			System.out.println("Acessing: " + f);
		String path = f.getAbsolutePath();
		if(path.contains("output/build/logs"))
			return f;
		registerPathAccess(path);
		if (!path.startsWith(Constants.CHROOT_DIR + "/")) {
			path = Constants.CHROOT_DIR + f.getAbsolutePath();
			//			System.out.println("Fudging temp file" + path);
			File r = new File(path);
//			try {
//				f.delete();
//				r.createNewFile();
				return r;
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		return f;
	}

	public static Object[] ensureSafeURLArrayReturn(String spec) throws MalformedURLException {
		if (spec != null && spec.startsWith("file://")) {
			URL ret = new URL(spec);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { "file://" + Constants.CHROOT_DIR + "/" + ret.getPath() };
			}
		}
		return new Object[] { (spec) };
	}

	public static Object[] ensureSafeURLArrayReturn(String protocol, String host, int port, String file) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);
			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { protocol, host, port, Constants.CHROOT_DIR + "/" + file };
			}
		}
		return new Object[] { protocol, host, port, file };
	}

	public static Object[] ensureSafeURLArrayReturn(String protocol, String host, int port, String file, URLStreamHandler handler) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);
			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { protocol, host, port, Constants.CHROOT_DIR + "/" + file, handler };
			}
		}
		return new Object[] { protocol, host, port, file, handler };
	}

	public static Object[] ensureSafeURLArrayReturn(String protocol, String host, String file) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);

			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { protocol, host, Constants.CHROOT_DIR + "/" + file };
			}
		}
		return new Object[] { protocol, host, file };
	}

	public static Object[] ensureSafeURIArrayReturn(String spec) throws URISyntaxException {
		if (spec != null && spec.startsWith("file://")) {
			URI ret = new URI(spec);
			registerPathAccess(ret.getPath());

			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { "file://" + Constants.CHROOT_DIR + "/" + ret.getPath() };
			}
		}
		return new Object[] { (spec) };
	}

	public static Object[] ensureSafeURIArrayReturn(String scheme, String ssp, String fragment) throws URISyntaxException {
		if (scheme != null && scheme.startsWith("file://")) {
			URI ret = new URI(scheme, ssp, fragment);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { scheme, Constants.CHROOT_DIR + ret.getRawSchemeSpecificPart(), fragment };
			}
		}
		return new Object[] { scheme, ssp, fragment };
	}

	public static Object[] ensureSafeURIArrayReturn(String scheme, String userInfo, String host, int port, String path, String query, String fragment) throws URISyntaxException {
		if (scheme != null && scheme.startsWith("file://")) {
			URI ret = new URI(scheme, userInfo, host, port, path, query, fragment);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { scheme, userInfo, host, port, Constants.CHROOT_DIR + ret.getRawSchemeSpecificPart(), query, fragment };
			}
		}
		return new Object[] { scheme, userInfo, host, port, path, query, fragment };
	}

	public static Object[] ensureSafeURIArrayReturn(String scheme, String host, String path, String fragment) throws URISyntaxException {
		if (scheme != null && scheme.startsWith("file://")) {
			URI ret = new URI(scheme, host, path, fragment);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { scheme, host, Constants.CHROOT_DIR + "/" + path, fragment };
			}
		}
		return new Object[] { scheme, host, path, fragment };
	}

	public static Object[] ensureSafeURIArrayReturn(String scheme, String authority, String path, String query, String fragment) throws URISyntaxException {
		if (scheme != null && scheme.startsWith("file://")) {
			URI ret = new URI(scheme, authority, path, query, fragment);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new Object[] { scheme, authority, Constants.CHROOT_DIR + "/" + path, query, fragment };
			}
		}
		return new Object[] { scheme, authority, path, query, fragment };
	}

	public static URL ensureSafeURL(String spec) throws MalformedURLException {
		if (spec != null && spec.startsWith("file://")) {
			URL ret = new URL(spec);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL("file://" + Constants.CHROOT_DIR + "/" + ret.getPath());
			}
		}
		return new URL(spec);
	}

	public static URL ensureSafeURL(String protocol, String host, int port, String file) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);
			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL(protocol, host, port, "/" + Constants.CHROOT_DIR + "/" + file);
			}
		}
		return new URL(protocol, host, port, file);
	}

	public static URL ensureSafeURL(String protocol, String host, int port, String file, URLStreamHandler handler) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);

			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL(protocol, host, port, "/" + Constants.CHROOT_DIR + "/" + file, handler);
			}
		}
		return new URL(protocol, host, port, file, handler);
	}

	public static URL ensureSafeURL(String protocol, String host, String file) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);

			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL(protocol, host, "/" + Constants.CHROOT_DIR + "/" + file);
			}
		}
		return new URL(protocol, host, file);
	}

	public static URI ensureSafeURI(String str) throws URISyntaxException {
		return ensureSafeURI(new URI(str));
	}

	public static URI ensureSafeURI(String scheme, String ssp, String fragment) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, ssp, fragment));
	}

	public static URI ensureSafeURI(String scheme, String userInfo, String host, int port, String path, String query, String fragment) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, userInfo, host, port, path, query, fragment));
	}

	public static URI ensureSafeURI(String scheme, String host, String path, String fragment) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, host, path, fragment));
	}

	public static URI ensureSafeURI(String scheme, String authority, String path, String query, String fragment) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, authority, path, query, fragment));
	}

	public static URL ensureSafeURL(String spec, VMState state) throws MalformedURLException {
		if (spec != null && spec.startsWith("file://")) {
			URL ret = new URL(spec);
			registerPathAccess(ret.getPath());
			if (!ret.getPath().startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL("file://" + Constants.CHROOT_DIR + "/" + ret.getPath());
			}
		}
		return new URL(spec);
	}

	public static URL ensureSafeURL(String protocol, String host, int port, String file, VMState state) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);
			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL(protocol, host, port, "/" + Constants.CHROOT_DIR + "/" + file);
			}
		}
		return new URL(protocol, host, port, file);
	}

	public static URL ensureSafeURL(String protocol, String host, int port, String file, URLStreamHandler handler, VMState state) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);

			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL(protocol, host, port, "/" + Constants.CHROOT_DIR + "/" + file, handler);
			}
		}
		return new URL(protocol, host, port, file, handler);
	}

	public static URL ensureSafeURL(String protocol, String host, String file, VMState state) throws MalformedURLException {
		if ("file".equals(protocol)) {
			registerPathAccess(file);

			if (!file.startsWith(Constants.CHROOT_DIR + "/")) {
				return new URL(protocol, host, "/" + Constants.CHROOT_DIR + "/" + file);
			}
		}
		return new URL(protocol, host, file);
	}

	public static URI ensureSafeURI(String str, VMState state) throws URISyntaxException {
		return ensureSafeURI(new URI(str), state);
	}

	public static URI ensureSafeURI(String scheme, String ssp, String fragment, VMState state) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, ssp, fragment), state);
	}

	public static URI ensureSafeURI(String scheme, String userInfo, String host, int port, String path, String query, String fragment, VMState state) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, userInfo, host, port, path, query, fragment), state);
	}

	public static URI ensureSafeURI(String scheme, String host, String path, String fragment, VMState state) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, host, path, fragment), state);
	}

	public static URI ensureSafeURI(String scheme, String authority, String path, String query, String fragment, VMState state) throws URISyntaxException {
		return ensureSafeURI(new URI(scheme, authority, path, query, fragment), state);
	}

	public static URI ensureSafeURI(URI uri, VMState state) {
		if (uri != null && uri.getScheme() != null && uri.getScheme().startsWith("file")) {
			File f = new File(uri);
			registerPathAccess(f.getAbsolutePath());

			if (!f.getAbsolutePath().startsWith(Constants.CHROOT_DIR + "/")) {
				String path = Constants.CHROOT_DIR + f.getAbsolutePath();
				return new File(path).toURI();
			}
		}
		return uri;
	}

	public static URI ensureSafeURI(URI uri) {
		if (uri != null && uri.getScheme() != null && uri.getScheme().startsWith("file")) {
			File f = new File(uri);
			registerPathAccess(f.getAbsolutePath());

			if (!f.getAbsolutePath().startsWith(Constants.CHROOT_DIR + "/")) {
				String path = Constants.CHROOT_DIR + f.getAbsolutePath();
				return new File(path).toURI();
			}
		}
		return uri;
	}
	public static URL ensureSafeURL(URL url) {
		try {
			return ensureSafeURI(url.toURI()).toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static URL ensureSafeURL(URL url, VMState state) {
		try {
			return ensureSafeURI(url.toURI(), state).toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
