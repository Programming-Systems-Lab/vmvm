package edu.columbia.cs.psl.vmvm.chroot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;

public class MethodLister {
	public static void main(String[] args) {
		MethodLister detector = new MethodLister(new String[] { "/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home/jre/lib/rt.jar" });
		Scanner s = new Scanner(System.in);
		System.out.println("? ");
		while(s.hasNextLine())
		{
			detector.findMethod(s.nextLine());
			System.out.println("? ");
		}
	}

	private LinkedList<String> methods = new LinkedList<String>();
	public void findMethod(String prefix)
	{
		for(String s : methods)
		{
			if(s.contains(prefix))
				System.out.println(s);
		}
	}
	public MethodLister(String[] jarPath) {
		JarFile classJar;
		for (String path : jarPath) {
			try {
				classJar = new JarFile(path);

				Enumeration<JarEntry> jarContents = classJar.entries();
				int i = 0;
				while (jarContents.hasMoreElements()) {
					String name = jarContents.nextElement().getName();
					if (!name.endsWith(".class"))
						continue;
					if (!name.startsWith("java"))
						continue;
					name = name.substring(0, name.length() - 6);
					Class c = Class.forName(name.replace("/", "."));
					for (Method m : c.getMethods()) {
						methods.add(name + "." + m.getName() + Type.getMethodDescriptor(m));
					}
					for (Constructor m : c.getConstructors()) {
						methods.add(name + ".<init>" + Type.getConstructorDescriptor(m));
					}
				}
				classJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Collections.sort(methods);
		
		try {
			File f = new File("all-methods.txt");
			if (f.exists())
				f.delete();
			FileWriter fw = new FileWriter(f);
			for (String s : methods)
				fw.append(s + "\n");
			fw.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
