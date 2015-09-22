package edu.columbia.cs.psl.vmvm.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCapabilityCV;

public class VMVMClassFileTransformer implements ClassFileTransformer {
	public static boolean isIgnoredClass(String internalName) {
		return internalName.startsWith("java") || internalName.startsWith("sun") || internalName.startsWith("com/sun") || internalName.startsWith("edu/columbia/cs/psl/vmvm/runtime")
				|| internalName.startsWith("org/junit") || internalName.startsWith("junit/") || internalName.startsWith("edu/columbia/cs/psl/vmvm/") || internalName.startsWith("org/apache/maven/surefire")
				|| internalName.startsWith("org/apache/tools/");
	}
	public static AdditionalInterfaceClassloader cl = new AdditionalInterfaceClassloader();


	public static HashMap<String, byte[]> instrumentedClasses = new HashMap<String, byte[]>();
	public static final boolean DEBUG = false;
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (isIgnoredClass(className))
			return null;
		if(classBeingRedefined != null)
			return classfileBuffer;
		try {
//			System.err.println("VMVMClassfiletransformer " + className);
			ClassReader cr = new ClassReader(classfileBuffer);

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ReinitCapabilityCV cv = new ReinitCapabilityCV(new CheckClassAdapter(cw,false));
			//new CheckClassAdapter(cw));
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
			byte [] ret =cw.toByteArray();
			instrumentedClasses.put(className, ret);

			ClassReader cr2 = new ClassReader(ret);
			cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			if(cv.getReClinitMethod() != null)
			{
				//Also, generate a resetter for the interface
				String newName = cr.getClassName()+"$$VMVMRESETTER";
//				Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});
//				defineClassMethod.setAccessible(true);
				try
				{
					cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					cw.visit(cv.version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newName, null, "java/lang/Object", null);
					cw.visitSource(null, null);
					cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
					cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.TOTAL_STATIC_CLASSES_CHECKED, "I", null, 0);
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
					cl.addClass(newName.replace("/", "."),b);
					Class<?> c = cl.loadClass(newName.replace("/", "."));
//					System.out.println(c);
//					Class<?> c = (Class) defineClassMethod.invoke(loader, new Object[]{newName.replace("/", "."), b, 0, b.length});
//					Method resolveMethod = ClassLoader.class.getDeclaredMethod("resolveClass", new Class[]{Class.class});
//					resolveMethod.setAccessible(true);
//					resolveMethod.invoke(loader, c);
//					System.out.println(c);
					
				}
				catch(Exception ex)
				{
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
