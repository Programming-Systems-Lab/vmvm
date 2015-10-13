package edu.columbia.cs.psl.vmvm.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReflectionFixingCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCapabilityCV;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCheckForceCV;

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
	public static final boolean DEBUG = true;
	public static final boolean ALWAYS_REOPT = false;
	public static final boolean HOTSPOT_REOPT = false;
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
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
			try {
				Field field = classBeingRedefined.getDeclaredField(Constants.VMVM_NEEDS_RESET);
				field.setAccessible(true);
				ClassState cs = (ClassState) field.get(null);
				if (cs.hasClassesToOptAway) {
					//											System.out.println("Trying to reopt " + className );
					ClassReader cr = new ClassReader(cs.originalClass);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					ReinitCheckForceCV cv = new ReinitCheckForceCV(cw, true);
					try {
						cr.accept(cv, ClassReader.EXPAND_FRAMES);
					} catch (Throwable t) {
						throw new IllegalStateException(t);
					}
					if (DEBUG) {
						File debugDir = new File("debug-instheavyopt");
						if (!debugDir.exists())
							debugDir.mkdir();
						File f = new File("debug-instheavyopt/" + className.replace("/", ".") + ".class");
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(cw.toByteArray());
						fos.close();
					}
					cs.isOptimized = true;
					cs.hasClassesToOptAway = false;
					return cw.toByteArray();
				}
				if(cs.fullyInstrumentedClass == null)
				{
					ClassReader cr = new ClassReader(cs.originalClass);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					ReinitCheckForceCV cv = new ReinitCheckForceCV(cw, false);
					try {
						cr.accept(cv, ClassReader.EXPAND_FRAMES);
					} catch (Throwable t) {
						throw new IllegalStateException(t);
					}
					cs.fullyInstrumentedClass = cw.toByteArray();
					if (VMVMClassFileTransformer.DEBUG) {
						File debugDir = new File("debug-instheavy");
						if (!debugDir.exists())
							debugDir.mkdir();
						File f = new File("debug-instheavy/" + cs.name.replace("/", ".") + ".class");
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(cs.fullyInstrumentedClass);
						fos.close();
					}
				}
				cs.isOptimized = false;
				return cs.fullyInstrumentedClass;

			} catch (Throwable e) {
				System.err.println("Problem when processing " + classBeingRedefined);
				e.printStackTrace();
			}

			System.err.println("Returning null for some reason??");
			new Exception().printStackTrace();
			return null;
		}
		try {
//			if(DEBUG)
//						System.err.println("VMVMClassfiletransformer PLAIN " + className);
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
				throw new IllegalStateException(t);
			}

			ret = cw.toByteArray();
			if(DEBUG){
			ClassReader cr2 = new ClassReader(ret);
			cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			}
			if (cv.getReClinitMethod() != null) {
				//Also, generate a resetter for the interface
				String newName = cr.getClassName() + "$$VMVMRESETTER";
				//				Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});
				//				defineClassMethod.setAccessible(true);
				try {
					cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					cw.visit(cv.version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newName, null, "java/lang/Object", null);
					cw.visitSource(null, null);
					cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
					for (String s : cv.getOwnersOfStaticFields()) {
						cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z", null, true);
					}

					cv.getReClinitMethod().accept(cw);
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
//					System.out.println("Got resetter: " + newName);
					cl.addClass(newName.replace("/", "."), b);
					Class<?> c = cl.loadClass(newName.replace("/", "."));
					//					System.out.println(c);
					//					Class<?> c = (Class) defineClassMethod.invoke(loader, new Object[]{newName.replace("/", "."), b, 0, b.length});
					//					Method resolveMethod = ClassLoader.class.getDeclaredMethod("resolveClass", new Class[]{Class.class});
					//					resolveMethod.setAccessible(true);
					//					resolveMethod.invoke(loader, c);
					//					System.out.println(c);

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
