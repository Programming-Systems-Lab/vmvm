package edu.columbia.cs.psl.vmvm.chroot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;


public class FSDetector {
	private static Logger			logger						= Logger.getLogger(FSDetector.class);
	
	public static HashSet<String> fsMethods = new HashSet<String>();
	public static HashMap<String, ClassNode> classInfo = new HashMap<String, ClassNode>();
	static
	{
		Scanner s;
		try {
			s = new Scanner(new File("core-fs.txt"));
			while(s.hasNextLine())
			{
				fsMethods.add(s.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}	
	}
	public void printNativeDeterministic() {
		ArrayList<String> toPrint = new ArrayList<String>();
		for (MethodInstance mi : methodMap.values()) {
			if (mi.isFSTaint()) {
				if (mi.tainted != null && mi.tainted.size() > 0)
					toPrint.add(mi.getFullName() + mi.tainted);
			}
		}
		Collections.sort(toPrint);
		for (String s : toPrint)
			System.out.println(s);
	}

	public static void main(String[] args) {
		logger.info("Building links");
		FSDetector detector = new FSDetector(
				new String[] {"/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home/jre/lib/rt.jar"}
				);
		logger.info("Initialized.. now writing to disk");

		try {
			File f = new File("fs-methods.txt");
			if (f.exists())
				f.delete();
			FileWriter fw = new FileWriter(f);
			for (MethodInstance mi : detector.getNonDeterministicMethods())
				fw.append(mi.getFullName() + "\n");
			fw.close();
			logger.info("Done");
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	private HashMap<String, MethodInstance>	methodMap					= new HashMap<String, MethodInstance>();
	private HashMap<String, ClassInstance>	classMap					= new HashMap<String, ClassInstance>();

	private Collection<MethodInstance>		nonDeterministicMethodCache	= null;

	public FSDetector(String[] jarPath) {
		JarFile classJar;
		for(String path : jarPath)
		{
		try {
			classJar = new JarFile(path);

			Enumeration<JarEntry> jarContents = classJar.entries();
			int i = 0;
			while (jarContents.hasMoreElements()) {
				String name = jarContents.nextElement().getName();
				if (!name.endsWith(".class"))
					continue;
				if(!name.startsWith("java"))
					continue;
				name = name.substring(0, name.length() - 6);
				ClassReader cr1 = new ClassReader(name);
				ClassNode cn = new ClassNode();
				cr1.accept(cn, 0);
				ClassNode cn2 = new ClassNode();
				cn2.superName = cn.superName;
				cn2.interfaces = cn.interfaces;
				classInfo.put(name, cn2);
				ClassReader cr = new ClassReader(name);
				FSClassVisitor ccv = new FSClassVisitor(Opcodes.ASM4, new ClassWriter(0), methodMap, classMap);
				cr.accept(ccv, 0);
				i++;
				if (i % 1000 == 0)
					logger.info(i + " classes processed");
			}
			classJar.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}


	public Collection<MethodInstance> getNonDeterministicMethods() {
		if (nonDeterministicMethodCache != null)
			return nonDeterministicMethodCache;
		buildNonDeterministicMethods();
	
		return nonDeterministicMethodCache;
	}

	private void buildNonDeterministicMethods() {
		HashMap<String, MethodInstance> toReturn = new HashMap<String, MethodInstance>();
		for (String s : methodMap.keySet()) {
			MethodInstance mi = methodMap.get(s);
			if (mi.isFSTaint()) {
				toReturn.put(s, mi);
//				for (String caller : mi.functionsThatCallMe)
//					addAllRecursively(methodMap.get(caller), toReturn);
			}
		}
		nonDeterministicMethodCache = toReturn.values();
//
//		int numChanged = 0;
//		for (MethodInstance mi : toReturn.values()) {
//			ClassInstance ci = classMap.get(mi.getClazz());
//			if (ci == null)
//				continue;
//			if (ci.parent != null && !ci.parent.equals("java/lang/Object") && ! ci.parent.contains("Exception") && ! ci.parent.contains("Throwable") && !mi.getMethod().getName().contains("<init>")) {
//				String fName = ci.parent + "." + mi.getMethod().getName() + ":" + mi.getMethod().getDescriptor();
//				if (methodMap.containsKey(fName) && !methodMap.get(fName).isNonDeterministic() && !deterministicNativeMethods.contains(fName)) {
//					methodMap.get(fName).setNonDeterministic(true);
//					numChanged++;
//				}
//			}
//			if (ci.interfaces != null && ci.interfaces.length > 0) {
//				for (String iName : ci.interfaces) {
//					String fName = iName + "." + mi.getMethod().getName() + ":" + mi.getMethod().getDescriptor();
//					if (methodMap.containsKey(fName) && !methodMap.get(fName).isNonDeterministic() && !deterministicNativeMethods.contains(fName)) {
//						methodMap.get(fName).setNonDeterministic(true);
//						numChanged++;
//					}
//				}
//			}
//		}
//		
//		HashSet<String> taintedFields =  new HashSet<String>();
//		for(MethodInstance mi : toReturn.values())
//		{
//			taintedFields.addAll(mi.fieldsPut);
//			taintedFields.addAll(mi.fieldsGet);
//		}
//		
//		for(MethodInstance mi : methodMap.values())
//		{
//			if(!mi.isNonDeterministic() && !deterministicNativeMethods.contains(mi.getFullName()))
//			{
//				for(String s : mi.fieldsGet)
//			{
//				if(taintedFields.contains(s))
//				{
//					numChanged++;
//					mi.setNonDeterministic(true);
//					continue;
//				}
//			}
//			}
//		}
//		if (numChanged != 0)
//			buildNonDeterministicMethods();
	}

	/**
	 * Handy for debugging... Call it with level =0, alreadyPrinted a new
	 * hashSet, fallback as mi.getFullName
	 * 
	 * @param mi
	 * @param level
	 * @param alreadyPrinted
	 * @param fallback
	 */
	private void printWhatICall(MethodInstance mi, int level, HashSet<String> alreadyPrinted, String fallback) {
		String r = "";
		r += "|";
		for (int i = 0; i < level; i++)
			r += "--";
		if (mi == null) {
			r += ("NULL!!!!!!!!!->" + fallback);
			logger.error(r);
			return;

		}

		logger.error(r + mi.getFullName() + " [" + (mi.isFSTaint() ? "NA" : "") + (mi.isFSTaint() ? "ND" : "") + "]");
		if (!alreadyPrinted.contains(mi.getFullName())) {
			alreadyPrinted.add(mi.getFullName());
			for (String s : mi.functionsThatICall)
				printWhatICall(methodMap.get(s), level + 1, alreadyPrinted, s);
		}
	}
	private String getParentsOf(String s)
	{
		ClassInstance ci = classMap.get(s);
		String ret = ci.parent;
		if(ci.parent != null && !ci.parent.equals("java/lang/Object"))
			ret = ret + " --> " + getParentsOf(ci.parent);
		return ret;
	}
	public void whyNative(String s) {
		getNonDeterministicMethods();
		
		for (MethodInstance mi : methodMap.values())
			if (mi.getFullName().startsWith(s)) {
				System.out.println(s);
				System.out.println("Parent: " + getParentsOf(mi.getClazz()));

				printWhatICall(mi, 0, new HashSet<String>(), mi.getFullName());
			}
	}
}
