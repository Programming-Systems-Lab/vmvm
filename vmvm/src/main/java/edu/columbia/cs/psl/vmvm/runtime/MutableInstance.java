package edu.columbia.cs.psl.vmvm.runtime;

public class MutableInstance {
	public static final String INTERNAL_NAME="edu/columbia/cs/psl/vmvm/runtime/MutableInstance";
	public static final String DESC = "Ledu/columbia/cs/psl/vmvm/runtime/MutableInstance;";
	private Object inst;
	public MutableInstance(Object o)
	{
		this.inst = o;
	}
	public MutableInstance()
	{
		
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
