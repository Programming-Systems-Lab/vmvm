package edu.columbia.cs.psl.vmvm.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCapabilityCV;

public class VMVMClassFileTransformer implements ClassFileTransformer {
	public static boolean isIgnoredClass(String internalName) {
		return internalName.startsWith("java") || internalName.startsWith("sun") || internalName.startsWith("com/sun") || internalName.startsWith("edu/columbia/cs/psl/vmvm/runtime");
	}
	public static AdditionalInterfaceClassloader cl = new AdditionalInterfaceClassloader();



	private static final boolean DEBUG = false;
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (isIgnoredClass(className))
			return null;
		try {
			ClassReader cr = new ClassReader(classfileBuffer);

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ReinitCapabilityCV cv = new ReinitCapabilityCV(cw);
			cr.accept(cv, 0);
			byte [] ret =cw.toByteArray();
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
				fos.write(cw.toByteArray());
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
