package edu.columbia.cs.psl.vmvm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
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

	private static BufferedWriter		log;
	static {
		try{
		moreIgnoredImmutables = new HashSet<Class<?>>();
		moreIgnoredImmutables.add(ClassLoader.class);
		moreIgnoredImmutables.add(Thread.class);
		moreIgnoredImmutables.add(URI.class);
		moreIgnoredImmutables.add(File.class);
		moreIgnoredImmutables.add(ZipFile.class);
		moreIgnoredImmutables.add(ZipEntry.class);
		moreIgnoredImmutables.add(Inflater.class);
//		moreIgnoredImmutables.add(InputStream.class);
//		moreIgnoredImmutables.add(OutputStream.class);
		moreIgnoredImmutables.add(Deflater.class);
//		moreIgnoredImmutables.add(Socket.class);
//		moreIgnoredImmutables.add(ServerSocket.class);
//		moreIgnoredImmutables.add(Channel.class);
//		moreIgnoredImmutables.add(Closeable.class);
		moreIgnoredImmutables.add(Class.class);
		cloner.setExtraImmutables(moreIgnoredImmutables);
//		cloner.setDumpClonedClasses(true);
//		try {
			File f = new File("cloneLog");
			if (f.exists())
				f.delete();
			log = new BufferedWriter(new FileWriter("cloneLog"));
		}
//		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		catch(Throwable t)
		{
			System.err.println("AHAHAHAHAHAHAH");
			t.printStackTrace();
//			throw t;
		}
	}

	public static final <T> T clone(T obj, String debug, int sandboxFlag) {
// 		System.out.println("source>"+obj + ";"+debug + "; " + sandboxFlag);
			return cloner.deepClone(obj, sandboxFlag);
	}
	public static final <T> T clone(T obj, String debug) {
		if(obj == null)
			return null;
		try {
			log.append(debug);			
			log.newLine();
			log.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cloner.deepClone(obj,0);
	}

	public static String debugString(String in)
	{
		if(in != null && in.contains("equalsstartt"))
		{
			System.out.println("Method return: >" + in);
			new Exception().printStackTrace();
		}
		return in;
	}
	public static IdentityHashMap<Object, Object>	cloneCache	= new IdentityHashMap<Object, Object>();	;
	

}
