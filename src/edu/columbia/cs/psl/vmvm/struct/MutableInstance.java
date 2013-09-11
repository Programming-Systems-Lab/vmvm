package edu.columbia.cs.psl.vmvm.struct;

public class MutableInstance {
	private Object inst;
	public Object get()
	{
		return inst;
	}
	public void put(Object o)
	{
		inst = o;
	}
}
