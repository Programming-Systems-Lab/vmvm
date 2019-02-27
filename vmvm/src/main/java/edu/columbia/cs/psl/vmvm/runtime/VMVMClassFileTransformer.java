package edu.columbia.cs.psl.vmvm.runtime;

import edu.columbia.cs.psl.vmvm.runtime.inst.ClassReinitCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReflectionFixingCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.StaticFinalMutibleizer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.util.CheckClassAdapter;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;

public class VMVMClassFileTransformer implements ClassFileTransformer {

	public static boolean isIgnoredClass(String internalName) {
		internalName = internalName.replace('.','/');
		if (isWhitelistedClass(internalName))
			return false;
		return internalName.startsWith("java") || internalName.startsWith("jdk") || internalName.startsWith("sun") || internalName.startsWith("com/sun")
				|| internalName.startsWith("edu/columbia/cs/psl/vmvm/runtime") || internalName.startsWith("org/junit") || internalName.startsWith("junit/")
				|| internalName.startsWith("edu/columbia/cs/psl/vmvm/") || internalName.startsWith("org/apache/maven/surefire") || internalName.startsWith("org/apache/tools/")
				|| internalName.startsWith("org/mockito") || internalName.startsWith("mockit")
				|| internalName.startsWith("org/powermock")
				|| internalName.startsWith("com/jprofiler");
	}

	public static boolean isWhitelistedClass(String internalName) {
		return internalName.startsWith("javax/servlet") || internalName.startsWith("com/sun/jini") ;
	}

	public static boolean isClassThatNeedsReflectionHacked(String internalName) {
		return internalName.startsWith("java/io/ObjectOutputStream") || internalName.startsWith("java/io/ObjectStream")
				|| internalName.startsWith("sun/reflect/annotation/AnnotationInvocationHandler");
		//		return internalName.startsWith("java/") && !internalName.startsWith("java/lang/reflect") && !internalName.equals("java/lang/Class");
	}

	public static AdditionalInterfaceClassloader cl = new AdditionalInterfaceClassloader();

	public static HashSet<String> instrumentedClasses = new HashSet<String>();
	public static final boolean DEBUG = true;
	public static final boolean ALWAYS_REOPT = false;
	public static final boolean HOTSPOT_REOPT = false;

	@SuppressWarnings("restriction")
	static sun.misc.Unsafe theUnsafe;

	@SuppressWarnings("restriction")
	static sun.misc.Unsafe getUnsafe() {
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

	private void generateResetter(ClassReader cr, ClassReinitCV cv, String className, ClassLoader loader, ProtectionDomain protectionDomain) {
		if (
				((instrumentedClasses.contains(className))
				)) //|| hasClass(loader, className + Constants.VMVM_RESET_SUFFIX)))
			return;
		String newName = cr.getClassName() + Constants.VMVM_RESET_SUFFIX;
//		System.out.println("Generating " + newName);
			instrumentedClasses.add(className);

		try {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_NEEDS_RESET, "Z", null, null);
			ArrayList<String> interfaces = new ArrayList<String>();

			for (String s : cr.getInterfaces()) {
				if (!isIgnoredClass(s)) {

					if (!instrumentedInterfaces.contains(s)) {
						InputStream is;
						if (loader == null)
							is = Thread.currentThread().getContextClassLoader().getResourceAsStream(s + ".class");
						else
							is = loader.getResourceAsStream(s + ".class");

						ClassReader _cr = new ClassReader(is);
						ClassReinitCV _cv = new ClassReinitCV(null);
						_cr.accept(_cv, ClassReader.SKIP_CODE);
						generateInterface(_cr, _cv, s, loader, protectionDomain);
						is.close();

					}
					interfaces.add(s + Constants.VMVM_RESET_SUFFIX + "$$INTERFACE");
				}
			}
			String[] ifaces = interfaces.toArray(new String[0]);
			if (!isIgnoredClass(cr.getSuperName()))
			{
				if(!instrumentedClasses.contains(cr.getSuperName()))
				{
					InputStream is;
					if (loader == null)
						is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cr.getSuperName() + ".class");
					else
						is = loader.getResourceAsStream(cr.getSuperName() + ".class");

					ClassReader _cr = new ClassReader(is);
					ClassReinitCV _cv = new ClassReinitCV(null);
					_cr.accept(_cv, ClassReader.EXPAND_FRAMES);
					generateResetter(_cr, _cv, cr.getSuperName(), loader, protectionDomain);
					is.close();
				}
				cw.visit(cv.version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newName, null, cr.getSuperName() + Constants.VMVM_RESET_SUFFIX, ifaces);
			}
			else
				cw.visit(cv.version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newName, null, "java/lang/Object", ifaces);
			cw.visitSource(null, null);
			cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
			for (String s : cv.getOwnersOfStaticFields()) {
				cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z", null, true);
			}

			if (cv.getReClinitMethod() != null)
				cv.getReClinitMethod().accept(cw);
			else
			{
				//Generate a reclinit method that just calls the one on the actual class :/
				MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "__vmvmReClinit", "()V", null, null);
				mv.visitCode();
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, cr.getClassName(), "__vmvmReClinit", "()V", false);
				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(0, 0);
				mv.visitEnd();
			}

			//...
			{
				MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,"instance","()L"+cr.getClassName()+Constants.VMVM_RESET_SUFFIX+";",null,null);
				mv.visitCode();
				mv.visitTypeInsn(Opcodes.NEW, className + Constants.VMVM_RESET_SUFFIX);
				mv.visitInsn(Opcodes.DUP);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className + Constants.VMVM_RESET_SUFFIX, "<init>", "()V", false);
				mv.visitInsn(Opcodes.ARETURN);
				mv.visitMaxs(0,0);
				mv.visitEnd();
			}


			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			if (!isIgnoredClass(cr.getSuperName()))
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, cr.getSuperName() + Constants.VMVM_RESET_SUFFIX, "<init>", "()V", false);
			else
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();

			for (FieldNode fn : cv.getAllFields()) {
				mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "get" + fn.name, "()" + fn.desc, null, null);
				mv = new StaticFinalMutibleizer(mv, cv.getFinalFields(), false);
				mv.visitCode();
				Label ok = new Label();
				mv.visitFieldInsn(Opcodes.GETSTATIC, className+Constants.VMVM_RESET_SUFFIX, Constants.VMVM_NEEDS_RESET, "Z");
				mv.visitJumpInsn(Opcodes.IFEQ, ok);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, className + Constants.VMVM_RESET_SUFFIX, "__vmvmReClinit", "()V", false);

				mv.visitLabel(ok);
				Type t = Type.getType(fn.desc);

				mv.visitFrame(Opcodes.F_NEW, 1, new Object[] { className + Constants.VMVM_RESET_SUFFIX }, 0, null);

				mv.visitFieldInsn(Opcodes.GETSTATIC, cr.getClassName(), fn.name, fn.desc);
				switch (t.getSort()) {
				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.SHORT:
				case Type.INT:
					mv.visitInsn(Opcodes.IRETURN);
					break;
				case Type.DOUBLE:
					mv.visitInsn(Opcodes.DRETURN);
					break;
				case Type.FLOAT:
					mv.visitInsn(Opcodes.FRETURN);
					break;
				case Type.LONG:
					mv.visitInsn(Opcodes.LRETURN);
					break;
				case Type.ARRAY:
				case Type.OBJECT:
					mv.visitInsn(Opcodes.ARETURN);
					break;
				default:
					throw new UnsupportedOperationException();
				}
				mv.visitMaxs(0, 0);
				mv.visitEnd();

				mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "set" + fn.name, "(" + fn.desc + ")V", null, null);
				mv = new StaticFinalMutibleizer(mv, cv.getFinalFields(), false);
				mv.visitCode();

				ok = new Label();
				mv.visitFieldInsn(Opcodes.GETSTATIC, className + Constants.VMVM_RESET_SUFFIX, Constants.VMVM_NEEDS_RESET, "Z");
				mv.visitJumpInsn(Opcodes.IFEQ, ok);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, className + Constants.VMVM_RESET_SUFFIX, "__vmvmReClinit", "()V", false);
				mv.visitLabel(ok);
//				mv.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);

				switch (t.getSort()) {
				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.SHORT:
				case Type.INT:
					mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.INTEGER }, 0, null);
					break;
				case Type.DOUBLE:
					mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.DOUBLE }, 0, null);
					break;
				case Type.FLOAT:
					mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.FLOAT }, 0, null);
					break;
				case Type.LONG:
					mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.LONG }, 0, null);
					break;
				case Type.ARRAY:
				case Type.OBJECT:
					mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Type.getType(fn.desc).getInternalName() }, 0, null);
					break;
				}

				switch (Type.getType(fn.desc).getSort()) {
				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.SHORT:
				case Type.INT:
					mv.visitVarInsn(Opcodes.ILOAD, 1);
					break;
				case Type.DOUBLE:
					mv.visitVarInsn(Opcodes.DLOAD, 1);
					break;
				case Type.FLOAT:
					mv.visitVarInsn(Opcodes.FLOAD, 1);
					break;
				case Type.LONG:
					mv.visitVarInsn(Opcodes.LLOAD, 1);
					break;
				case Type.ARRAY:
				case Type.OBJECT:
					mv.visitVarInsn(Opcodes.ALOAD, 1);
					break;
				default:
					throw new UnsupportedOperationException();
				}
				mv.visitFieldInsn(Opcodes.PUTSTATIC, cr.getClassName(), fn.name, fn.desc);
				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(0, 0);
				mv.visitEnd();
			}
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

	private void generateInterface(ClassReader cr, ClassReinitCV cv, String className, ClassLoader loader, ProtectionDomain protectionDomain) throws IOException {
		if(
				((instrumentedInterfaces.contains(className)) ) //||
//						hasClass(loader, className + Constants.VMVM_RESET_SUFFIX + "$$INTERFACE"))
			)
			return;
		instrumentedInterfaces.add(className);
		String newName = cr.getClassName() + Constants.VMVM_RESET_SUFFIX;
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		newName = newName + "$$INTERFACE";
//		System.out.println("Generating " + newName + " in " + loader);

		if (!isIgnoredClass(cr.getSuperName()))
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, newName, null, cr.getSuperName() + Constants.VMVM_RESET_SUFFIX, null);
		else
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, newName, null, "java/lang/Object", null);
		cw.visitSource(null, null);

		for (FieldNode fn : cv.getAllFields()) {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "get" + fn.name, "()" + fn.desc, null, null);
			mv = new StaticFinalMutibleizer(mv, cv.getFinalFields(), false);
			mv.visitCode();
			Label ok = new Label();
			mv.visitFieldInsn(Opcodes.GETSTATIC, className+Constants.VMVM_RESET_SUFFIX, Constants.VMVM_NEEDS_RESET, "Z");
			mv.visitJumpInsn(Opcodes.IFEQ, ok);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, className + Constants.VMVM_RESET_SUFFIX, "__vmvmReClinit", "()V", false);
			mv.visitLabel(ok);
			Type t = Type.getType(fn.desc);

			mv.visitFrame(Opcodes.F_NEW, 1, new Object[] { newName }, 0, null);

			mv.visitFieldInsn(Opcodes.GETSTATIC, cr.getClassName(), fn.name, fn.desc);
			switch (t.getSort()) {
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT:
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.DOUBLE:
				mv.visitInsn(Opcodes.DRETURN);
				break;
			case Type.FLOAT:
				mv.visitInsn(Opcodes.FRETURN);
				break;
			case Type.LONG:
				mv.visitInsn(Opcodes.LRETURN);
				break;
			case Type.ARRAY:
			case Type.OBJECT:
				mv.visitInsn(Opcodes.ARETURN);
				break;
			default:
				throw new UnsupportedOperationException();
			}
			mv.visitMaxs(0, 0);
			mv.visitEnd();

			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "set" + fn.name, "(" + fn.desc + ")V", null, null);
			mv = new StaticFinalMutibleizer(mv,cv.getFinalFields(),  false);
			mv.visitCode();

			ok = new Label();
			mv.visitFieldInsn(Opcodes.GETSTATIC, className+Constants.VMVM_RESET_SUFFIX, Constants.VMVM_NEEDS_RESET, "Z");
			mv.visitJumpInsn(Opcodes.IFEQ, ok);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, className+Constants.VMVM_RESET_SUFFIX, "__vmvmReClinit", "()V", false);
			mv.visitLabel(ok);

			switch (t.getSort()) {
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Opcodes.INTEGER }, 0, null);
				break;
			case Type.DOUBLE:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Opcodes.DOUBLE }, 0, null);
				break;
			case Type.FLOAT:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Opcodes.FLOAT }, 0, null);
				break;
			case Type.LONG:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Opcodes.LONG }, 0, null);
				break;
			case Type.ARRAY:
			case Type.OBJECT:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Type.getType(fn.desc).getInternalName() }, 0, null);
				break;
			}

			switch (Type.getType(fn.desc).getSort()) {
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT:
				mv.visitVarInsn(Opcodes.ILOAD, 1);
				break;
			case Type.DOUBLE:
				mv.visitVarInsn(Opcodes.DLOAD, 1);
				break;
			case Type.FLOAT:
				mv.visitVarInsn(Opcodes.FLOAD, 1);
				break;
			case Type.LONG:
				mv.visitVarInsn(Opcodes.LLOAD, 1);
				break;
			case Type.ARRAY:
			case Type.OBJECT:
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				break;
			default:
				throw new UnsupportedOperationException();
			}
			mv.visitFieldInsn(Opcodes.PUTSTATIC, cr.getClassName(), fn.name, fn.desc);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}
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
//		System.out.println("Done with " + newName + " in " + System.identityHashCode(loader) + ", " + System.identityHashCode(protectionDomain));
		getUnsafe().defineClass(newName.replace("/", "."), b, 0, b.length, loader, protectionDomain);
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
		if (classBeingRedefined == null && isClassThatNeedsReflectionHacked(className)) {
			try {
				ClassReader cr = new ClassReader(classfileBuffer);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ReflectionFixingCV cv = new ReflectionFixingCV(cw);
				cr.accept(cv, 0);
				return cw.toByteArray();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (isIgnoredClass(className)) {
			return null;
		}
		if (classBeingRedefined != null) {
			System.err.println("Redefine!");
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
			ClassReinitCV cv = new ClassReinitCV(new CheckClassAdapter(cw, false));
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
			boolean isInterface = (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
			if (isInterface) {
				generateInterface(cr, cv, className, loader, protectionDomain);
			}

			generateResetter(cr, cv, className, loader, protectionDomain);
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
