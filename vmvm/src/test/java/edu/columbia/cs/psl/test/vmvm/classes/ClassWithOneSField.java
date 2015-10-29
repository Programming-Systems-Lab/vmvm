package edu.columbia.cs.psl.test.vmvm.classes;

public class ClassWithOneSField implements IFace {
	public static int foo = 5;
	public int val = 5;

	static
	{
		System.out.println("Classwonesf init");
	}
	public ClassWithOneSField()
	{
		
	}
	public ClassWithOneSField(int baz)
	{
		
	}
	static int getFoo()
	{
		return foo;
	}
	public static ClassWithOneSField getInst()
	{
		return inst;
	}
}
