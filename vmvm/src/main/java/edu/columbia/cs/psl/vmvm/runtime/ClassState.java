package edu.columbia.cs.psl.vmvm.runtime;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;

public final class ClassState {
	public final static String INTERNAL_NAME = "edu/columbia/cs/psl/vmvm/runtime/ClassState";
	public static final String DESC = "Ledu/columbia/cs/psl/vmvm/runtime/ClassState;";
	public boolean needsReinit;
	public boolean isInterface;
	public boolean isOptimized;
	public boolean hasClassesToOptAway;
	public byte[] originalClass;
	public byte[] fullyInstrumentedClass;
	public Class<?> clazz;
	public ClassState parent;
	public ClassState[] interfaces;
	public boolean fullyPopulated;
	public String superName;
	public String[] interfacesStr;
	public Field[] fields;

	/**
	 * The name of the class, in java-binary version (aka "a.b.c")
	 */
	public String name;

	public ClassState(Class<?> clazz, String[] interfaces, String superName) {
		this.clazz = clazz;
		this.name = clazz.getName();
		this.interfacesStr = interfaces;
		this.interfaces = new ClassState[interfaces.length];
		this.superName = superName;
	}

	public void addClassToOptAway() {
		hasClassesToOptAway = true;
	}

	public void populate(HashMap<String, WeakReference<ClassState>> initializedClasses) {
		boolean gotAll = true;
		if (!VMVMClassFileTransformer.isIgnoredClass(superName.replace(".", "/"))) {
			if (parent == null && initializedClasses.containsKey(superName)) {
				parent = initializedClasses.get(superName).get();
			}
			gotAll &= (parent != null);
		}
		for (int i = 0; i < interfacesStr.length; i++) {
			if (!VMVMClassFileTransformer.isIgnoredClass(interfacesStr[i].replace(".", "/"))) {
				if (interfaces[i] == null && initializedClasses.containsKey(interfacesStr[i])) {
					interfaces[i] = initializedClasses.get(interfacesStr[i]).get();
				}
				gotAll &= (interfaces[i] != null);
			}
		}
		if (this.fields == null)
			this.fields = clazz.getDeclaredFields();
		fullyPopulated = gotAll;
	}
}
