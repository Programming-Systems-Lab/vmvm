package edu.columbia.cs.psl.vmvm.runtime;

import java.util.HashMap;

public class AdditionalInterfaceClassloader extends ClassLoader {
	HashMap<String, byte[]> definedClasses = new HashMap<String, byte[]>();

	public void addClass(String name, byte[] b) {
		definedClasses.put(name, b);
	}

	protected java.lang.Class<?> findClass(String name) throws ClassNotFoundException {
		if (definedClasses.containsKey(name)) {
			byte[] b = definedClasses.remove(name);
			Class<?> c = defineClass(name, b, 0, b.length);
			resolveClass(c);
			return c;
		}
		return null;
	}
}
