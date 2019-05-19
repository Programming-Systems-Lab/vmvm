package edu.columbia.cs.psl.vmvm.runtime;

import java.edu.columbia.cs.psl.vmvm.runtime.InterfaceReinitializer;
import java.edu.columbia.cs.psl.vmvm.runtime.VMVMInstrumented;
import edu.columbia.cs.psl.vmvm.runtime.inst.ClassReinitCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReflectionFixingCV;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.HashSet;

public class VMVMClassFileTransformer implements ClassFileTransformer {

	public static HashSet<String> ignoredClasses = new HashSet<>();
	public static boolean isIgnoredClass(String internalName) {
		internalName = internalName.replace('.','/');
		if (isWhitelistedClass(internalName))
			return false;
		if(ignoredClasses.contains(internalName))
			return true;
		return
				internalName.startsWith("java")
						|| internalName.startsWith("jdk")
						|| internalName.startsWith("sun/misc")
						|| internalName.startsWith("sun/reflect")
						|| internalName.equals("sun/java2d/opengl/OGLRenderQueue")
						|| internalName.startsWith("sun")
				|| internalName.startsWith("com/sun/java/util/jar")
						|| internalName.startsWith("edu/columbia/cs/psl/vmvm/runtime")
						|| internalName.startsWith("org/junit")
						|| internalName.startsWith("junit/")
						|| internalName.startsWith("java/edu/columbia/cs/psl/vmvm")
						|| internalName.startsWith("edu/columbia/cs/psl/vmvm/")
						|| internalName.startsWith("org/apache/maven/surefire") || internalName.startsWith("org/apache/tools/")
						|| internalName.startsWith("org/mockito") || internalName.startsWith("mockit")
						|| internalName.startsWith("org/powermock")
						|| internalName.startsWith("com/jprofiler");
	}

	public static boolean isWhitelistedClass(String internalName) {
		return internalName.startsWith("javax/servlet") || internalName.startsWith("com/sun/jini") || internalName.startsWith("java/awt") || internalName.startsWith("javax/swing") || internalName.startsWith("javax/xml") || internalName.startsWith("sun/awt");
	}

	public static boolean isClassThatNeedsReflectionHacked(String internalName) {
		return ignoredClasses.contains(internalName) || internalName.startsWith("java/io/ObjectOutputStream") || internalName.startsWith("java/io/ObjectStream")
				|| internalName.startsWith("sun/reflect/annotation/AnnotationInvocationHandler");
		//		return internalName.startsWith("java/") && !internalName.startsWith("java/lang/reflect") && !internalName.equals("java/lang/Class");
	}

	public static AdditionalInterfaceClassloader cl = new AdditionalInterfaceClassloader();

	public static HashSet<String> instrumentedClasses = new HashSet<String>();
	public static final boolean DEBUG = System.getProperty("vmvm.debug") != null;
	public static final boolean ALWAYS_REOPT = false;
	public static final boolean HOTSPOT_REOPT = false;

	@SuppressWarnings("restriction")
	static sun.misc.Unsafe theUnsafe;

	@SuppressWarnings("restriction")
	public static sun.misc.Unsafe getUnsafe() {
		if (theUnsafe == null) {
			try {
				Field f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				theUnsafe = (Unsafe) f.get(null);
			}catch(NoSuchFieldException | IllegalAccessException ex){
				ex.printStackTrace();
			}
		}
		return theUnsafe;
	}

	public static Class<?> generateResetter(Class<?> hostClass)
	{

		String hostName = hostClass.getName().replace(".","/");
		String newName = hostName + Constants.VMVM_RESET_FIELD;
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		Integer version = ClassReinitCV.reClinitClassHelperVersions.remove(hostName);
		if(version == null)
			version = Opcodes.V1_8;
		cw.visit(version, Opcodes.ACC_PUBLIC, newName , null, InterfaceReinitializer.INTERNAL_NAME, null);
		cw.visitSource(null, null);
		MethodNode clinitMethod = ClassReinitCV.reClinitMethods.remove(hostName);
		if(clinitMethod != null)
		{
			cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
			clinitMethod.accept(cw);
		}
		else{
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "__vmvmReClinit", "()V", null, null);
			mv.visitCode();
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, hostName, "__vmvmReClinit", "()V", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, InterfaceReinitializer.INTERNAL_NAME, "<init>", "()V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		cw.visitEnd();
		byte[] b = cw.toByteArray();
		if (DEBUG) {
			File debugDir = new File("debug");
			if (!debugDir.exists())
				debugDir.mkdir();
			File f = new File("debug/" + newName.replace("/",".") +".class");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(f);
				fos.write(cw.toByteArray());
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (DEBUG) {
			ClassReader cr2 = new ClassReader(b);
			cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
		}
		return getUnsafe().defineAnonymousClass(hostClass, b, new Object[0]);
	}
	private void generateResetter(ClassReader cr, ClassReinitCV cv, String className, ClassLoader loader, ProtectionDomain protectionDomain) {
		if (
				((instrumentedClasses.contains(className))
				)) //|| hasClass(loader, className + Constants.VMVM_RESET_FIELD)))
			return;
		String newName = cr.getClassName() + Constants.VMVM_RESET_FIELD;
//		System.out.println("Generating " + newName);
			instrumentedClasses.add(className);

		try {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			cw.visitSource(null, null);



			cw.visitEnd();
			if (DEBUG) {
				File debugDir = new File("debug");
				if (!debugDir.exists())
					debugDir.mkdir();
				File f = new File("debug/" + newName.replace("/", ".") + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(cw.toByteArray());
				fos.close();
			}
			byte[] b = cw.toByteArray();
			if (DEBUG) {
				ClassReader cr2 = new ClassReader(b);
				cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			}
			if(alwaysLoadInBootCP(newName))
			{
				loader = null;
				protectionDomain = null;
			}
//			System.out.println("Done with " + newName + " in " + System.identityHashCode(loader) + ", " + System.identityHashCode(protectionDomain));
			getUnsafe().defineClass(newName.replace("/", "."), b, 0, b.length, loader, protectionDomain);

		} catch (Exception ex) {
			System.err.println("Problem while creating " + newName);
			ex.printStackTrace();
			
			System.exit(-1);
		}
	}

	private boolean alwaysLoadInBootCP(String newName) {
		return newName.startsWith("org/xml")
				|| newName.startsWith("org/w3c")
				|| newName.startsWith("java/")
				|| newName.startsWith("javax/");
	}

	private static HashSet<String> instrumentedInterfaces = new HashSet<String>();

	@SuppressWarnings("restriction")
	@Override
	public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		//		if(DEBUG)
//					System.err.println("VMVMClassfiletransformer PLAIN1 " + className + " " + loader + " " + classBeingRedefined);
		if(className == null)
			return null;
		if(className.equals("org/junit/internal/requests/ClassRequest"))
		{
			try {
				ClassReader cr = new ClassReader(classfileBuffer);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				JUnitInterceptingClassVisitor cv = new JUnitInterceptingClassVisitor(cw);
				cr.accept(cv, 0);
				if (DEBUG) {
					File debugDir = new File("debug");
					if (!debugDir.exists())
						debugDir.mkdir();
					File f = new File("debug/" + className.replace("/", ".") + ".class");
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(cw.toByteArray());
					fos.close();
				}
				return cw.toByteArray();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (classBeingRedefined == null && isClassThatNeedsReflectionHacked(className)) {
			try {
				ClassReader cr = new ClassReader(classfileBuffer);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ReflectionFixingCV cv = new ReflectionFixingCV(cw);
				cr.accept(cv, 0);
				if (DEBUG) {
					File debugDir = new File("debug");
					if (!debugDir.exists())
						debugDir.mkdir();
					File f = new File("debug/" + className.replace("/", ".") + ".class");
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(cw.toByteArray());
					fos.close();
				}
				return cw.toByteArray();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (isIgnoredClass(className)) {
//			System.out.println("Skip "  + className);
			return null;
		}
		if (classBeingRedefined != null) {
//			System.err.println("Redefine!");
//			return null;
		}
//		System.out.println("Inst " + className);
		try {
			//			if(DEBUG)
//			System.err.println("VMVMClassfiletransformer PLAIN " + className + " in " + loader);
//			new Exception().printStackTrace();
			ClassReader cr = new ClassReader(classfileBuffer);

			if(DEBUG)
			{
				if (DEBUG) {
					File debugDir = new File("debug-uninst");
					if (!debugDir.exists())
						debugDir.mkdir();
					File f = new File("debug-uninst/" + className.replace("/", ".") + ".class");
					try{
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(classfileBuffer);
					fos.close();
					}
					catch(Throwable t)
					{
						t.printStackTrace();
					}
				}
			}
			if (DEBUG) {
				ClassNode cn = new ClassNode();
				cr.accept(cn, 0);
				for (Object s : cn.interfaces) {
					if (s.equals(Type.getInternalName(VMVMInstrumented.class)))
						return null;
//						throw new IllegalArgumentException();
				}
			}
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor _cv = cw;
			if(DEBUG)
				_cv = new CheckClassAdapter(_cv, false);
			ClassReinitCV cv = new ClassReinitCV(_cv);
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
			byte[] ret = cw.toByteArray();

			if (DEBUG) {
				File debugDir = new File("debug-plain");
				if (!debugDir.exists())
					debugDir.mkdir();
				File f = new File("debug-plain/" + className.replace("/", ".") + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(ret);
				fos.close();
			}
			if (DEBUG) {
				ClassReader cr2 = new ClassReader(ret);
				cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			}

			if (DEBUG) {
				File debugDir = new File("debug");
				if (!debugDir.exists())
					debugDir.mkdir();
				File f = new File("debug/" + className.replace("/", ".") + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(ret);
				fos.close();
			}
			return ret;
		} catch (Throwable t) {
			//Make sure that an exception in instrumentation gets printed, rather than squelched
			System.err.println("In transformation for " + className + ":");
			t.printStackTrace();
			return null;
		}

	}

	private boolean hasClass(ClassLoader loader, String string) {
		try{
			if(loader == null)
				Class.forName(string.replace("/", "."));
			else
				loader.loadClass(string.replace("/", "."));
			return true;
		}
		catch(Exception ex)
		{
//			System.out.println("Couldn't find " + string.replace("/", ".") + " in " + loader);
			return false;
		}
	}

}
