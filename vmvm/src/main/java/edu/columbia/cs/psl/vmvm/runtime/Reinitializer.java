package edu.columbia.cs.psl.vmvm.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import edu.columbia.cs.psl.vmvm.runtime.inst.ReinitCheckForceCV;

public final class Reinitializer {
	public static final String INTERNAL_NAME = "edu/columbia/cs/psl/vmvm/runtime/Reinitializer";

	public static Instrumentation inst;

	static LinkedList<WeakReference<Class>> classesToReinit = new LinkedList<WeakReference<Class>>();
	static WeakHashMap<Class, byte[]> classesToInstrumentedDef = new WeakHashMap<Class, byte[]>();
	static WeakHashMap<Class, byte[]> classesToOptimizedDef = new WeakHashMap<Class, byte[]>();

	public static final void callReinitOnInterface(String c) {
		try {
			Class cl = lookupInterfaceClass(c);
			cl.getDeclaredMethod("__vmvmReClinit", null).invoke(null);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}

	public static final void markAllClassesForReinit() {
		long start = System.currentTimeMillis();
		System.err.println("Start MCR ");
//		ClassDefinition[] toReinit = new ClassDefinition[classesToReinit.size()];
		int i = 0;
		LinkedList<WeakReference<Class>> toReinit = classesToReinit;
		classesToReinit = new LinkedList<WeakReference<Class>>();
		for (WeakReference<Class> w : toReinit) {
			if (w.get() != null) {
				Class c = w.get();

				try {
					Field f = c.getField(Constants.VMVM_NEEDS_RESET);
					if (!f.isAccessible())
						f.setAccessible(true);
					ClassState cs = (ClassState) f.get(null);
					cs.needsReinit = true;
					inst.redefineClasses(new ClassDefinition(c, classesToInstrumentedDef.get(c)));
					i++;
//					System.err.println("Adding checks in " + c);
				} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | NoClassDefFoundError | ClassNotFoundException | UnmodifiableClassException ex ) {
					ex.printStackTrace();
				}
				
			}
		}
//		try {
//			inst.redefineClasses(toReinit);
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		} catch (UnmodifiableClassException e) {
//			e.printStackTrace();
//		}
		System.err.println("End MCR " + (System.currentTimeMillis() - start));
	}

	public static final void markAsDone(Class<?> c) {
		try {
			System.err.println("Restoring optimized " + c);
			inst.redefineClasses(new ClassDefinition(c, classesToOptimizedDef.get(c)));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (UnmodifiableClassException e) {
			e.printStackTrace();
		}
	}

	public static final void reinitCalled(Class<?> c) {
//				System.err.println("REinit called " + c);
		classesToReinit.add(new WeakReference<Class>(c));
		//		inst.redefineClasses(definitions);
	}

	public static final void clinitCalled(Class<?> c) {
//				System.err.println("Clinit called on " + c.getName());
		classesToReinit.add(new WeakReference<Class>(c));
		try {
			byte[] uninst = VMVMClassFileTransformer.instrumentedClasses.remove(c.getName().replace(".", "/"));
			classesToOptimizedDef.put(c, uninst);
			ClassReader cr = new ClassReader(uninst);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ReinitCheckForceCV cv = new ReinitCheckForceCV(cw);
			try{
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
			}
			catch(Throwable t)
			{
				throw new IllegalStateException(t);
			}
			if (VMVMClassFileTransformer.DEBUG) {
				File debugDir = new File("debug-instheavy");
				if (!debugDir.exists())
					debugDir.mkdir();
				File f = new File("debug-instheavy/" + c.getName().replace("/", ".") + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(cw.toByteArray());
				fos.close();

			}
			classesToInstrumentedDef.put(c, cw.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Class<?> lookupInterfaceClass(String name) throws ClassNotFoundException {
		return VMVMClassFileTransformer.cl.loadClass(name.replace("/", "."));
	}

}
