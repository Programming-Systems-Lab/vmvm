package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class IFaceClassloaderITCase {

	static boolean doEvil;
	@Test
	public void testClassesLoadedByIfaceCLRightCL() throws Exception {
		File f = new File("/Users/jon/Documents/PSL/Projects/vmvm-jvmti/vmvm/resources/java/");
		URLClassLoader cl = new URLClassLoader(new URL[]{f.toURL()});
		System.out.println(Arrays.toString(cl.getURLs()));
		Class<?> c = cl.loadClass("edu.columbia.cs.psl.vmvmtest.classes.ClassLoadingIface");
		LinkedList<Class<?>> o = (LinkedList<Class<?>>) c.getDeclaredField("foo").get(null);
		Class<?> oc = o.getFirst();
		System.out.println(c.toString() + "" +c.getClassLoader().toString());
		System.out.println(oc.toString() +  oc.getClassLoader().toString());
		cl.close();
		c = null;
		cl = null;
		Reinitializer.markAllClassesForReinit();
		cl = new URLClassLoader(new URL[]{f.toURL()});
		c = cl.loadClass("edu.columbia.cs.psl.vmvmtest.classes.ClassLoadingIface");
		o = (LinkedList<Class<?>>) c.getDeclaredField("foo").get(null);
		oc = o.getFirst();
		System.out.println(c.toString() + "" +c.getClassLoader().toString());
		System.out.println(oc.toString() +  oc.getClassLoader().toString());
	}
}
