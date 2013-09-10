package edu.columbia.cs.psl.vmvm;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.Channel;
import java.security.Permissions;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.rits.cloning.Cloner;

public class CloningUtils {
	public static boolean				CATCH_ALL_ERRORS	= true;
	private static Cloner				cloner				= new Cloner();
	public static ReadWriteLock		exportLock			= new ReentrantReadWriteLock();
	private static HashSet<Class<?>>	moreIgnoredImmutables;
	private static HashSet<Class<?>>	nullInsteads;

//	private static BufferedWriter		log;
	static {
		moreIgnoredImmutables = new HashSet<Class<?>>();
		moreIgnoredImmutables.add(ClassLoader.class);
		moreIgnoredImmutables.add(Thread.class);
		moreIgnoredImmutables.add(URI.class);
		moreIgnoredImmutables.add(File.class);
		moreIgnoredImmutables.add(ZipFile.class);
		moreIgnoredImmutables.add(ZipEntry.class);
		moreIgnoredImmutables.add(Inflater.class);
		moreIgnoredImmutables.add(InputStream.class);
		moreIgnoredImmutables.add(OutputStream.class);
		moreIgnoredImmutables.add(Deflater.class);
		moreIgnoredImmutables.add(Socket.class);
		moreIgnoredImmutables.add(ServerSocket.class);
		moreIgnoredImmutables.add(Channel.class);
		moreIgnoredImmutables.add(Closeable.class);
		moreIgnoredImmutables.add(Class.class);
		cloner.setExtraNullInsteadOfClone(moreIgnoredImmutables);
		cloner.setExtraImmutables(moreIgnoredImmutables);
		
		nullInsteads = new HashSet<Class<?>>();
		nullInsteads.add(Permissions.class);
		cloner.setExtraNullInsteadOfClone(nullInsteads);
//		cloner.setDumpClonedClasses(true);
//		try {
//			File f = new File("cloneLog");
//			if (f.exists())
//				f.delete();
//			log = new BufferedWriter(new FileWriter("cloneLog"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public static final <T> T clone(T obj, String debug, int sandboxFlag) {
// 		System.out.println("source>"+debug);
			return cloner.deepClone(obj, sandboxFlag);
	}

	public static IdentityHashMap<Object, Object>	cloneCache	= new IdentityHashMap<Object, Object>();	;
	

}
