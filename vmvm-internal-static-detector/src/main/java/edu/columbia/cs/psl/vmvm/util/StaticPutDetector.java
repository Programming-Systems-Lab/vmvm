package edu.columbia.cs.psl.vmvm.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;


public class StaticPutDetector {
	private static final Logger logger = Logger.getLogger(StaticPutDetector.class);
	private static FileWriter fw;
	public static void main(String[] args) throws Throwable {
		File f = new File("static-mod-methods.txt");
		if (f.exists())
			f.delete();
//		fw = new FileWriter(f);
		Instrumenter.pass_number = 0;
		Instrumenter.MAX_CLASSES = Integer.valueOf(args[1]);
		long start = System.currentTimeMillis();
		Instrumenter.processJar(new File(args[0]),null);
		Instrumenter.finishedPass();
		long end = System.currentTimeMillis();
		System.err.println(Instrumenter.classesInstrumented +"\t"+(end-start));
//		for(MethodListClassNode cn : Instrumenter.instrumentedClasses.values())
//		{
//			if(cn.clInitCalculatedNecessary && (cn.name.startsWith("java") || cn.name.startsWith("System")))
//			{
//				System.err.println(cn.name);
//			}
//		}
//		fw.close();
//		findAllStatics("/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home/jre/lib/rt.jar");
	}
	private static void findAllStatics(String path)
	{
		JarFile classJar;
		try {
			classJar = new JarFile(path);

			Enumeration<JarEntry> jarContents = classJar.entries();
			int i = 0;
			while (jarContents.hasMoreElements()) {
				String name = jarContents.nextElement().getName();
				if (!name.endsWith(".class"))
					continue;
				name = name.substring(0, name.length() - 6);
				try{
				ClassReader cr = new ClassReader(name);
				StaticFinderCV ccv = new StaticFinderCV(Opcodes.ASM4)
;				cr.accept(ccv, 0);
				}
				catch(IOException ex)
				{
					System.out.println(name);
				    ex.printStackTrace();
				}
				i++;
				if (i % 5000 == 0)
					logger.info(i + " classes processed");
			}
			classJar.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	static class StaticFinderCV extends ClassVisitor{
		private String className;
		public StaticFinderCV(int api) {
			super(api);
		}
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.className = name;
		}
		@Override
		public void visitEnd() {
			if(isBad && className.startsWith("java") && (sets.size() >0 || adds.size() >0))
				System.err.println(className + adds + "." + removes + "."+sets+"."+gets);
			super.visitEnd();
		}
		private boolean isBad = false;
		private HashSet<String> adds = new HashSet<String>();
		private HashSet<String> sets = new HashSet<String>();
		private HashSet<String> gets = new HashSet<String>();
		private HashSet<String> removes = new HashSet<String>();
		@Override
		public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc, String signature, String[] exceptions) {
			if((access & Opcodes.ACC_STATIC) != 0 && (methodName.startsWith("set") || methodName.startsWith("add") || methodName.startsWith("remove") || methodName.startsWith("get")))
			{
				if((access & Opcodes.ACC_PUBLIC) == 0)
					return null;
				if(methodName.startsWith("add"))
					adds.add(methodName);
				if(methodName.startsWith("remove"))
					removes.add(methodName);
				if(methodName.startsWith("set"))
					sets.add(methodName);
				if(methodName.startsWith("get"))
					gets.add(methodName);
				
				isBad = true;
			}
			return null;
		}
		
	}
}
