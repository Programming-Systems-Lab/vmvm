package edu.columbia.cs.psl.vmvm.runtime;

public class Reinitializer {
	public static final String INTERNAL_NAME = "edu/columbia/cs/psl/vmvm/runtime/Reinitializer";
	
	private static int engaged = 0;
	private static native void _markAllClassesForReinit();
	private static native void _reinitCalled(Class c);
	public static void markAllClassesForReinit()
	{
		long start = System.currentTimeMillis();
		System.err.println("Start MCR ");
		if(engaged == 0)
			throw new IllegalArgumentException("JVMTI Agent not loaded");
		_markAllClassesForReinit();
		System.err.println("End MCR " + (System.currentTimeMillis() - start));
	}
	public static void reinitCalled(Class<?> c)
	{
		if(engaged == 0)
			throw new IllegalArgumentException("JVMTI Agent not loaded");
		_reinitCalled(c);
	}
	public static Class<?> lookupInterfaceClass(String name) throws ClassNotFoundException
	{
		return VMVMClassFileTransformer.cl.loadClass(name.replace("/", "."));
	}

}
