package edu.columbia.cs.psl.vmvm.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReflectionFixingCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCapabilityCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCheckForceCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.StaticFinalMutibleizer;

public class VMVMClassFileTransformer implements ClassFileTransformer {
	public static boolean isIgnoredClass(String internalName) {
		if(isWhitelistedClass(internalName))
			return false;
		return internalName.startsWith("java") || internalName.startsWith("jdk") || internalName.startsWith("sun") || internalName.startsWith("com/sun") || internalName.startsWith("edu/columbia/cs/psl/vmvm/runtime")
				|| internalName.startsWith("org/junit") || internalName.startsWith("junit/") || internalName.startsWith("edu/columbia/cs/psl/vmvm/")
				|| internalName.startsWith("org/apache/maven/surefire") || internalName.startsWith("org/apache/tools/")
				|| internalName.startsWith("com/jprofiler");
	}
	public static boolean isWhitelistedClass(String internalName)
	{
		return internalName.startsWith("javax/servlet") || internalName.startsWith("com/sun/jini");
	}
	public static boolean isClassThatNeedsReflectionHacked(String internalName)
	{
		return internalName.startsWith("java/io/ObjectOutputStream") || internalName.startsWith("java/io/ObjectStream");
//		return internalName.startsWith("java/") && !internalName.startsWith("java/lang/reflect") && !internalName.equals("java/lang/Class");
	}

	public static AdditionalInterfaceClassloader cl = new AdditionalInterfaceClassloader();

	public static HashMap<String, byte[]> instrumentedClasses = new HashMap<String, byte[]>();
	public static final boolean DEBUG = false;
	public static final boolean ALWAYS_REOPT = false;
	public static final boolean HOTSPOT_REOPT = false;
	
	@SuppressWarnings("restriction")
	static sun.misc.Unsafe theUnsafe;
	@SuppressWarnings("restriction")
	static sun.misc.Unsafe getUnsafe()
	{
		if(theUnsafe == null)
			theUnsafe = sun.misc.Unsafe.getUnsafe();
		return theUnsafe;
	}
	
	private void generateInterface(ClassReader cr, ReinitCapabilityCV cv, String className, ClassLoader loader, ProtectionDomain protectionDomain) throws IOException
	{
		instrumentedInterfaces.add(className);
		String newName = cr.getClassName() + Constants.VMVM_RESET_SUFFIX;
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		newName = newName+"$$INTERFACE";
		if (!isIgnoredClass(cr.getSuperName()))
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, newName, null, cr.getSuperName()+Constants.VMVM_RESET_SUFFIX, null);
		else
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, newName, null, "java/lang/Object", null);
		cw.visitSource(null, null);

		for(FieldNode fn : cv.getMutabilizedFields())
		{
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "get"+fn.name, "()"+fn.desc, null, null);
			mv = new StaticFinalMutibleizer(mv, false);
			mv.visitCode();
			Label ok = new Label();
			mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
			mv.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
			mv.visitJumpInsn(Opcodes.IFEQ, ok);
			if((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0)
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, className+Constants.VMVM_RESET_SUFFIX, "__vmvmReClinit", "()V", false);
			else
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__vmvmReClinit", "()V", false);
			mv.visitLabel(ok);
			Type t = Type.getType(fn.desc);
			
			mv.visitFrame(Opcodes.F_NEW, 1, new Object[] { newName}, 0, null);		
			
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

			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "set"+fn.name, "("+fn.desc+")V", null, null);
			mv = new StaticFinalMutibleizer(mv, false);
			mv.visitCode();
			
			ok = new Label();
			mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
//			if (className.equals("org/apache/log4j/Level")) { //XXX todo why was this necessary on nutch!?
//				super.visitJumpInsn(Opcodes.IFNULL, ok);
//				super.visitFieldInsn(Opcodes.GETSTATIC, owner, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
//			}
			mv.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
			mv.visitJumpInsn(Opcodes.IFEQ, ok);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__vmvmReClinit", "()V", false);
			mv.visitLabel(ok);
			
			switch(t.getSort())
			{
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Opcodes.INTEGER }, 0, null);
				break;
			case Type.DOUBLE:
				mv.visitFrame(Opcodes.F_NEW, 3, new Object[] { newName, Opcodes.DOUBLE, Opcodes.TOP }, 0, null);
				break;
			case Type.FLOAT:
				mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { newName, Opcodes.FLOAT }, 0, null);
				break;
			case Type.LONG:
				mv.visitFrame(Opcodes.F_NEW, 3, new Object[] { newName, Opcodes.LONG, Opcodes.TOP }, 0, null);
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
		if(DEBUG){
			ClassReader cr2 = new ClassReader(b);
			cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			}
		getUnsafe().defineClass(newName.replace("/", "."), b, 0, b.length, loader, protectionDomain);
	}
	private HashSet<String> instrumentedInterfaces = new HashSet<String>();
	@SuppressWarnings("restriction")
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//		if(DEBUG)
//			System.err.println("VMVMClassfiletransformer PLAIN1 " + className);
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
//			try {
//				Field field = classBeingRedefined.getDeclaredField(Constants.VMVM_NEEDS_RESET);
//				field.setAccessible(true);
//				ClassState cs = (ClassState) field.get(null);
//				if (cs.hasClassesToOptAway) {
//					//											System.out.println("Trying to reopt " + className );
//					ClassReader cr = new ClassReader(cs.originalClass);
//					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//					ReinitCheckForceCV cv = new ReinitCheckForceCV(cw, true);
//					try {
//						cr.accept(cv, ClassReader.EXPAND_FRAMES);
//					} catch (Throwable t) {
//						throw new IllegalStateException(t);
//					}
//					if (DEBUG) {
//						File debugDir = new File("debug-instheavyopt");
//						if (!debugDir.exists())
//							debugDir.mkdir();
//						File f = new File("debug-instheavyopt/" + className.replace("/", ".") + ".class");
//						FileOutputStream fos = new FileOutputStream(f);
//						fos.write(cw.toByteArray());
//						fos.close();
//					}
//					cs.isOptimized = true;
//					cs.hasClassesToOptAway = false;
//					return cw.toByteArray();
//				}
//				if(cs.fullyInstrumentedClass == null)
//				{
//					ClassReader cr = new ClassReader(cs.originalClass);
//					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//					ReinitCheckForceCV cv = new ReinitCheckForceCV(cw, false);
//					try {
//						cr.accept(cv, ClassReader.EXPAND_FRAMES);
//					} catch (Throwable t) {
//						throw new IllegalStateException(t);
//					}
//					cs.fullyInstrumentedClass = cw.toByteArray();
//					if (VMVMClassFileTransformer.DEBUG) {
//						File debugDir = new File("debug-instheavy");
//						if (!debugDir.exists())
//							debugDir.mkdir();
//						File f = new File("debug-instheavy/" + cs.name.replace("/", ".") + ".class");
//						FileOutputStream fos = new FileOutputStream(f);
//						fos.write(cs.fullyInstrumentedClass);
//						fos.close();
//					}
//				}
//				cs.isOptimized = false;
//				return cs.fullyInstrumentedClass;
//
//			} catch (Throwable e) {
//				System.err.println("Problem when processing " + classBeingRedefined);
//				e.printStackTrace();
//			}
//
//			System.err.println("Returning null for some reason??");
//			new Exception().printStackTrace();
			return null;
		}
		try {
			if(DEBUG)
						System.err.println("VMVMClassfiletransformer PLAIN " + className);
			ClassReader cr = new ClassReader(classfileBuffer);

			if(DEBUG)
			{
				ClassNode cn = new ClassNode();
				cr.accept(cn, 0);
				for(Object s : cn.interfaces)
				{
					if(s.equals(Type.getInternalName(VMVMInstrumented.class)))
						throw new IllegalArgumentException();
				}
			}
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ReinitCapabilityCV cv = new ReinitCapabilityCV(new CheckClassAdapter(cw, false));
			//new CheckClassAdapter(cw));
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
			byte[] ret = cw.toByteArray();
			instrumentedClasses.put(className, ret);

			if (DEBUG) {
				File debugDir = new File("debug-plain");
				if (!debugDir.exists())
					debugDir.mkdir();
				File f = new File("debug-plain/" + className.replace("/", ".") + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(ret);
				fos.close();
			}

			//now generate the one that has force checks in too
			cr = new ClassReader(ret);

			cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ReinitCheckForceCV cv2 = new ReinitCheckForceCV(cw, false);
			try {
				cr.accept(cv2, ClassReader.EXPAND_FRAMES);
			} catch (Throwable t) {
				System.out.println("Problem on " + className);
				throw new IllegalStateException(t);
			}

			ret = cw.toByteArray();
			if(DEBUG){
			ClassReader cr2 = new ClassReader(ret);
			cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			}
			boolean isInterface = (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
			if(isInterface && !instrumentedInterfaces.contains(className))
			{
				generateInterface(cr,cv,className,loader,protectionDomain);
			}
			if (cv.getMutabilizedFields().size() != 0) {
				//Also, generate a resetter for the interface
				String newName = cr.getClassName() + Constants.VMVM_RESET_SUFFIX;
				//				Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});
				//				defineClassMethod.setAccessible(true);
				try {
					cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					ArrayList<String> interfaces = new ArrayList<String>();
					for(String s : cr.getInterfaces())
					{
						if(!isIgnoredClass(s))
						{

							if(!instrumentedInterfaces.contains(s))
							{
								System.out.println("Trying to load " + s);
								InputStream is = loader.getResourceAsStream(s+".class");
								ClassReader _cr = new ClassReader(is);
								ReinitCapabilityCV _cv = new ReinitCapabilityCV(null);
								_cr.accept(_cv, ClassReader.SKIP_CODE);
								generateInterface(_cr,_cv,s,loader,protectionDomain);
								is.close();
							}
							interfaces.add(s+Constants.VMVM_RESET_SUFFIX+"$$INTERFACE");
						}
					}
					String[] ifaces = interfaces.toArray(new String[0]);
					if (!isIgnoredClass(cr.getSuperName()))
						cw.visit(cv.version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newName, null, cr.getSuperName()+Constants.VMVM_RESET_SUFFIX, ifaces);
					else
						cw.visit(cv.version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newName, null, "java/lang/Object", ifaces);
					cw.visitSource(null, null);
					cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
					for (String s : cv.getOwnersOfStaticFields()) {
						cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z", null, true);
					}

					if(cv.getReClinitMethod() != null)
						cv.getReClinitMethod().accept(cw);
					{						
						MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
						mv.visitCode();
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						if (!isIgnoredClass(cr.getSuperName()))
							mv.visitMethodInsn(Opcodes.INVOKESPECIAL, cr.getSuperName()+Constants.VMVM_RESET_SUFFIX, "<init>", "()V", false);
						else
							mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
						mv.visitInsn(Opcodes.RETURN);
						mv.visitMaxs(0, 0);
						mv.visitEnd();
					}
					for(FieldNode fn : cv.getMutabilizedFields())
					{
						MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "get"+fn.name, "()"+fn.desc, null, null);
						mv = new StaticFinalMutibleizer(mv, false);
						mv.visitCode();
						Label ok = new Label();
						mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
						mv.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
						mv.visitJumpInsn(Opcodes.IFEQ, ok);
						if((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0)
							mv.visitMethodInsn(Opcodes.INVOKESTATIC, className+Constants.VMVM_RESET_SUFFIX, "__vmvmReClinit", "()V", false);
						else
							mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__vmvmReClinit", "()V", false);
						mv.visitLabel(ok);
						Type t = Type.getType(fn.desc);
						
						mv.visitFrame(Opcodes.F_NEW, 1, new Object[] { className + Constants.VMVM_RESET_SUFFIX}, 0, null);		
						
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

						mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "set"+fn.name, "("+fn.desc+")V", null, null);
						mv = new StaticFinalMutibleizer(mv, false);
						mv.visitCode();
						
						ok = new Label();
						mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
//						if (className.equals("org/apache/log4j/Level")) { //XXX todo why was this necessary on nutch!?
//							super.visitJumpInsn(Opcodes.IFNULL, ok);
//							super.visitFieldInsn(Opcodes.GETSTATIC, owner, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
//						}
						mv.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
						mv.visitJumpInsn(Opcodes.IFEQ, ok);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__vmvmReClinit", "()V", false);
						mv.visitLabel(ok);
						
						switch(t.getSort())
						{
						case Type.BOOLEAN:
						case Type.BYTE:
						case Type.CHAR:
						case Type.SHORT:
						case Type.INT:
							mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.INTEGER }, 0, null);
							break;
						case Type.DOUBLE:
							mv.visitFrame(Opcodes.F_NEW, 3, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.DOUBLE, Opcodes.TOP }, 0, null);
							break;
						case Type.FLOAT:
							mv.visitFrame(Opcodes.F_NEW, 2, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.FLOAT }, 0, null);
							break;
						case Type.LONG:
							mv.visitFrame(Opcodes.F_NEW, 3, new Object[] { className + Constants.VMVM_RESET_SUFFIX, Opcodes.LONG, Opcodes.TOP }, 0, null);
							break;
						case Type.ARRAY:
						case Type.OBJECT:
							System.out.println(Type.getType(fn.desc).getInternalName());
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
					if(DEBUG){
						ClassReader cr2 = new ClassReader(b);
						cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
						}
					getUnsafe().defineClass(newName.replace("/", "."), b, 0, b.length, loader, protectionDomain);

				} catch (Exception ex) {
					ex.printStackTrace();
				}
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
			t.printStackTrace();
			return null;
		}

	}

}
