package edu.columbia.cs.psl.vmvm.runtime;

public class MutableInstance {
	public static final String INTERNAL_NAME="edu/columbia/cs/psl/vmvm/runtime/MutableInstance";
	public static final String DESC = "Ledu/columbia/cs/psl/vmvm/runtime/MutableInstance;";
	private Object inst;
	private Class type;
	public MutableInstance(Class type,Object o)
	{
		this.inst = o;
		this.type = type;
	}
	public MutableInstance(Class type)
	{
		this.type = type;
	}
	public Class getType() {
		return type;
	}
	public Object get()
	{
		return inst;
	}
	public void put(Object o)
	{
		inst = o;
	}
}
