package edu.columbia.cs.psl.vmvm.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;

public class ClassLoaderBasedTransformer implements ClassFileTransformer{
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		if(className.startsWith("java") || className.startsWith("sun") || className.startsWith("com/sun"))
			return null;
		try {
			URLClassLoader ldr = new URLClassLoader(new URL[]{ new File(System.getProperty("VMVMLib")).toURL()}, loader);
			ClassFileTransformer t = (ClassFileTransformer) ldr.loadClass("edu.columbia.cs.psl.vmvm.agent.VMVMClassFileTransformer").newInstance();
			ldr.close();
			return t.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
