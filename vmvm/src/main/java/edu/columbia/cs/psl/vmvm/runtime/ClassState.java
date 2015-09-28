package edu.columbia.cs.psl.vmvm.runtime;

import java.util.LinkedList;

public final class ClassState {
	public final static String INTERNAL_NAME="edu/columbia/cs/psl/vmvm/runtime/ClassState";
	public static final String DESC = "Ledu/columbia/cs/psl/vmvm/runtime/ClassState;";
	public boolean needsReinit;
	public boolean isInterface;
	public boolean isOptimized;
	public boolean hasClassesToOptAway;
	public byte[] originalClass;
	public byte[] fullyInstrumentedClass;
	public String name;
	public void addClassToOptAway(String s)
	{
//		System.out.println("Can opt away in " + name + " - " + s);
		hasClassesToOptAway = true;
	}
}
