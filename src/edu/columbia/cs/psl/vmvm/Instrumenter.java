package edu.columbia.cs.psl.vmvm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;

import edu.columbia.cs.psl.vmvm.asm.ClinitPrintingCV;
import edu.columbia.cs.psl.vmvm.asm.InterceptingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.IntializedTypeAnalyzer;
import edu.columbia.cs.psl.vmvm.asm.IntializedTypeAnalyzer.Node;
import edu.columbia.cs.psl.vmvm.asm.JUnitResettingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.MethodVisitorFactory;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFieldIsolatorMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFinalMutibleizer;
import edu.columbia.cs.psl.vmvm.asm.struct.EqFieldInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassReader;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassWriter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.GeneratorAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.JSRInlinerAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.SerialVersionUIDAdder;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.ClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.InsnList;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.AnalyzerException;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.BasicInterpreter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.Frame;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.util.CheckClassAdapter;

public class Instrumenter {
	public static URLClassLoader loader;
	public static final boolean DEBUG=false;
	
	public static MethodVisitorFactory methodVisitorFactory;
	static
	{
		try{
			//Load properties file
			Scanner s = new Scanner(Instrumenter.class.getResourceAsStream("vmvm.properties"));
			while(s.hasNextLine())
			{
				String[] d = s.nextLine().split("=");
				if(d[0].equals("methodVisitorFactory"))
				{
					methodVisitorFactory = (MethodVisitorFactory) Class.forName(d[1]).newInstance();
				}
				else
					System.err.println("Warn: ignoring option "+d[0]+"="+d[1]);
			}
			s.close();
		}
		catch(Throwable t)
		{
			//nop
		}
	}
	public static HashSet<String> ignoredClasses = new HashSet<>();
	static
	{
		try{
		Scanner s = new Scanner(Instrumenter.class.getResourceAsStream("ignored-clinits"));
		while(s.hasNextLine())
			ignoredClasses.add(s.nextLine());
		s.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			//nop
		}
	}
	private static final ClassVisitorFactory cvFactory = 
//			new DebugVisitorFactory();
			new JUnitVisitorFactory();
	
	private static Logger logger = Logger.getLogger("edu.columbia.cs.psl.vmvm");
	public static HashMap<String, MethodListClassNode> instrumentedClassesBypath = new HashMap<String, MethodListClassNode>();

	public static HashMap<String, MethodListClassNode> instrumentedClasses = new HashMap<String, MethodListClassNode>();
	private static HashMap<String, MethodListClassNode> unInstrumentedClasses = new HashMap<String, MethodListClassNode>();
	public static HashSet<String> remappedInterfaces = new HashSet<>();
	public static HashMap<String, HashSet<String>> superToSubClass = new HashMap<>();
	private static final int NUM_PASSES = 2;
	private static final int PASS_ANALYZE = 0;
	private static final int PASS_OUTPUT = 1;

	public static int pass_number = 0;

	private static File rootOutputDir;
	private static String lastInstrumentedClass;
	public static HashSet<String> finalClasses = new HashSet<String>();
//	public static HashSet<String> anonClasses = new HashSet<String>();

	public static HashSet<String> finalMethods = new HashSet<String>();
	public static HashSet<String> finalFields = new HashSet<String>();

	public static int MAX_SANDBOXES = 2;
	public static HashMap<String, String> mutablizedFields = new HashMap<>();
	public static void finishedPass() {
		switch (pass_number) {
		case PASS_ANALYZE:
			calculateMutableClasses();
			calculateIgnoredReRunningClinits();
			calculatedWhatsClinitedPerMethod();
			if(DEBUG)
			{
				System.out.println("Ignored clinits: " + ignoredClasses);
			}
			break;
		case PASS_OUTPUT:
			File genDir = new File(rootOutputDir,"gen");
			genDir.mkdirs();
			//Write out all of the info on classes that we have already instrumented
			File f = new File(genDir,"vmvm-runtimecheat");
			if (f.exists())
				f.delete();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
				oos.writeObject(instrumentedClasses); //TODO reenable this
				oos.writeObject(remappedInterfaces);
//				oos.writeObject(anonClasses);
				oos.writeObject(finalClasses);
				oos.writeObject(finalMethods);
				oos.writeObject(finalFields);
				oos.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			break;
		}
	}

	private static void calculatedWhatsClinitedPerMethod() {
		LinkedList<EqMethodNode> nodesToVisit = new LinkedList<>();
		for (MethodListClassNode cn : instrumentedClasses.values()) {
			for (EqMethodNode mn : cn.methodsHashSet) {
				if ((mn.name.equals("<clinit>") || mn.name.equals("main")) && (mn.access & Opcodes.ACC_STATIC) != 0)
					nodesToVisit.add(mn);
			}
		}
		for (EqMethodNode mn : nodesToVisit) {
			mn.typesToIgnoreInit = new HashSet<>();
		}
		while (!nodesToVisit.isEmpty()) {
			EqMethodNode mn = nodesToVisit.pop();
			for (EqMethodNode child : mn.methodsICall) {
				if (child.typesToIgnoreInit == null) {
					child.typesToIgnoreInit = new HashSet<>();
					child.typesToIgnoreInit.addAll(mn.typesThatIInit);
					child.typesToIgnoreInit.addAll(mn.typesToIgnoreInit);
					nodesToVisit.add(child);
				} else {
					HashSet<String> tmp = new HashSet<>();
					tmp.addAll(mn.typesThatIInit);
					tmp.addAll(mn.typesToIgnoreInit);
					child.typesToIgnoreInit.retainAll(tmp);
				}
			}
		}
	}
	private static boolean getClinitBySuper(MethodListClassNode cn)
	{
		if(cn == null)
			return false;
		if(cn.hasClinit)
		{
			cn.hasClinitOrSuperHasClinit = true;
			cn.superClinitExplored = true;
			return true;
		}
		if(cn.superClinitExplored)
			return cn.hasClinitOrSuperHasClinit;

		if(cn.superName != null && !"java/lang/Object".equals(cn.superName))
		{
			cn.hasClinitOrSuperHasClinit = getClinitBySuper(getClassNodeNOLOAD(cn.superName));
			cn.hasClinit = cn.hasClinitOrSuperHasClinit;
			cn.superClinitExplored = true;
		}
		cn.hasClinitOrSuperHasClinit = cn.hasClinit;
		cn.superClinitExplored = true;
		return cn.hasClinitOrSuperHasClinit;
		
	}
	private static void calculateIgnoredReRunningClinits() {
		for (MethodListClassNode cn : instrumentedClasses.values()) {
			if(!cn.hasClinit && ! cn.superClinitExplored && !ignoredClasses.contains(cn.name))
			{
				cn.hasClinit = getClinitBySuper(cn);
			}
		}
		for (String s : instrumentedClassesBypath.keySet()) {
			MethodListClassNode cn = instrumentedClassesBypath.get(s);
			if(!cn.hasClinit && ! cn.superClinitExplored && !ignoredClasses.contains(cn.name))
			{
				cn.hasClinit = getClinitBySuper(cn);
			}
		}
		for (MethodListClassNode cn : instrumentedClasses.values()) {
			if(ignoredClasses.contains(cn.name))
				continue;
			for(Object o : cn.fields)
			{
				FieldNode fn = (FieldNode) o;
				if((fn.access & Opcodes.ACC_STATIC) == 0)
					continue; //ignore instance fields
				if((fn.access & Opcodes.ACC_FINAL) == 0 && ! fn.name.toUpperCase().equals(fn.name))
					cn.clInitCalculatedNecessary = true; //require all static fields to be final
//				if(fn.desc.startsWith("["))
//					cn.clInitCalculatedNecessary = true; //whenever we have an array, let's crap out and say we need to
				else if(fn.desc.startsWith("L") && !fn.desc.equals("Ljava/lang/String;"))
				{
					MethodListClassNode ownerCN = instrumentedClasses.get(Type.getType(fn.desc).getInternalName());
					if(ownerCN != null)
						cn.clInitCalculatedNecessary = cn.clInitCalculatedNecessary | ownerCN.isMutable;
				}
				if(CLASS_TO_DEBUG != null && cn.name.startsWith(CLASS_TO_DEBUG))
				{
					System.err.println(cn.name + "." + fn.name);
				}
			}
			if(cn != null && cn.name != null && CLASS_TO_DEBUG != null && cn.name.startsWith(CLASS_TO_DEBUG)){
				System.out.println(cn.name+": " + (cn.clInitCalculatedNecessary ? "Y" : "N"));
				System.out.println(cn.clInitInsns);
			}
			if(cn.clInitCalculatedNecessary || cn.clInitInsns == null)
				continue;
			
			boolean calcNecessaryBeforeVMVMIniit = false;
			for (int i = 0; i < cn.clInitInsns.size(); i++) {
				switch(cn.clInitInsns.get(i).getType())
				{
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode fin = (FieldInsnNode) cn.clInitInsns.get(i);
					if(fin.getOpcode() == Opcodes.PUTSTATIC && ! fin.owner.equals(cn.name))
					{
						if(CLASS_TO_DEBUG != null && cn.name.startsWith(CLASS_TO_DEBUG))
						{
							System.err.println(cn.name + "->" + fin.name);
						}
						calcNecessaryBeforeVMVMIniit = true;
						cn.clInitCalculatedNecessary = true;
					}
					else if(fin.getOpcode() == Opcodes.GETSTATIC)
					{
						//we will propogate through this stuff below.
					}
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode min = (MethodInsnNode) cn.clInitInsns.get(i);
//					if(cn.name.endsWith("util/StringUtils"))
//						System.err.println(min.owner + ", " + min.name);
					if(!min.name.startsWith("$VRi") && !(min.name.equals("<init>") && !min.owner.equals(cn.name))&& 
							!min.name.equals("getLogger") && !min.owner.equals("java/lang/String") &&
							!(min.owner.equals("java/util/Arrays") && min.name.equals("fill")))
					{
						cn.clInitCalculatedNecessary = true;
						calcNecessaryBeforeVMVMIniit = true;
					}
					if(calcNecessaryBeforeVMVMIniit && min.owner.equals(Type.getInternalName(VirtualRuntime.class)))
						cn.clInitCalculatedNecessary = false; //might have triggered this in the preamble to register clinit
					if(min.owner.equals(cn.name) && cn.getMethod(min.name, min.desc) != null && (cn.getMethod(min.name, min.desc).access & Opcodes.ACC_NATIVE) != 0)
						cn.clInitCalculatedNecessary = false;
					if(CLASS_TO_DEBUG != null && cn.name.startsWith(CLASS_TO_DEBUG))
					{
						System.err.println(cn.name + "->" + min.owner+"."+min.name);
					}
					break;
				}
			}
		}
		boolean hadChanges = false;
		do{
			hadChanges = false;
		for (MethodListClassNode cn : instrumentedClasses.values()) {
			if(cn.clInitCalculatedNecessary)
				continue;
			if(ignoredClasses.contains(cn.name))
				continue;
			MethodListClassNode sup = instrumentedClasses.get(cn.superName);
			if(sup != null && CLASS_TO_DEBUG != null && cn.name.startsWith(CLASS_TO_DEBUG))
			{
				System.err.println(cn.name + "->" + sup.name+"."+(sup.clInitCalculatedNecessary?"Y":"N"));
			}
			if(sup != null && sup.clInitCalculatedNecessary)
				cn.clInitCalculatedNecessary = true;
			for(Object intf : cn.interfaces)
			{
				sup = instrumentedClasses.get((String) intf);
				if(sup != null && cn.clInitCalculatedNecessary)
					cn.clInitCalculatedNecessary = true;
				if(sup != null && CLASS_TO_DEBUG != null && cn.name.startsWith(CLASS_TO_DEBUG))
				{
					System.err.println(cn.name + "->" + sup.name+"."+(sup.clInitCalculatedNecessary?"Y":"N"));
				}
			}
			if(cn.clInitCalculatedNecessary || cn.clInitInsns == null || ignoredClasses.contains(cn.name))
				continue;

			for (int i = 0; i < cn.clInitInsns.size(); i++) {
				switch(cn.clInitInsns.get(i).getType())
				{
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode min = (MethodInsnNode) cn.clInitInsns.get(i);
					if(min.getOpcode() == Opcodes.INVOKESTATIC && !min.name.startsWith("$VRi") &&
							!min.name.equals("getLogger") && !min.owner.equals("java/lang/String") &&
							!(min.owner.equals("java/util/Arrays") && min.name.equals("fill")))
					{
						MethodListClassNode owner = instrumentedClasses.get(min.owner);
						if(owner!= null && owner.clInitCalculatedNecessary
								&& ! ((owner.getMethod(min.name, min.desc).access & Opcodes.ACC_NATIVE) != 0 &&
								min.owner.equals(cn.name)))
						{
							cn.clInitCalculatedNecessary = true;
							hadChanges = true;
						}
					}
					break;
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode fin = (FieldInsnNode) cn.clInitInsns.get(i);
					if(fin.getOpcode() == Opcodes.GETSTATIC)
					{
						MethodListClassNode owner = instrumentedClasses.get(fin.owner);
						if(owner != null && owner.clInitCalculatedNecessary)
						{
							cn.clInitCalculatedNecessary =true;
							hadChanges = true;
						}
					}
					else if(fin.getOpcode() == Opcodes.PUTSTATIC)
					{
						MethodListClassNode owner = instrumentedClasses.get(fin.owner);
						if(owner != null && owner.clInitCalculatedNecessary)
						{
							cn.clInitCalculatedNecessary =true;
							hadChanges = true;
						}
					}
					break;
				}
			}
		}
		}while(hadChanges);
		for (MethodListClassNode cn : instrumentedClasses.values()) {
//			if(cn.isMutable)
//			{
//				System.out.println(cn.name + " is found mutable");
//			}
			if(cn != null && cn.name != null && CLASS_TO_DEBUG!= null && cn.name.startsWith(CLASS_TO_DEBUG))
			{
				System.err.println(cn.name);
				if(cn.hasClinit)
					System.err.println("has clinit");
				if(cn.clInitCalculatedNecessary)
					System.err.println("clinit necessary");
			}
			if(DEBUG && cn.hasClinit && ! cn.clInitCalculatedNecessary)
			{
				System.out.println("Safely ignoring clinit on  " + cn.name);
			}
			cn.hasClinit = cn.hasClinit && cn.clInitCalculatedNecessary;

		}
	}
	private static String CLASS_TO_DEBUG = "java/lang/Object";//"java/util/regex/Pattern";
	private static void calculateMutableClasses()
	{
		for(MethodListClassNode cn : instrumentedClasses.values())
		{
			if(cn.isMutable)
				continue;
			boolean mutable = false;
			for(EqMethodNode mn : cn.methodsHashSet)
			{
				if(mn.name.equals("<init>"))
					continue;
				if(!mn.sideEffectsExplored)
				{
					mn.hasSideEffects = checkHasSideEffects(mn);
				}
				if(mn.hasSideEffects)
				{
					mutable = true;
					break;
				}
			}
			cn.isMutable = mutable;
		}
	}
	private static boolean checkHasSideEffects(EqMethodNode mn) {
		if(mn.sideEffectsExplored)
			return mn.hasSideEffects;
		mn.sideEffectsExplored = true;
		for(EqMethodNode c : mn.methodsICall)
		{
			if(!c.sideEffectsExplored)
			{
				c.hasSideEffects = checkHasSideEffects(c);
			}
			if(c.hasSideEffects)
			{
				mn.hasSideEffects = true;
				return true;
			}
		}
		return false;
	}
	public static String remapInterface(String intfc) {
		if(intfc.contains("JSSEFactory") && remappedInterfaces.contains(intfc))
			System.out.println(intfc);
		if (remappedInterfaces.contains(intfc)) {
			return intfc+"$vmvm";
		}
		return intfc;
	}

	public static MethodListClassNode getClassNode(String clazz) {
		MethodListClassNode cn = null;
		if(clazz.endsWith("DetectorFactoryCollection"))
			System.err.println("DetectorFactoryCollection path is " + curPath);
		if(curPath != null)
			cn = instrumentedClassesBypath.get(curPath + clazz);
		if(cn == null && clazz.endsWith("DetectorFactoryCollection"))
			System.err.println("DetectorFactoryCollection not in " + curPath + " registry");
		if(cn == null)
			cn = instrumentedClasses.get(clazz);
		if (cn == null) {
			cn = getJREClassInfo(clazz);
			if (cn == null)
				return null;
		}
		return cn;
	}
	public static MethodListClassNode getClassNodeNOLOAD(String clazz) {
		MethodListClassNode cn = null;
		if(curPath != null)
			cn = instrumentedClassesBypath.get(curPath + clazz);
		if(cn == null && clazz.endsWith("DetectorFactoryCollection"))
			System.err.println("DetectorFactoryCollection not in " + curPath + " registry");
		if(cn == null)
			cn = instrumentedClasses.get(clazz);
		return cn;
	}
	
	public static MethodNode getMethodNode(ClassNode thisClassInfo, String name2, String desc2) {
		for (Object o : thisClassInfo.methods) {
			MethodNode mn = (MethodNode) o;
			if (mn.name.equals(name2) && mn.desc.equals(desc2))
				return mn;
		}
		return null;
	}

	public static boolean atLeastASuperEq(String classToCheck, String whatWeWant, int n) {
		ClassNode cn = Instrumenter.getClassNode(classToCheck);
		if (cn == null || cn.superName == null)
			return false;
		if (cn.superName.equals(whatWeWant))
			return true;
		else if (n < 20)
			return atLeastASuperEq(cn.superName, whatWeWant, n + 1);
		return false;
	}
	public static EqMethodNode getMethodNodeInstOnly(String clazz, String name, String desc) {
		MethodListClassNode cn = instrumentedClasses.get(clazz);
		if (cn == null) {
				return null;
		}
		for (EqMethodNode mn : cn.methodsHashSet) {
			if (mn.name.equals(name) && mn.desc.equals(desc))
				return mn;
		}
		return null;
	}

	public static EqMethodNode getMethodNode(String clazz, String name, String desc) {
		MethodListClassNode cn = instrumentedClasses.get(clazz);
		if (cn == null) {
			cn = getJREClassInfo(clazz);
			if (cn == null)
				return null;
		}
		for (EqMethodNode mn : cn.methodsHashSet) {
			if (mn.name.equals(name) && mn.desc.equals(desc))
				return mn;
		}
		return null;
	}
	private static String curPath = null;


	public static class InstrumentResult
	{
		public byte[] clazz;
		public byte[] intfc;
		public byte[] intfcReseter;
	}
	static final boolean FORCE_UPGRADE_CLASS = false;
	
	public static InstrumentResult instrumentClass(String path, InputStream is, boolean renameInterfaces) {
		try {
			InstrumentResult out = new InstrumentResult();
			curPath = path;

			ClassReader cr = new ClassReader(is);
			//TODO why did we need to add the serial version uid??
//			{
//				ClassWriter cw = new ClassWriter(cr, 0);
//				SerialVersionUIDAdder uidAdder = new SerialVersionUIDAdder(cw);
//				cr.accept(uidAdder, ClassReader.SKIP_FRAMES);
//				byte[] b = cw.toByteArray();
//				cr = new ClassReader(b);
//			}

			ClassNode cn = new ClassNode();
			cr.accept(cn, ClassReader.SKIP_FRAMES);

			if(FORCE_UPGRADE_CLASS && (cn.version < 50 || cn.version > 1000))
			{
				ClassWriter cw = new InstrumenterClassWriter(cr, ClassWriter.COMPUTE_FRAMES, loader);
				cr.accept(new ClassVisitor(Opcodes.ASM5,cw) {
					@Override
					public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
						version = 50;
						if((access & Opcodes.ACC_INTERFACE) != 0)
							access |= Opcodes.ACC_ABSTRACT;
						super.visit(version, access, name, signature, superName, interfaces);
					}
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
					}
				}, ClassReader.EXPAND_FRAMES);
				cr = new ClassReader(cw.toByteArray());
				cn = new ClassNode();
				cr.accept(cn, ClassReader.SKIP_FRAMES);
			}
			ClassWriter cw = new InstrumenterClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);

			VMVMClassVisitor cv = cvFactory.getVisitor(cw, cn, true);

			cr.accept(cv, ClassReader.SKIP_FRAMES);

			lastInstrumentedClass = cv.getClassName();
			out.clazz = cw.toByteArray();
			
			if((cn.access & Opcodes.ACC_INTERFACE) != 0)
			{
				out.intfcReseter = generateInterfaceCLinits(instrumentedClasses.get(cn.name));
			}
			if (cv.hasExtraInterface() && renameInterfaces) {
				out.intfc = cv.getExtraInterface();
			}
			try {
				ClassReader cr2 = new ClassReader(out.clazz);
				cr2.accept(new CheckClassAdapter(new ClassWriter(0)), ClassReader.SKIP_FRAMES);
			} catch (Exception ex) {
				System.out.println(lastInstrumentedClass);
				ex.printStackTrace();
			}
			
			if (DEBUG) {
				File debugDir = new File("debug");
				if (!debugDir.exists())
					debugDir.mkdir();
				File f = new File("debug/" + cn.name.replace("/", ".") + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(cw.toByteArray());
				fos.close();
			}
			curPath = null;
			return out;
		} catch (Exception ex) {
			curPath = null;
			logger.log(Level.SEVERE, "Exception processing class: " + lastInstrumentedClass, ex);
			ex.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) {
		_main(args);
		System.out.println("Completed VMVM instrumentation");
	}

	public static void _main(String[] args) {

		String outputFolder = args[1];
		rootOutputDir = new File(outputFolder);
		if (!rootOutputDir.exists())
			rootOutputDir.mkdir();
		String inputFolder = args[0];
		// Setup the class loader
		URL[] urls = new URL[args.length - 2];
		for (int i = 2; i < args.length; i++) {
			File f = new File(args[i]);
			if (!f.exists()) {
				System.err.println("Unable to read path " + args[i]);
				System.exit(-1);
			}
			if (f.isDirectory() && !f.getAbsolutePath().endsWith("/"))
				f = new File(f.getAbsolutePath() + "/");
			try {
				urls[i - 2] = f.getCanonicalFile().toURI().toURL();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		loader = new URLClassLoader(urls, Instrumenter.class.getClassLoader());

		for (pass_number = 0; pass_number < NUM_PASSES; pass_number++) // Do
																		// each
																		// pass.
		{
			File f = new File(inputFolder);
			if (!f.exists()) {
				System.err.println("Unable to read path " + inputFolder);
				System.exit(-1);
			}
			if (f.isDirectory())
				processDirectory(f, rootOutputDir, true);
			else if (inputFolder.endsWith(".jar"))
				//				try {
				//					FileOutputStream fos =  new FileOutputStream(rootOutputDir.getPath() + File.separator + f.getName());
				processJar(f, rootOutputDir);
			//				} catch (FileNotFoundException e1) {
			//					// TODO Auto-generated catch block
			//					e1.printStackTrace();
			//				}
			else if (inputFolder.endsWith(".class"))
				try {
					processClass(f.getName(), new FileInputStream(f), rootOutputDir);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else if (inputFolder.endsWith(".zip")) {
				processZip(f, rootOutputDir);
			} else {
				System.err.println("Unknown type for path " + inputFolder);
				System.exit(-1);
			}
			finishedPass();
		}
//		generateInterfaceCLinits(rootOutputDir);
		// }

	}

	private static byte[] generateInterfaceCLinits(MethodListClassNode cn)
	{
		if(cn != null && (cn.access & Opcodes.ACC_INTERFACE) != 0)
		{
			//Need to generate the clinit wrap

			ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//			JUnitResettingClassVisitor cv = new JUnitResettingClassVisitor(cw, cn, false);
			cv.visit(51, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, cn.name+"$vmvmReseter", null, "java/lang/Object", null);
			cv.visitSource(null, null);
			cv.visitAnnotation(Type.getDescriptor(VMVMInstrumented.class), false);


			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "vmvm_reinit_statics", "()V", null, null);
			StaticFinalMutibleizer gmv = new StaticFinalMutibleizer(mv, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, cn.name, "vmvm_reinit_statics", "()V");
			gmv.visitCode();
			gmv.visitInsn(Opcodes.ICONST_0);
			gmv.visitFieldInsn(Opcodes.PUTSTATIC, cn.name+"$vmvmReseter", Constants.VMVM_NEEDS_RESET, "Z");
			
			gmv.visitLdcInsn(Type.getType("L" + cn.name+"$vmvmReseter"+ ";"));
			gmv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "registerClInit", "(Ljava/lang/Class;)V",false);
			
			if(StaticFieldIsolatorMV.CLINIT_ORDER_DEBUG)
			{

			gmv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
			gmv.visitLdcInsn("clinit  rerunning>" + cn.name+"$vmvmReseter");
			gmv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V",false);
			}

			
			//				gmv.visitCode();
			if (cn.clInitInsns != null) {
				MethodNode mn2 = new MethodNode();
				mn2.maxLocals = 0;
				mn2.maxStack = 0;
				mn2.instructions = cn.clInitInsns;
				mn2.instructions.resetLabels();
				mn2.accept(gmv);
//				gmv.returnValue();
//				gmv.visitMaxs(0, 0);
				gmv.visitEnd();
			} else {
				gmv.returnValue();
				gmv.visitMaxs(0, 0);
				gmv.visitEnd();
			}
			
		     {
		         mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		         mv.visitVarInsn(Opcodes.ALOAD, 0);
		         mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
		                 "java/lang/Object",
		                 "<init>",
		                 "()V", false);
		         mv.visitInsn(Opcodes.RETURN);
		         mv.visitMaxs(1, 1);
		         mv.visitEnd();
		     }
				cv.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_NEEDS_RESET, "Z", null, true);
				cv.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null,null);
			cv.visitEnd();
			
			return cv.toByteArray();
		}
		return null;
	}
	
	public static MethodListClassNode getJREClassInfo(String className) {
		if (!unInstrumentedClasses.containsKey(className)) {
			try {
				//				System.out.println(className);
				analyzeClass(null,new ClassReader(className), unInstrumentedClasses);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//				e.printStackTrace();
			}
		}
		return unInstrumentedClasses.get(className);

	}
	private static HashSet<EqFieldInsnNode> getFieldsEffectedBy(String className, String methodName, String methodDesc, HashSet<EqMethodNode> explored)
	{
		HashSet<EqFieldInsnNode> ret = new HashSet<>();
		EqMethodNode mn = getMethodNodeInstOnly(className, methodName, methodDesc);
		if(mn != null && ! explored.contains(mn) && !className.startsWith("java/"))
		{
			explored.add(mn);
			for(int i = 0; i < mn.instructions.size(); i++)
			{
				Object o = mn.instructions.get(i);
				if(o instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode)o;
					if(instrumentedClasses.containsKey(fin.owner))
						ret.add(new EqFieldInsnNode(0, fin.owner, fin.name, fin.desc));
				}
				else if (o instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) o;
					ret.addAll(getFieldsEffectedBy(min.owner, min.name, min.desc,explored));
				}
			}
		}
		return ret;

	}
	public static HashSet<EqFieldInsnNode> getFieldsEffectedBy(String className, String methodName, String methodDesc)
	{
		return getFieldsEffectedBy(className, methodName, methodDesc, new HashSet<EqMethodNode>());
	}
	private static HashMap<String, EqMethodNode> methods = new HashMap<>();

	public static int MAX_CLASSES = -1;
	public static int classesInstrumented = 0;
	private static EqMethodNode getMethodNodeInternal(String owner, String name, String desc)
	{
		EqMethodNode mn = methods.get(owner+"."+name+desc);
		if(mn == null)
		{
			mn = new EqMethodNode(0, name, desc, owner, null, null);
			methods.put(owner+"."+name+desc, mn);
		}
		return mn;
	}
	public static void analyzeClass(String path, ClassReader cr, HashMap<String, MethodListClassNode> cacheMap) {
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		MethodListClassNode cn2 = new MethodListClassNode();
		cn2.methodsHashSet = new HashSet<>();
//		if(cn.name.endsWith("DetectorFactoryCollection"))
//			System.err.println("Visiting class: " + cn.name);
		
		
		instrumentedClassesBypath.put(path+cn.name, cn2);
		
		if (cacheMap.containsKey(cn.name))
		{
			cn2.isMutable = cacheMap.get(cn.name).isMutable;
		}

		cacheMap.put(cn.name, cn2);
		cn2.superName = cn.superName;
		cn2.interfaces = cn.interfaces;
		cn2.name = cn.name;
		cn2.access = cn.access;

		cn2.fields = cn.fields;

		if (cn.superName != null && !cn.superName.equals("java/lang/Object") && cacheMap != unInstrumentedClasses) {
			if (!superToSubClass.containsKey(cn.superName))
				superToSubClass.put(cn.superName, new HashSet<String>());
			superToSubClass.get(cn.superName).add(cn.name);
		}
		if (cn.interfaces != null && cacheMap != unInstrumentedClasses) {
			for (int i = 0; i < cn.interfaces.size(); i++) {
				String interfc = (String) cn.interfaces.get(i);
				if (!superToSubClass.containsKey(interfc))
					superToSubClass.put(interfc, new HashSet<String>());
				superToSubClass.get(interfc).add(cn.name);
			}
		}
		IntializedTypeAnalyzer a = new IntializedTypeAnalyzer();

		for (Object o : cn.methods) {
			MethodNode m = (MethodNode) o;
			EqMethodNode mn;
			if(methods.containsKey(cn.name+"."+m.name+m.desc))
			{
				mn = methods.get(cn.name+"."+m.name+m.desc);
				mn.access = m.access;
				mn.name = m.name;
				mn.desc = m.desc;
				mn.owner = cn.name;
				mn.signature = m.signature;
			}
			else
			{
				mn = new EqMethodNode(m.access, m.name, m.desc, cn.name, m.signature, null);
				methods.put(cn.name+"."+m.name+m.desc,mn);
			}
			if(m.name.equals("<clinit>"))
			{
				cn2.hasClinit = true;
//				cn2.clInit = mn;
			}
			
			try {
				Frame[] frames = a.analyze(cn.name, m);
				if (frames != null && frames.length > 0) {
//					System.out.println(mn);

					mn.typesThatIInit = a.getTypesDefinitelyInitedStartingWith(frames[0]);
//					System.out.println(mn.typesThatIInit);
					int i = -1;
					for(Frame f : frames)
					{
						if(f != null && ((Node)f).isUnnecessary)
							mn.ignoredInitCalls.add(i);
//						if(f!=null)
//						System.out.println((((Node) f).isUnnecessary ? "IGNORE\t" : "\t") + i + f);
						i++;
					}
				}
			} catch (AnalyzerException e) {
				e.printStackTrace();
			}
			
			
			
			mn.localVariables = m.localVariables;
			mn.invisibleAnnotations = m.invisibleAnnotations;
			mn.visibleAnnotations = m.visibleAnnotations;
			cn2.methodsHashSet.add(mn);
			mn.instructions = new InsnList();
			Iterator<?> iter = m.instructions.iterator();
			while (iter.hasNext()) {
				AbstractInsnNode in = (AbstractInsnNode) iter.next();
				if (in.getOpcode() == Opcodes.GETSTATIC || in.getOpcode() == Opcodes.PUTSTATIC) {
					FieldInsnNode fn = (FieldInsnNode) in;
					mn.instructions.add(new FieldInsnNode(fn.getOpcode(), fn.owner, fn.name, fn.desc));
				} else if (in.getType() == AbstractInsnNode.METHOD_INSN) {
					MethodInsnNode min = (MethodInsnNode) in;
					mn.instructions.add(new MethodInsnNode(min.getOpcode(), min.owner, min.name, min.desc,min.itf));
					//Add to the callgraph

					if(methods.containsKey(min.owner+"."+min.name+min.desc))
					{
						mn.methodsICall.add(methods.get(min.owner+"."+min.name+min.desc));
						methods.get(min.owner+"."+min.name+min.desc).methodsThatCallMe.add(mn);
					}
					else
					{
						EqMethodNode mnICall = new EqMethodNode(0, min.name, min.desc,cn2.name, null, null);
						methods.put(min.owner+"."+min.name+min.desc,mnICall);
						mn.methodsICall.add(mnICall);
						mnICall.methodsThatCallMe.add(mn);
					}
				}
				else if(in.getOpcode() == Opcodes.PUTFIELD)
				{
					//This method has side effects.
					FieldInsnNode fn = (FieldInsnNode) in;
					if (mn.name.equals("<init>") && cn2.name.equals(fn.owner)) {
						//it's fine to have putfield in the constructor
					} else {
						MethodListClassNode ownerCN = cacheMap.get(fn.owner);
						if (ownerCN == null) {
							ownerCN = new MethodListClassNode();
							cacheMap.put(fn.owner, ownerCN);
						}
						ownerCN.isMutable = true;
					}
				}
			}
			if(m.name.equals("<clinit>"))//XXX why was this filteirng this? && ((cn.access & Opcodes.ACC_INTERFACE) != 0) || (cn.access & Opcodes.ACC_ENUM) != 0)
			{
				cn2.clInitInsns= m.instructions;
			}
		}
		if((cn.access & Opcodes.ACC_INTERFACE) != 0 && !cn.name.endsWith("/package-info"))
		{
			if(!InterceptingClassVisitor.shouldIgnoreClass(cn.name))
				remappedInterfaces.add(cn.name);
			
			cn2 = new MethodListClassNode();
			cn2.methodsHashSet = new HashSet<>();
			cn2.name = getInstrumentedInterfaceName(cn.name);
			if (cacheMap.containsKey(cn2.name))
				cn2 = cacheMap.get(cn2.name);
			else
				cacheMap.put(cn2.name, cn2);
			cn2.superName = cn.name;
			cn2.interfaces = cn.interfaces;
			cn2.name = getInstrumentedInterfaceName(cn.name);
			cn2.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE;
			
			if (cn2.superName != null && !cn2.superName.equals("java/lang/Object") && cacheMap != unInstrumentedClasses) {
				if (!superToSubClass.containsKey(cn.superName))
					superToSubClass.put(cn2.superName, new HashSet<String>());
				superToSubClass.get(cn2.superName).add(cn2.name);
			}
			if (cn2.interfaces != null && cacheMap != unInstrumentedClasses) {
				for (int i = 0; i < cn.interfaces.size(); i++) {
					String interfc = (String) cn2.interfaces.get(i);
					if (!superToSubClass.containsKey(interfc))
						superToSubClass.put(interfc, new HashSet<String>());
					superToSubClass.get(interfc).add(cn2.name);
				}
			}

			for (Object o : cn.methods) {
				MethodNode m = (MethodNode) o;
				Type[] argTypes = Type.getArgumentTypes(m.desc);
				Type[] nTypes = new Type[argTypes.length+1];
				nTypes[argTypes.length] = Type.getType(VMState.class);
				System.arraycopy(argTypes, 0, nTypes, 0, argTypes.length);
				EqMethodNode mn = new EqMethodNode(m.access, "_"+m.name, Type.getMethodDescriptor(Type.getReturnType(m.desc), nTypes),cn.name, m.signature, null);
				mn.localVariables = m.localVariables;
				cn2.methodsHashSet.add(mn);
				mn.instructions = new InsnList();
				Iterator<?> iter = m.instructions.iterator();
				while (iter.hasNext()) {
					AbstractInsnNode in = (AbstractInsnNode) iter.next();
					if (in.getOpcode() == Opcodes.GETSTATIC || in.getOpcode() == Opcodes.PUTSTATIC) {
						FieldInsnNode fn = (FieldInsnNode) in;
						mn.instructions.add(new FieldInsnNode(fn.getOpcode(), fn.owner, fn.name, fn.desc));
					} else if (in.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) in;
						mn.instructions.add(new MethodInsnNode(min.getOpcode(), min.owner, min.name, min.desc, false));
					}
				}
				if(m.name.equals("<clinit>") && (cn.access & Opcodes.ACC_INTERFACE) != 0)
				{
					cn2.clInitInsns= m.instructions;
				}
			}
		}
	}
	public static String getReseterName(String oldName) {
		if (oldName.endsWith(".class"))
			return oldName.replace(".class", "$vmvmReseter.class");
		else
			return oldName + "$vmvmReseter";
	}
	public static String getInstrumentedInterfaceName(String oldName) {
		if (oldName.endsWith(".class"))
			return oldName.replace(".class", "$vmvm.class");
		else
			return oldName + "$vmvm";
	}

	private static void processClass(String name, InputStream is, File outputDir) {
		switch (pass_number) {
		case PASS_ANALYZE:
			try {
				analyzeClass(outputDir.getAbsolutePath(),new ClassReader(is), instrumentedClasses);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(ArrayIndexOutOfBoundsException ex)
			{
				
			}
			break;
		case PASS_OUTPUT:
			try {
				FileOutputStream fos = new FileOutputStream(outputDir.getPath() + File.separator + name);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				lastInstrumentedClass = outputDir.getPath() + File.separator + name;

				InstrumentResult c = instrumentClass(outputDir.getAbsolutePath(),is, true);
				bos.write(c.clazz);
				bos.writeTo(fos);
				fos.close();
				if (c.intfc != null) {
					fos = new FileOutputStream(outputDir.getPath() + File.separator + getInstrumentedInterfaceName(name));
					bos = new ByteArrayOutputStream();
					lastInstrumentedClass = outputDir.getPath() + File.separator + getInstrumentedInterfaceName(name);
					bos.write(c.intfc);
					bos.writeTo(fos);
					fos.close();
				}
				if(c.intfcReseter != null)
				{
					fos = new FileOutputStream(outputDir.getPath() + File.separator + getReseterName(name));
					bos = new ByteArrayOutputStream();
					bos.write(c.intfcReseter);
					bos.writeTo(fos);
					fos.close();
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static void processDirectory(File f, File parentOutputDir, boolean isFirstLevel) {
		if(f.getName().equals(".AppleDouble"))
			return;
		File thisOutputDir;
		if (isFirstLevel) {
			thisOutputDir = parentOutputDir;
		} else {
			thisOutputDir = new File(parentOutputDir.getAbsolutePath() + File.separator + f.getName());
			if (pass_number == PASS_OUTPUT)
				thisOutputDir.mkdir();
		}
		for (File fi : f.listFiles()) {
			if (fi.isDirectory())
				processDirectory(fi, thisOutputDir, false);
			else if (fi.getName().endsWith(".class") && !fi.getName().endsWith("package-info.class"))
				try {
					FileInputStream fis = new FileInputStream(fi);
					processClass(fi.getName(), fis , thisOutputDir);
					fis.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			else if (fi.getName().endsWith(".jar"))
				//				try {
				//					FileOutputStream fos = new FileOutputStream(thisOutputDir.getPath() + File.separator + f.getName());
				processJar(fi, thisOutputDir);
			//					fos.close();
			//				} catch (IOException e1) {
			// TODO Auto-generated catch block
			//					e1.printStackTrace();
			//				}
			else if (fi.getName().endsWith(".zip"))
				processZip(fi, thisOutputDir);
			else if (pass_number == PASS_OUTPUT) {
				File dest = new File(thisOutputDir.getPath() + File.separator + fi.getName());
				FileChannel source = null;
				FileChannel destination = null;

				try {
					source = new FileInputStream(fi).getChannel();
					destination = new FileOutputStream(dest).getChannel();
					destination.transferFrom(source, 0, source.size());
				} catch (Exception ex) {
					logger.log(Level.SEVERE, "Unable to copy file " + fi, ex);
//					System.exit(-1);
				} finally {
					if (source != null) {
						try {
							source.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (destination != null) {
						try {
							destination.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
		}

	}

	//	private static void processJar(InputStream inputStream, OutputStream os) {
	//		try {
	//			File f = new File("/tmp/classfile");
	//			if (f.exists())
	//				f.delete();
	//			FileOutputStream fos = new FileOutputStream(f);
	//			byte buf[] = new byte[1024];
	//			int len;
	//			while ((len = inputStream.read(buf)) > 0) {
	//				fos.write(buf, 0, len);
	//			}
	//			fos.close();
	//			inputStream.close();
	//
	//			JarFile jar = new JarFile(f);
	//			JarOutputStream jos = null;
	//			if (pass_number == PASS_OUTPUT)
	//				jos = new JarOutputStream(os);
	//			//				jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
	//			Enumeration<JarEntry> entries = jar.entries();
	//			while (entries.hasMoreElements()) {
	//				JarEntry e = entries.nextElement();
	//				switch (pass_number) {
	//				case PASS_ANALYZE:
	//					if (e.getName().endsWith(".class")) {
	//						analyzeClass(new ClassReader(jar.getInputStream(e)), instrumentedClasses);
	//					}
	//					break;
	//				case PASS_OUTPUT:
	//					if (e.getName().endsWith(".class") && !e.getName().startsWith("java") && !e.getName().startsWith("org/objenesis")
	//							&& !e.getName().startsWith("com/thoughtworks/xstream/") && !e.getName().startsWith("com/rits/cloning")
	//							&& !e.getName().startsWith("com/apple/java/Application")) {
	//						{
	//							JarEntry outEntry = new JarEntry(e.getName());
	//							jos.putNextEntry(outEntry);
	//							byte[] clazz = instrumentClass(jar.getInputStream(e));
	//							if (clazz == null) {
	//								InputStream is = jar.getInputStream(e);
	//								byte[] buffer = new byte[1024];
	//								while (true) {
	//									int count = is.read(buffer);
	//									if (count == -1)
	//										break;
	//									jos.write(buffer, 0, count);
	//								}
	//							} else
	//								jos.write(clazz);
	//							jos.closeEntry();
	//						}
	//
	//					} else {
	//						JarEntry outEntry = new JarEntry(e.getName());
	//						if (e.isDirectory()) {
	//							jos.putNextEntry(outEntry);
	//							jos.closeEntry();
	//						} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
	//							// don't copy this
	//						} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
	//							Scanner s = new Scanner(jar.getInputStream(e));
	//							jos.putNextEntry(outEntry);
	//
	//							String curPair = "";
	//							while (s.hasNextLine()) {
	//								String line = s.nextLine();
	//								if (line.equals("")) {
	//									curPair += "\n";
	//									if (!curPair.contains("SHA1-Digest:"))
	//										jos.write(curPair.getBytes());
	//									curPair = "";
	//								} else {
	//									curPair += line + "\n";
	//								}
	//							}
	//							s.close();
	//							jos.write("\n".getBytes());
	//							jos.closeEntry();
	//						} else {
	//							jos.putNextEntry(outEntry);
	//							InputStream is = jar.getInputStream(e);
	//							byte[] buffer = new byte[1024];
	//							while (true) {
	//								int count = is.read(buffer);
	//								if (count == -1)
	//									break;
	//								jos.write(buffer, 0, count);
	//							}
	//							jos.closeEntry();
	//						}
	//					}
	//				}
	//
	//			}
	//			//			if (pass_number == PASS_OUTPUT) {
	//			//				jos.close();
	//			//			}
	//			jar.close();
	//		} catch (Exception e) {
	//			// TODO Auto-generated catch block
	//			logger.error("Unable to process jar", e);
	//			System.exit(-1);
	//		}
	//
	//	}
	//
	//	private static void processZip(File f, File outputDir) {
	//		try {
	//			ZipFile jar = new ZipFile(f);
	//			ZipOutputStream jos = null;
	//			if (pass_number == PASS_OUTPUT)
	//				jos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
	//			Enumeration<? extends ZipEntry> entries = jar.entries();
	//			while (entries.hasMoreElements()) {
	//				ZipEntry e = entries.nextElement();
	//				switch (pass_number) {
	//				case PASS_ANALYZE:
	//					if (e.getName().endsWith(".class")) {
	//						analyzeClass(new ClassReader(jar.getInputStream(e)), instrumentedClasses);
	//					}
	//					break;
	//				case PASS_OUTPUT:
	//					if (e.getName().endsWith(".class") && !e.getName().startsWith("java") && !e.getName().startsWith("org/objenesis")
	//							&& !e.getName().startsWith("com/thoughtworks/xstream/") && !e.getName().startsWith("com/rits/cloning")
	//							&& !e.getName().startsWith("com/apple/java/Application")) {
	//						{
	//							JarEntry outEntry = new JarEntry(e.getName());
	//							jos.putNextEntry(outEntry);
	//							byte[] clazz = instrumentClass(jar.getInputStream(e));
	//							if (clazz == null) {
	//								InputStream is = jar.getInputStream(e);
	//								byte[] buffer = new byte[1024];
	//								while (true) {
	//									int count = is.read(buffer);
	//									if (count == -1)
	//										break;
	//									jos.write(buffer, 0, count);
	//								}
	//							} else
	//								jos.write(clazz);
	//							jos.closeEntry();
	//						}
	//
	//					} else if (e.getName().endsWith(".jar")) {
	//						ZipEntry outEntry = new ZipEntry(e.getName());
	//						jos.putNextEntry(outEntry);
	//						try {
	//							processJar(jar.getInputStream(e), jos);
	//							jos.flush();
	//							jos.closeEntry();
	//						} catch (FileNotFoundException e1) {
	//							// TODO Auto-generated catch block
	//							e1.printStackTrace();
	//						}
	//					} else {
	//						ZipEntry outEntry = new ZipEntry(e.getName());
	//						if (e.isDirectory()) {
	//							jos.putNextEntry(outEntry);
	//							jos.closeEntry();
	//						} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
	//							// don't copy this
	//						} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
	//							Scanner s = new Scanner(jar.getInputStream(e));
	//							jos.putNextEntry(outEntry);
	//
	//							String curPair = "";
	//							while (s.hasNextLine()) {
	//								String line = s.nextLine();
	//								if (line.equals("")) {
	//									curPair += "\n";
	//									if (!curPair.contains("SHA1-Digest:"))
	//										jos.write(curPair.getBytes());
	//									curPair = "";
	//								} else {
	//									curPair += line + "\n";
	//								}
	//							}
	//							s.close();
	//							jos.write("\n".getBytes());
	//							jos.closeEntry();
	//						} else {
	//							jos.putNextEntry(outEntry);
	//							InputStream is = jar.getInputStream(e);
	//							byte[] buffer = new byte[1024];
	//							while (true) {
	//								int count = is.read(buffer);
	//								if (count == -1)
	//									break;
	//								jos.write(buffer, 0, count);
	//							}
	//							jos.closeEntry();
	//						}
	//					}
	//				}
	//
	//			}
	//			if (pass_number == PASS_OUTPUT) {
	//				jos.close();
	//			}
	//			jar.close();
	//		} catch (Exception e) {
	//			// TODO Auto-generated catch block
	//			logger.error("Unable to process zip" + f, e);
	//			System.exit(-1);
	//		}
	//
	//	}
	public static void processJar(File f, File outputDir) {
		try {
			//			@SuppressWarnings("resource")
			//			System.out.println("File: " + f.getName());
			JarFile jar = new JarFile(f);
			JarOutputStream jos = null;
			if (pass_number == PASS_OUTPUT)
				//				jos = new JarOutputStream(os);
				jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				switch (pass_number) {
				case PASS_ANALYZE:
					if (e.getName().endsWith(".class") && ! e.getName().endsWith("looks.like.a.class")) {
						classesInstrumented++;
						if(MAX_CLASSES > 0 && classesInstrumented >= MAX_CLASSES)
							return;
						analyzeClass(f.getAbsolutePath(),new ClassReader(jar.getInputStream(e)), instrumentedClasses);
					}
					break;
				case PASS_OUTPUT:
					if (e.getName().endsWith(".class") && !e.getName().startsWith("org/xml/sax") && ! e.getName().endsWith("package-info.class") && ! e.getName().endsWith("looks.like.a.class")) {
						{
							try{
							JarEntry outEntry = new JarEntry(e.getName());
							jos.putNextEntry(outEntry);
							InputStream _is = jar.getInputStream(e);
							InstrumentResult clazz = instrumentClass(f.getAbsolutePath(),_is, true);
							_is.close();
							if (clazz == null || clazz.clazz == null) {
								System.out.println("Failed to instrument " + e.getName() + " in " + f.getName());
								InputStream is = jar.getInputStream(e);
								byte[] buffer = new byte[1024];
								while (true) {
									int count = is.read(buffer);
									if (count == -1)
										break;
									jos.write(buffer, 0, count);
								}
								is.close();
							} else
							{
								jos.write(clazz.clazz);
							}
							jos.closeEntry();
							if(clazz != null && clazz.intfcReseter != null)
							{
								outEntry = new JarEntry(getReseterName(e.getName()));
								jos.putNextEntry(outEntry);
								jos.write(clazz.intfcReseter);
								jos.closeEntry();
							}
							if (clazz != null && clazz.intfc != null) {
								outEntry = new JarEntry(getInstrumentedInterfaceName(e.getName()));
								jos.putNextEntry(outEntry);
								jos.write(clazz.intfc);
								jos.closeEntry();
							}
							}
							catch(ZipException ex)
							{
								ex.printStackTrace();
								continue;
							}

						}

					} else {
						JarEntry outEntry = new JarEntry(e.getName());
						if (e.isDirectory()) {
							jos.putNextEntry(outEntry);
							jos.closeEntry();
						} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
							// don't copy this
						} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
							Scanner s = new Scanner(jar.getInputStream(e));
							jos.putNextEntry(outEntry);

							String curPair = "";
							while (s.hasNextLine()) {
								String line = s.nextLine();
								if (line.equals("")) {
									curPair += "\n";
									if (!curPair.contains("SHA1-Digest:"))
										jos.write(curPair.getBytes());
									curPair = "";
								} else {
									curPair += line + "\n";
								}
							}
							s.close();
							jos.write("\n".getBytes());
							jos.closeEntry();
						} else {
							jos.putNextEntry(outEntry);
							InputStream is = jar.getInputStream(e);
							byte[] buffer = new byte[1024];
							while (true) {
								int count = is.read(buffer);
								if (count == -1)
									break;
								jos.write(buffer, 0, count);
							}
							jos.closeEntry();
						}
					}
				}

			}
			if (jos != null) {
				jos.close();

			}
			jar.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.log(Level.SEVERE, "Unable to process jar" + f.getAbsolutePath(), e);
			File dest = new File(outputDir.getPath() + File.separator + f.getName());
			FileChannel source = null;
			FileChannel destination = null;

			try {
				source = new FileInputStream(f).getChannel();
				dest.getParentFile().mkdirs();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "Unable to copy file " + f, ex);
//				System.exit(-1);
			} finally {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
				if (destination != null) {
					try {
						destination.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			}
//			System.exit(-1);
		}

	}

	private static void processZip(File f, File outputDir) {
		try {
			//			@SuppressWarnings("resource")
			ZipFile zip = new ZipFile(f);
			ZipOutputStream zos = null;
			if (pass_number == PASS_OUTPUT)
				zos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();
				switch (pass_number) {
				case PASS_ANALYZE:
					if (e.getName().endsWith(".class")) {
						analyzeClass(f.getAbsolutePath(),new ClassReader(zip.getInputStream(e)), instrumentedClasses);
					} else if (e.getName().endsWith(".jar")) {
						File tmp = new File("/tmp/classfile");
						if (tmp.exists())
							tmp.delete();
						FileOutputStream fos = new FileOutputStream(tmp);
						byte buf[] = new byte[1024];
						int len;
						InputStream is = zip.getInputStream(e);
						while ((len = is.read(buf)) > 0) {
							fos.write(buf, 0, len);
						}
						is.close();
						fos.close();

						processJar(tmp, new File("/tmp"));
						//						processJar(jar.getInputStream(e), jos);
					}
					break;
				case PASS_OUTPUT:
					if (e.getName().endsWith(".class")) {
						{
							ZipEntry outEntry = new ZipEntry(e.getName());
							zos.putNextEntry(outEntry);

							InputStream _is=zip.getInputStream(e);
							InstrumentResult clazz = instrumentClass(f.getAbsolutePath(),_is, true);
							_is.close();
							if (clazz == null || clazz.clazz != null) {
								InputStream is = zip.getInputStream(e);
								byte[] buffer = new byte[1024];
								while (true) {
									int count = is.read(buffer);
									if (count == -1)
										break;
									zos.write(buffer, 0, count);
								}
								is.close();
							} else
								zos.write(clazz.clazz);
							zos.closeEntry();

							if (clazz != null && clazz.intfc != null) {
								outEntry = new ZipEntry(getInstrumentedInterfaceName(e.getName()));
								zos.putNextEntry(outEntry);
								zos.write(clazz.intfc);
								zos.closeEntry();
							}
							if(clazz != null && clazz.intfcReseter != null)
							{
								outEntry = new ZipEntry(getReseterName(e.getName()));
								zos.putNextEntry(outEntry);
								zos.write(clazz.intfcReseter);
								zos.closeEntry();
							}
						}

					} else if (e.getName().endsWith(".jar")) {
						ZipEntry outEntry = new ZipEntry(e.getName());
						//						jos.putNextEntry(outEntry);
						//						try {
						//							processJar(jar.getInputStream(e), jos);
						//							jos.closeEntry();
						//						} catch (FileNotFoundException e1) {
						//							// TODO Auto-generated catch block
						//							e1.printStackTrace();
						//						}

						File tmp = new File("/tmp/classfile");
						if (tmp.exists())
							tmp.delete();
						FileOutputStream fos = new FileOutputStream(tmp);
						byte buf[] = new byte[1024];
						int len;
						InputStream is = zip.getInputStream(e);
						while ((len = is.read(buf)) > 0) {
							fos.write(buf, 0, len);
						}
						is.close();
						fos.close();
						//						System.out.println("Done reading");
						processJar(tmp, new File("tmp2"));

						zos.putNextEntry(outEntry);
						is = new FileInputStream("tmp2/classfile");
						byte[] buffer = new byte[1024];
						while (true) {
							int count = is.read(buffer);
							if (count == -1)
								break;
							zos.write(buffer, 0, count);
						}
						is.close();
						zos.closeEntry();
						//						jos.closeEntry();
					} else {
						ZipEntry outEntry = new ZipEntry(e.getName());
						if (e.isDirectory()) {
							zos.putNextEntry(outEntry);
							zos.closeEntry();
						} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
							// don't copy this
						} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
							Scanner s = new Scanner(zip.getInputStream(e));
							zos.putNextEntry(outEntry);

							String curPair = "";
							while (s.hasNextLine()) {
								String line = s.nextLine();
								if (line.equals("")) {
									curPair += "\n";
									if (!curPair.contains("SHA1-Digest:"))
										zos.write(curPair.getBytes());
									curPair = "";
								} else {
									curPair += line + "\n";
								}
							}
							s.close();
							zos.write("\n".getBytes());
							zos.closeEntry();
						} else {
							zos.putNextEntry(outEntry);
							InputStream is = zip.getInputStream(e);
							byte[] buffer = new byte[1024];
							while (true) {
								int count = is.read(buffer);
								if (count == -1)
									break;
								zos.write(buffer, 0, count);
							}
							zos.closeEntry();
						}
					}
				}

			}
			if (pass_number == PASS_OUTPUT) {
				zos.close();
				zip.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.log(Level.SEVERE, "Unable to process zip" + f, e);
			File dest = new File(outputDir.getPath() + File.separator + f.getName());
			FileChannel source = null;
			FileChannel destination = null;

			try {
				source = new FileInputStream(f).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "Unable to copy file " + f, ex);
//				System.exit(-1);
			} finally {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
				if (destination != null) {
					try {
						destination.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			}
		}

	}
	public static MethodListClassNode getClassNodeWithMethod(String owner, String name, String desc) {
		MethodListClassNode cn = instrumentedClasses.get(owner);
		if(cn != null)
		{
			for(Object o : cn.methods)
			{
				MethodNode fn = (MethodNode) o;
				if(fn.name.equals(name) && fn.desc.equals(desc))
					return cn;
			}
			if(cn.interfaces != null)
			{
				for(Object o : cn.interfaces)
				{
					MethodListClassNode r = getClassNodeWithMethod((String) o, name,desc);
					if(r != null)
						return r;
				}
			}
			if(cn.superName != null)
				return getClassNodeWithMethod(cn.superName, name,desc);
		}
		return null;
	}

	public static MethodListClassNode getClassNodeWithField(String owner, String name) {
		MethodListClassNode cn = instrumentedClasses.get(owner);
		if(cn != null)
		{
			for(Object o : cn.fields)
			{
				FieldNode fn = (FieldNode) o;
				if(fn.name.equals(name))
					return cn;
			}
			if(cn.interfaces != null)
			{
				for(Object o : cn.interfaces)
				{
					MethodListClassNode r = getClassNodeWithField((String) o, name);
					if(r != null)
						return r;
				}
			}
			if(cn.superName != null)
				return getClassNodeWithField(cn.superName, name);
		}
		return null;
	}
	public static FieldNode getFieldNode(String owner, String name) {
		ClassNode cn = getClassNode(owner);
		if(cn != null)
		{
			for(Object o : cn.fields)
			{
				FieldNode fn = (FieldNode) o;
				if(fn.name.equals(name))
					return fn;
			}
			if(cn.interfaces != null)
			{
				for(Object o : cn.interfaces)
				{
					FieldNode r = getFieldNode((String) o, name);
					if(r != null)
						return r;
				}
			}
			if(cn.superName != null)
				return getFieldNode(cn.superName, name);
		}
		return null;
	}

}
interface ClassVisitorFactory
{
	VMVMClassVisitor getVisitor(ClassVisitor cv, ClassNode thisClassInfo, boolean generateVMVMInterface);
}
class DebugVisitorFactory implements ClassVisitorFactory
{
	@Override
	public VMVMClassVisitor getVisitor(ClassVisitor cv, ClassNode thisClassInfo, boolean generateVMVMInterface) {
		return new ClinitPrintingCV(Opcodes.ASM4, cv, generateVMVMInterface);
	}
}
class JUnitVisitorFactory implements ClassVisitorFactory
{
	@Override
	public VMVMClassVisitor getVisitor(ClassVisitor cv, ClassNode thisClassInfo, boolean generateVMVMInterface) {
		return new JUnitResettingClassVisitor(cv, thisClassInfo, generateVMVMInterface);
	}
}
class VMVMVisitorFactory implements ClassVisitorFactory
{
	@Override
	public VMVMClassVisitor getVisitor(ClassVisitor cv, ClassNode thisClassInfo, boolean generateVMVMInterface) {
		return new InterceptingClassVisitor(cv, thisClassInfo, generateVMVMInterface);
	}
}