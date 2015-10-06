package edu.columbia.cs.psl.vmvm.runtime;


public final class ClassState {
	public final static String INTERNAL_NAME="edu/columbia/cs/psl/vmvm/runtime/ClassState";
	public static final String DESC = "Ledu/columbia/cs/psl/vmvm/runtime/ClassState;";
	public boolean needsReinit;
	public boolean isInterface;
	public boolean isOptimized;
	public boolean hasClassesToOptAway;
	public byte[] originalClass;
	public byte[] fullyInstrumentedClass;
	public Class<?> clazz;
	public String name;
	public ClassState(Class<?> clazz)
	{
		this.clazz = clazz;
		this.name = clazz.getName();
	}
	public void addClassToOptAway()
	{
		hasClassesToOptAway = true;
	}
}
